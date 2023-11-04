/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSafeWalkFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSlowFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldZitterFeature
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.targetFinding.AimMode
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetFinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetFinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.FaceTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.NearestRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.PositionFactoryConfiguration
import net.ccbluex.liquidbounce.utils.block.targetFinding.RandomTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.StabilizedRotationTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetFinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.client.enforced
import net.ccbluex.liquidbounce.utils.client.moveKeys
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.item.DISALLOWED_BLOCKS_TO_PLACE
import net.ccbluex.liquidbounce.utils.item.PreferAverageHardBlocks
import net.ccbluex.liquidbounce.utils.item.PreferFullCubeBlocks
import net.ccbluex.liquidbounce.utils.item.PreferLessSlipperyBlocks
import net.ccbluex.liquidbounce.utils.item.PreferSolidBlocks
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.SideShapeType
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemPlacementContext
import net.minecraft.item.ItemStack
import net.minecraft.item.ItemUsageContext
import net.minecraft.item.Items
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.abs
import kotlin.random.Random

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
object ModuleScaffold : Module("Scaffold", Category.WORLD) {
    object SimulatePlacementAttempts : ToggleableConfigurable(this, "SimulatePlacementAttempts", false) {
        val cps by intRange("CPS", 5..8, 0..50)

        val failedAttemptsOnly by boolean("FailedAttemptsOnly", true)
    }

    private val cpsScheduler = tree(CpsScheduler())

    private val silent by boolean("Silent", true)
    private val slotResetDelay by int("SlotResetDelay", 5, 0..40)
    private var delay by intRange("Delay", 3..5, 0..40)

    private val swing by boolean("Swing", true)

    // Rotation
    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val aimMode = enumChoice("RotationMode", AimMode.STABILIZED, AimMode.values())

    object AdvancedRotation : ToggleableConfigurable(this, "AdvancedRotation", false) {
        val DEFAULT_XZ_RANGE = 0.1f..0.9f
        val DEFAULT_Y_RANGE = 0.33f..0.85f

        val xRange by floatRange("XRange", DEFAULT_XZ_RANGE, 0.0f..1.0f)
        val yRange by floatRange("YRange", DEFAULT_Y_RANGE, 0.0f..1.0f)
        val zRange by floatRange("ZRange", DEFAULT_XZ_RANGE, 0.0f..1.0f)
        val step by float("Step", 0.1f, 0f..1f)
    }

    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    val zitterModes =
        choices(
            "ZitterMode",
            ScaffoldZitterFeature.Off,
            arrayOf(
                ScaffoldZitterFeature.Off,
                ScaffoldZitterFeature.Teleport,
                ScaffoldZitterFeature.Smooth,
            ),
        )
    private val timer by float("Timer", 1f, 0.01f..10f)
    private val speedModifier by float("SpeedModifier", 1f, 0f..3f)

    private val sameY by boolean("SameY", false)
    private var currentTarget: BlockPlacementTarget? = null

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    private val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    init {
        tree(SimulatePlacementAttempts)
        tree(ScaffoldEagleFeature)
        tree(ScaffoldDownFeature)
        tree(ScaffoldSlowFeature)
        tree(ScaffoldSafeWalkFeature)
        tree(AdvancedRotation)
        tree(ScaffoldStabilizeMovementFeature)
    }

    var randomization = Random.nextDouble(-0.02, 0.02)
    private var startY = 0

