@file:Suppress("TooManyFunctions","NestedBlockDepth")

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raytraceBlock
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityType
import net.minecraft.entity.TntEntity
import net.minecraft.entity.mob.CreeperEntity
import net.minecraft.item.BlockItem
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import net.minecraft.util.shape.VoxelShapes
import kotlin.math.abs

/**
 * Automatically builds a protective wall to shield against nearby explosives (TNT or Creepers).
 * Configurable detection range, wall dimensions, block selection, and building behavior.
 */
object ModuleAntiExplosion : ClientModule("AntiExplosion", Category.WORLD) {

    private val detectionRange by float("DetectionRange", 10.0f, 1.0f..20.0f, "blocks")
    private val buildTriggerRange by float("BuildTriggerRange", 3.0f, 1.0f..10.0f, "blocks")

    private val wallHeight by int("WallHeight", 2, 1..5, "blocks")
    private val wallWidth by int("WallWidth", 2, 1..5, "blocks")
    private val minBlockCount by int("MinBlockCount", 1, 0..8, "blocks")

    private val immediateSlotSwitch by boolean("ImmediateSlotSwitch", true)
    private val notDuringCombat by boolean("NotDuringCombat", true)
    private val preferHarderBlocks by boolean("PreferHarderBlocks", true)

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var isBuildingWall = false
    private var placedAnyBlock = false
    private var wallPositions = mutableListOf<BlockPos>()
    private var currentWallIndex = 0
    private var lastExplosivePos: Vec3d? = null
    private var lastWallCenter: BlockPos? = null
    private var lastExplosiveId: Int? = null
    private val knownExplosiveIds = mutableSetOf<Int>()
    private val cooldownTimer = Chronometer()

    /**
     * Rotation update handler to manage aiming at block placement positions.
     */
    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        if (!isBuildingWall || !canProceedWithBuilding()) return@handler

        val player = mc.player ?: return@handler
        if (currentWallIndex >= wallPositions.size) {
            finishBuildingWall()
            return@handler
        }

        val targetPos = wallPositions[currentWallIndex]
        val blockSlot = findBlockSlot() ?: return@handler