    /**
     * This comparator will estimate the value of a block. If this comparator says that Block A > Block B, Scaffold will
     * prefer Block A over Block B.
     * The chain will prefer the block that is solid. If both are solid, it goes to the next criteria
     * (in this case full cube) and so on
     */
    private val BLOCK_COMPARATOR_FOR_HOTBAR =
        ComparatorChain(
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferLessSlipperyBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = false),
        )
    val BLOCK_COMPARATOR_FOR_INVENTORY =
        ComparatorChain(
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferLessSlipperyBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = true),
        )

    override fun enable() {
        // Chooses a new randomization value
        randomization = Random.nextDouble(-0.01, 0.01)
        startY = player.blockPos.y

        ScaffoldMovementPlanner.reset()

        super.enable()
    }

    override fun disable() {
        moveKeys.forEach {
            it.enforced = null
        }

        ScaffoldMovementPlanner.reset()
        SilentHotbar.resetSlot(this)
    }

    private val rotationUpdateHandler =
        handler<PlayerNetworkMovementTickEvent> {
            if (it.state != EventState.PRE) {
                return@handler
            }

            val blockInHotbar = findBestValidHotbarSlotForTarget()

            val bestStick =
                if (blockInHotbar == null) {
                    ItemStack(Items.SANDSTONE, 64)
                } else {
                    player.inventory.getStack(blockInHotbar)
                }

            val optimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(DirectionalInput(player.input))

            val priorityGetter: (Vec3i) -> Double =
                if (optimalLine != null) {
                    { vec ->
                        -optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5))
                    }
                } else {
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
                }

            val searchOptions =
                BlockPlacementTargetFindingOptions(
                    if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
                    bestStick,
                    getFacePositionFactoryForConfig(),
                    priorityGetter,
                )

            currentTarget = findBestBlockPlacementTarget(getTargetedPosition(), searchOptions)

            val target = currentTarget ?: return@handler

            if (optimalLine != null) {
                val b = target.placedBlock.toVec3d().add(0.5, 1.0, 0.5)
                val a = optimalLine.getNearestPointTo(b)

                // Debug the line a-b
                ModuleDebug.debugGeometry(
                    ModuleScaffold,
                    "lineToBlock",
                    ModuleDebug.DebuggedLineSegment(
                        from = Vec3(a),
                        to = Vec3(b),
                        Color4b(255, 0, 0, 255),
                    ),
                )
            }

            RotationManager.aimAt(
                target.rotation,
                openInventory = ignoreOpenInventory,
                configurable = rotationsConfigurable,
            )
        }

    fun getFacePositionFactoryForConfig(): FaceTargetPositionFactory {
        val config =
            PositionFactoryConfiguration(
                mc.player!!.eyePos,
                if (AdvancedRotation.enabled) AdvancedRotation.xRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
                if (AdvancedRotation.enabled) AdvancedRotation.yRange.toDouble() else AdvancedRotation.DEFAULT_Y_RANGE.toDouble(),
                if (AdvancedRotation.enabled) AdvancedRotation.zRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
                AdvancedRotation.step.toDouble(),
                randomization,
            )

        return when (aimMode.get()) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED ->
                StabilizedRotationTargetPositionFactory(
                    config,
                    ScaffoldMovementPlanner.getOptimalMovementLine(DirectionalInput(player.input)),
                )

            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
        }
    }

    val moveHandler =
        repeatable {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE)
        }

    val networkTickHandler =
        repeatable {
            val target = currentTarget
            val currentRotation = RotationManager.currentRotation ?: player.rotation
            val currentCrosshairTarget = raycast(4.5, currentRotation)

            // Prioritize by all means the main hand if it has a block
            val suitableHand =
                arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull { isValidBlock(player.getStackInHand(it)) }

            repeat(
                cpsScheduler.clicks(
                    { simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving },
                    SimulatePlacementAttempts.cps,
                ),
            ) {
                // By the time this reaches here the variables are already non-null
                ModuleNoFall.MLG.doPlacement(
                    currentCrosshairTarget!!,
                    suitableHand!!,
                    ModuleScaffold::swing,
                    ModuleScaffold::swing,
                )
            }

            if (target == null || currentCrosshairTarget == null) {
                return@repeatable
            }

            // Is the target the crosshair points too well-adjusted to our target?
            if (!target.doesCrosshairTargetFullfitRequirements(currentCrosshairTarget) ||
                !isValidCrosshairTarget(
                    currentCrosshairTarget,
                )
            ) {
                return@repeatable
            }

            var hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot))
            val hasBlockInOffHand = isValidBlock(player.offHandStack)

            // Handle silent block selection
            if (silent && !hasBlockInMainHand && !hasBlockInOffHand) {
                val bestMainHandSlot = findBestValidHotbarSlotForTarget()

                if (bestMainHandSlot != null) {
                    SilentHotbar.selectSlotSilently(this, bestMainHandSlot, slotResetDelay)

                    hasBlockInMainHand = true
                } else {
                    SilentHotbar.resetSlot(this)
                }
            } else {
                SilentHotbar.resetSlot(this)
            }

            if (!hasBlockInMainHand && !hasBlockInOffHand) {
                return@repeatable
            }

            // no need for additional checks
            val handToInteractWith = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
            val result =
                interaction.interactBlock(
                    player,
                    handToInteractWith,
                    currentCrosshairTarget,
                )

            if (!result.isAccepted) {
                return@repeatable
            }

            ScaffoldMovementPlanner.trackPlacedBlock(target)
            ScaffoldEagleFeature.onBlockPlacement()

            if (player.isOnGround) {
                player.velocity.x *= speedModifier
                player.velocity.z *= speedModifier
            }
            if (result.shouldSwingHand() && swing) {
                player.swingHand(handToInteractWith)
            }

            currentTarget = null

            delay.random().let {
                if (it > 0) {
                    wait(it)
                }
            }
        }

    private fun findBestValidHotbarSlotForTarget(): Int? {
        val bestSlot =
            (0..8)
                .filter { isValidBlock(player.inventory.getStack(it)) }
                .mapNotNull {
                    val stack = player.inventory.getStack(it)

                    if (stack.item is BlockItem) {
                        Pair(it, stack)
                    } else {
                        null
                    }
                }
                .maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.second, o2.second) }?.first

        return bestSlot
    }

    private fun isValidCrosshairTarget(rayTraceResult: BlockHitResult): Boolean {
        val eyesPos = player.eyes
        val hitVec = rayTraceResult.pos

        val diffX = hitVec.x - eyesPos.x
        val diffZ = hitVec.z - eyesPos.z

        val side = rayTraceResult.side

        // Apply minDist
        if (side != Direction.UP && side != Direction.DOWN) {
            val diff = if (side == Direction.NORTH || side == Direction.SOUTH) diffZ else diffX

            if (abs(diff) < minDist) {
                return false
            }
        }

        return true
    }

    private fun isValidBlock(stack: ItemStack?): Boolean {
        if (stack == null) return false

        val item = stack.item

        if (item !is BlockItem) {
            return false
        }

        val block = item.block

        if (!block.defaultState.isSideSolid(world, BlockPos.ORIGIN, Direction.UP, SideShapeType.CENTER)) {
            return false
        }

        return !DISALLOWED_BLOCKS_TO_PLACE.contains(block)
    }

    private fun getTargetedPosition(): BlockPos {
        if (ScaffoldDownFeature.shouldGoDown) {
            return player.blockPos.add(0, -2, 0)
        }

        return if (sameY) {
            BlockPos(player.blockPos.x, startY - 1, player.blockPos.z)
        } else {
            player.blockPos.add(0, -1, 0)
        }
    }

    private fun commonOffsetToInvestigate(xzOffsets: List<Int>): List<Vec3i> {
        return xzOffsets.flatMap { x ->
            xzOffsets.flatMap { z ->
                (0 downTo -1).flatMap { y ->
                    listOf(Vec3i(x, y, z))
                }
            }
        }
    }

    private fun simulatePlacementAttempts(
        hitResult: BlockHitResult?,
        suitableHand: Hand?,
    ): Boolean {
        val stack =
            if (suitableHand == Hand.MAIN_HAND) {
                player.inventory.getStack(SilentHotbar.serversideSlot)
            } else {
                player.offHandStack
            }

        val option = SimulatePlacementAttempts

        if (hitResult == null || suitableHand == null || !option.enabled) {
            return false
        }

        if (hitResult.type != HitResult.Type.BLOCK) {
            return false
        }

        val context = ItemUsageContext(player, suitableHand, hitResult)

        val canPlaceOnFace = (stack.item as BlockItem).getPlacementState(ItemPlacementContext(context)) != null

        return when {
            SimulatePlacementAttempts.failedAttemptsOnly -> {
                !canPlaceOnFace
            }

            sameY -> {
                context.blockPos.y == startY - 1 && (hitResult.side != Direction.UP || !canPlaceOnFace)
            }

            else -> {
                val isTargetUnderPlayer = context.blockPos.y <= player.blockY - 1
                val isTowering =
                    context.blockPos.y == player.blockY - 1 &&
                            canPlaceOnFace &&
                            context.side == Direction.UP

                isTargetUnderPlayer && !isTowering
            }
        }
    }
}