        val placementTarget = findBestBlockPlacementTarget(
            targetPos,
            BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    listOf(Vec3i.ZERO),
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = blockSlot.itemStack,
                PlayerLocationOnPlacement(position = player.pos)
            )
        ) ?: return@handler

        RotationManager.setRotationTarget(
            rotationsConfigurable.toRotationTarget(placementTarget.rotation),
            priority = Priority.IMPORTANT_FOR_USAGE_3,
            provider = this@ModuleAntiExplosion
        )
    }

    /**
     * Tick handler to detect explosives and manage wall building.
     */
    @Suppress("unused")
    private val tickHandler = tickHandler {
        val player = mc.player ?: return@tickHandler
        val world = mc.world ?: return@tickHandler

        if (!canProceedWithBuilding()) return@tickHandler

        if (isBuildingWall) {
            continueBuildingWall(player)
            return@tickHandler
        }

        val nearbyExplosive = findNearbyExplosive(buildTriggerRange) ?: run {
            if (lastExplosiveId != null && findNearbyExplosive(detectionRange) == null) {
                stopBuildingWall()
            }
            return@tickHandler
        }

        lastWallCenter?.let { center ->
            if (player.pos.squaredDistanceTo(Vec3d.ofCenter(center)) > buildTriggerRange * buildTriggerRange) {
                stopBuildingWall()
                findNearbyExplosive(buildTriggerRange)?.let { explosive ->
                    if (canBuildWall(player, explosive.pos)) {
                        startNewWall(player, explosive)
                    }
                }
            }
        }
        if (!knownExplosiveIds.contains(nearbyExplosive.id) || needsRebuildForExplosive(nearbyExplosive, player)) {
            startNewWall(player, nearbyExplosive)
        }
    }

    /**
     * Checks if building can proceed based on conditions like combat, sneaking, and cooldown.
     */
    private fun canProceedWithBuilding(): Boolean {
        val player = mc.player ?: return false
        return !player.isUsingItem && (!notDuringCombat || !CombatManager.isInCombat)
    }

    /**
     * Starts building a new protective wall.
     */
    private fun startNewWall(player: ClientPlayerEntity, nearbyExplosive: Entity) {
        if (!canBuildWall(player, nearbyExplosive.pos) || findBlockSlot() == null) {
            return
        }

        if (immediateSlotSwitch) {
            switchToBlockSlot(player)
        }

        startBuildingWall(player, nearbyExplosive.pos, nearbyExplosive.id)
        notification(
            "AntiExplosion",
            "Building protective wall against ${getExplosiveName(nearbyExplosive)}.",
            NotificationEvent.Severity.INFO
        )
    }

    /**
     * Determines if a new wall is needed for the explosive.
     */
    private fun needsRebuildForExplosive(explosive: Entity, player: ClientPlayerEntity): Boolean {
        if (!knownExplosiveIds.contains(explosive.id)) return true
        val center = lastWallCenter ?: return true
        return player.blockPos != center
    }

    /**
     * Finds the closest explosive entity within the specified range.
     */
    private fun findNearbyExplosive(range: Float): Entity? {
        val player = mc.player ?: return null
        val world = mc.world ?: return null
        val searchBox = Box(
            player.pos.x - range, player.pos.y - range, player.pos.z - range,
            player.pos.x + range, player.pos.y + range, player.pos.z + range
        )
        return world.getEntitiesByType(EntityType.TNT, searchBox) { e -> e is TntEntity && e.fuse > 0 }
            .plus(
                world.getEntitiesByType(EntityType.CREEPER, searchBox) { e ->
                    e is CreeperEntity && e.isIgnited && e.fuseSpeed > 0
                }
            )
            .filter { entity -> player.pos.squaredDistanceTo(entity.pos) <= range * range }
            .minByOrNull { it.pos.squaredDistanceTo(player.pos) }
    }

    /**
     * Switches to the best block slot in the hotbar.
     */
    private fun switchToBlockSlot(player: ClientPlayerEntity) {
        val slot = findBlockSlot()
        if (slot is HotbarItemSlot && slot.useHand == Hand.MAIN_HAND) {
            val hotbar = slot.hotbarSlot
            if (hotbar in 0..8) player.inventory.selectedSlot = hotbar
        }
    }
    /**
     * Finds the best block slot based on hardness and count.
     */
    private fun findBlockSlot(): ItemSlot? {
        return Slots.OffhandWithHotbar
            .filter { slot ->
                val stack = slot.itemStack
                stack.count >= minBlockCount && stack.item != Items.TNT && isValidBlock(stack)
            }
            .maxByOrNull { slot ->
                val stack = slot.itemStack
                val block = (stack.item as? BlockItem)?.block
                val hardness = block?.hardness ?: 0f
                if (preferHarderBlocks) hardness else stack.count.toFloat()
            }
    }

    /**
     * Generates the positions for the protective wall.
     */
    private fun getWallPositions(player: ClientPlayerEntity, explosivePos: Vec3d): List<BlockPos> {
        val playerPos = player.blockPos
        val direction = explosivePos.subtract(player.pos)
        val length = direction.length()
        val normalizedDirection = if (length > 0) direction.multiply(1.0 / length) else Vec3d.ZERO
        val wallDirection = calculateWallDirection(normalizedDirection)
        val wallCenter = playerPos.add(wallDirection.x.toInt(), 0, wallDirection.z.toInt())

        val wallPositions = mutableListOf<BlockPos>()
        for (y in 0 until wallHeight) {
            for (x in 0 until wallWidth) {
                for (z in 0 until wallWidth) {
                    val pos = wallCenter.add(x, y, z)
                    if (x == 0 && z == 0) {
                        wallPositions.add(pos)
                    } else if (wallWidth > 1) {
                        wallPositions.add(pos)
                    }
                }
            }
        }
        return wallPositions
    }

    /**
     * Checks if the wall can be built at the specified positions.
     */
    private fun canBuildWall(player: ClientPlayerEntity, explosivePos: Vec3d): Boolean {
        val explosiveBlockPos = BlockPos.ofFloored(explosivePos)
        val wallPositionsToCheck = getWallPositions(player, explosivePos)

        if (wallPositionsToCheck.contains(explosiveBlockPos)) {
            return false
        }

        return wallPositionsToCheck.all { pos ->
            val state = player.world.getBlockState(pos)
            state != null && state.isAir
        }
    }

    /**
     * Initiates the wall-building process.
     */
    private fun startBuildingWall(player: ClientPlayerEntity, explosivePos: Vec3d, explosiveId: Int) {
        isBuildingWall = true
        placedAnyBlock = false
        currentWallIndex = 0
        lastExplosivePos = explosivePos
        lastExplosiveId = explosiveId
        knownExplosiveIds.add(explosiveId)

        val wallPositionsToCheck = getWallPositions(player, explosivePos)
        lastWallCenter = wallPositionsToCheck.firstOrNull() ?: return

        wallPositions.clear()
        wallPositions.addAll(wallPositionsToCheck)

        wallPositions.removeAll { pos ->
            pos == BlockPos.ofFloored(explosivePos) || player.world.getBlockState(pos).let { state ->
                state != null && !state.isAir
            }
        }
    }

    /**
     * Calculates the direction for the wall based on the explosive's position.
     */
    private fun calculateWallDirection(normalizedDirection: Vec3d): Vec3d {
        return when {
            abs(normalizedDirection.x) > abs(normalizedDirection.z) -> {
                if (normalizedDirection.x > 0) Vec3d(1.0, 0.0, 0.0) else Vec3d(-1.0, 0.0, 0.0)
            }
            else -> {
                if (normalizedDirection.z > 0) Vec3d(0.0, 0.0, 1.0) else Vec3d(0.0, 0.0, -1.0)
            }
        }
    }

    /**
     * Continues the wall-building process by placing blocks.
     */
    private fun continueBuildingWall(player: ClientPlayerEntity) {
        if (currentWallIndex >= wallPositions.size) {
            finishBuildingWall()
            return
        }

        val blockSlot = findBlockSlot() ?: run {
            stopBuildingWall()
            return
        }

        if (blockSlot is HotbarItemSlot && blockSlot.useHand == Hand.MAIN_HAND) {
            val hotbar = blockSlot.hotbarSlot
            if (hotbar in 0..8 && player.inventory.selectedSlot != hotbar) {
                player.inventory.selectedSlot = hotbar
            }
        }

        placeBlockAtPosition(player, wallPositions[currentWallIndex], blockSlot)
        placedAnyBlock = true
        currentWallIndex++
    }

    /**
     * Stops the wall-building process and resets state.
     */
    private fun stopBuildingWall() {
        isBuildingWall = false
        wallPositions.clear()
        currentWallIndex = 0
        lastExplosivePos = null
        lastExplosiveId?.let { knownExplosiveIds.remove(it) }
        lastExplosiveId = null
        lastWallCenter = null
        if (placedAnyBlock) {
            notification(
                "AntiExplosion",
                "Protective wall completed.",
                NotificationEvent.Severity.SUCCESS
            )
        }
        cooldownTimer.reset()
    }

    /**
     * Finishes the wall-building process.
     */
    private fun finishBuildingWall() {
        isBuildingWall = false
        wallPositions.clear()
        currentWallIndex = 0
        cooldownTimer.reset()
    }

    /**
     * Places a block at the specified position.
     */
    private fun placeBlockAtPosition(player: ClientPlayerEntity, targetPos: BlockPos, blockSlot: ItemSlot) {
        val itemStack = blockSlot.itemStack
        val searchOptions = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(listOf(Vec3i.ZERO), BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = itemStack,
            PlayerLocationOnPlacement(position = player.pos)
        )

        val placementTarget = findBestBlockPlacementTarget(targetPos, searchOptions) ?: return
        val rotation = placementTarget.rotation

        val worldState = placementTarget.interactedBlockPos.getState() ?: return
        val rayTraceResult = raytraceBlock(
            4.5,
            rotation,
            placementTarget.interactedBlockPos,
            worldState
        ) ?: return

        val hand = if (blockSlot is HotbarItemSlot) blockSlot.useHand else Hand.MAIN_HAND

        doPlacement(rayTraceResult, hand = hand)
    }

    /**
     * Gets the name of the explosive entity for notifications.
     */
    private fun getExplosiveName(entity: Entity): String {
        return when (entity) {
            is TntEntity -> "TNT"
            is CreeperEntity -> if (entity.isCharged) "Charged Creeper" else "Creeper"
            else -> "Explosive"
        }
    }

    override fun onDisabled() {
        stopBuildingWall()
    }
}
