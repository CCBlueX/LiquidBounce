/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.*
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldEagleTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTellyTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetFinding.*
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.combat.ClickScheduler
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.isCloseToEdge
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.movement.findEdgeCollision
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.FallingBlock
import net.minecraft.block.SideShapeType
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket.Full
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
@Suppress("TooManyFunctions")
object ModuleScaffold : Module("Scaffold", Category.WORLD) {

    object SimulatePlacementAttempts : ToggleableConfigurable(this, "SimulatePlacementAttempts", false) {
        internal val clickScheduler = tree(ClickScheduler(ModuleScaffold, false, maxCps = 100))
        val failedAttemptsOnly by boolean("FailedAttemptsOnly", true)
    }

    private var delay by intRange("Delay", 3..5, 0..40, "ticks")
    object Swing : ToggleableConfigurable(this, "Swing", true) {
        val silentSwing by boolean("Silent", false);
    }

    // Silent block selection
    private val autoBlock by boolean("AutoBlock", true)
    private val alwaysHoldBlock by boolean("AlwaysHoldBlock", false)
    private val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")

    // Rotation
    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val aimMode by enumChoice("RotationMode", AimMode.STABILIZED)
    private val aimTimingMode by enumChoice("AimTiming", AimTimingMode.NORMAL)
    internal val technique = choices("Technique", ScaffoldNormalTechnique,
        arrayOf(ScaffoldNormalTechnique, ScaffoldEagleTechnique, ScaffoldTellyTechnique))

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

    // SafeWalk feature - uses the SafeWalk module as a base
    @Suppress("UnusedPrivateProperty")
    private val safeWalkMode = choices("SafeWalk", {
        it.choices[1] // Safe mode
    }) {
        arrayOf(NoneChoice(it), ModuleSafeWalk.Safe(it), ModuleSafeWalk.Simulate(it), ModuleSafeWalk.OnEdge(it))
    }

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

    private val sameY by boolean("SameY", false)
    private val jumpSlowdown by float("SlowdownOnJump", 0f, 0f..1f)

    private var currentTarget: BlockPlacementTarget? = null

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    private val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    init {
        tree(SimulatePlacementAttempts)
        tree(Swing)
        tree(ScaffoldSlowFeature)
        tree(ScaffoldSpeedLimiterFeature)
        tree(ScaffoldDownFeature)
        tree(ScaffoldAutoJumpFeature)
        tree(AdvancedRotation)
        tree(ScaffoldStabilizeMovementFeature)
        tree(ScaffoldBreezilyFeature)
    }

    @Suppress("UnusedPrivateProperty")
    val towerMode = choices("Tower", {
        it.choices[0] // None
    }) {
        arrayOf(NoneChoice(it), ScaffoldTowerMotion)
    }

    private var randomization = Random.nextDouble(-0.02, 0.02)
    private var placementY = 0

    /**
     * This comparator will estimate the value of a block. If this comparator says that Block A > Block B, Scaffold will
     * prefer Block A over Block B.
     * The chain will prefer the block that is solid. If both are solid, it goes to the next criteria
     * (in this case full cube) and so on
     */
    val BLOCK_COMPARATOR_FOR_HOTBAR =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = false),
        )
    val BLOCK_COMPARATOR_FOR_INVENTORY =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks,
            PreferStackSize(higher = true),
        )

    override fun enable() {
        // Chooses a new randomization value
        randomization = Random.nextDouble(-0.01, 0.01)

        // Placement Y is the Y coordinate of the block below the player
        placementY = player.blockPos.y - 1

        ScaffoldMovementPlanner.reset()

        super.enable()
    }

    override fun disable() {
        NoFallBlink.waitUntilGround = false
        ScaffoldMovementPlanner.reset()
        SilentHotbar.resetSlot(this)
    }

    private val afterJumpEvent = handler<PlayerAfterJumpEvent>(priority = EventPriorityConvention.SAFETY_FEATURE) {
        randomization = Random.nextDouble(-0.01, 0.01)
        placementY = player.blockPos.y - if (mc.options.jumpKey.isPressed) 0 else 1

        // Slow down the player when jumping
        if (jumpSlowdown != 0f) {
            val velocity = player.velocity

            player.setVelocity(
                velocity.x / (1 + jumpSlowdown),
                velocity.y,
                velocity.z / (1 + jumpSlowdown)
            )
        }
    }

    private val rotationUpdateHandler = handler<SimulatedTickEvent> {
        NoFallBlink.waitUntilGround = true

        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStack = if (blockInHotbar == null) {
            ItemStack(Items.SANDSTONE, 64)
        } else {
            player.inventory.getStack(blockInHotbar)
        }

        val optimalLine = this.currentOptimalLine

        val predictedPos = getPredictedPlacementPos() ?: player.pos

        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityGetter: (Vec3i) -> Double = if (optimalLine != null) {
            { vec -> -optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        val searchOptions =
            BlockPlacementTargetFindingOptions(
                if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
                bestStack,
                getFacePositionFactoryForConfig(),
                priorityGetter,
                predictedPos
            )

        currentTarget = findBestBlockPlacementTarget(getTargetedPosition(), searchOptions)

        val target = currentTarget

        // Debug stuff
        if (optimalLine != null && target != null) {
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

        val rotation = when (aimMode) {
            AimMode.GODBRIDGE -> ScaffoldGodBridgeFeature.optimizeRotation(target)
            AimMode.BREEZILY -> ScaffoldBreezilyFeature.optimizeRotation(target)
            else -> target?.rotation
        } ?: return@handler

        // Do not aim yet in SKIP mode, since we want to aim at the block only when we are about to place it
        if (aimTimingMode != AimTimingMode.ON_TICK) {
            RotationManager.aimAt(
                rotation,
                considerInventory = !ignoreOpenInventory,
                configurable = rotationsConfigurable,
                provider = this@ModuleScaffold,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
        }
    }

    /**
     * Calculates where the player will stand when he places the block. Useful for rotations
     *
     * @return the predicted pos or `null` if the prediction failed
     */
    private fun getPredictedPlacementPos(): Vec3d? {
        val optimalLine = this.currentOptimalLine ?: return null

        val optimalEdgeDist = 0.2

        // When we are close to the edge, we are able to place right now. Thus, we don't want to use a future position
        if (player.isCloseToEdge(DirectionalInput(player.input), distance = optimalEdgeDist))
            return null

        // TODO Check if the player is moving away from the line and implement another prediction method for that case

        val nearestPosToPlayer = optimalLine.getNearestPointTo(player.pos)

        val fromLine = nearestPosToPlayer + Vec3d(0.0, -0.1, 0.0)
        val toLine = fromLine + optimalLine.direction.normalize().multiply(3.0)

        val edgeCollision = findEdgeCollision(fromLine, toLine)

        // The next placement point is far in the future. Don't predict for now
        if (edgeCollision == null)
            return null

        val fallOffPoint = Vec3d(edgeCollision.x, player.pos.y, edgeCollision.z)
        val fallOffPointToPlayer = fallOffPoint - player.pos

        // Move the point where we want to place a bit more to the player since we ideally want to place at an edge
        // distance of 0.2 or so
        val vec3d = fallOffPoint - fallOffPointToPlayer.normalize() * optimalEdgeDist

        return vec3d
    }

    var currentOptimalLine: Line? = null

    val moveEvent = handler<MovementInputEvent> { event ->
        this.currentOptimalLine = null

        val currentInput = event.directionalInput

        if (currentInput == DirectionalInput.NONE) {
            return@handler
        }

        this.currentOptimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(event.directionalInput)

        ScaffoldBreezilyFeature.doBreezilyIfNeeded(event)
    }

    fun getFacePositionFactoryForConfig(): FaceTargetPositionFactory {
        val config = PositionFactoryConfiguration(
            player.eyes,
            if (AdvancedRotation.enabled) AdvancedRotation.xRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
            if (AdvancedRotation.enabled) AdvancedRotation.yRange.toDouble() else AdvancedRotation.DEFAULT_Y_RANGE.toDouble(),
            if (AdvancedRotation.enabled) AdvancedRotation.zRange.toDouble() else AdvancedRotation.DEFAULT_XZ_RANGE.toDouble(),
            AdvancedRotation.step.toDouble(),
            randomization,
        )

        return when (aimMode) {
            AimMode.CENTER, AimMode.GODBRIDGE, AimMode.BREEZILY -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(config, this.currentOptimalLine)
            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
        }
    }

    val timerHandler = repeatable {
        if (timer != 1f) {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleScaffold)
        }
    }

    val networkTickHandler = repeatable {
        val target = currentTarget

        val currentRotation = if (aimTimingMode == AimTimingMode.ON_TICK && target != null) {
            target.rotation
        } else {
            RotationManager.serverRotation
        }
        val currentCrosshairTarget = raycast(4.5, currentRotation)

        val currentDelay = delay.random()

        var hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offHandStack)

        if (alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        // Prioritize by all means the main hand if it has a block
        val suitableHand =
            arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull { isValidBlock(player.getStackInHand(it)) }

        if (simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving
            && SimulatePlacementAttempts.clickScheduler.goingToClick) {
            SimulatePlacementAttempts.clickScheduler.clicks {
                // By the time this reaches here, the variables are already non-null
                doPlacement(currentCrosshairTarget!!, suitableHand!!, { Swing.silentSwing }, Swing::enabled, Swing::enabled)
                true
            }
        }

        if (target == null || currentCrosshairTarget == null) {
            return@repeatable
        }

        // Does the crosshair target meet the requirements?
        if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
            || !isValidCrosshairTarget(currentCrosshairTarget)) {
            ScaffoldAutoJumpFeature.jumpIfNeeded(currentDelay)

            return@repeatable
        }

        if (ScaffoldAutoJumpFeature.shouldJump(currentDelay) &&
            currentCrosshairTarget.blockPos.offset(currentCrosshairTarget.side).y + 0.9 > player.pos.y) {
            ScaffoldAutoJumpFeature.jumpIfNeeded(currentDelay)
        }

        if (!alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        if (!hasBlockInMainHand && !hasBlockInOffHand) {
            return@repeatable
        }

        val handToInteractWith = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
        var wasSuccessful = false

        if (aimTimingMode == AimTimingMode.ON_TICK) {
            network.sendPacket(Full(player.x, player.y, player.z, currentRotation.yaw, currentRotation.pitch,
                player.isOnGround))
        }

        doPlacement(currentCrosshairTarget, handToInteractWith, { Swing.silentSwing }, {
            ScaffoldMovementPlanner.trackPlacedBlock(target)
            ScaffoldEagleTechnique.onBlockPlacement()
            ScaffoldAutoJumpFeature.onBlockPlacement()

            currentTarget = null

            wasSuccessful = true

            Swing.enabled
        }, Swing::enabled)

        if (aimTimingMode == AimTimingMode.ON_TICK) {
            network.sendPacket(Full(player.x, player.y, player.z, player.yaw, player.pitch, player.isOnGround))
        }

        if (wasSuccessful) {
            waitTicks(currentDelay)
        }
    }

    fun findBestValidHotbarSlotForTarget(): Int? {
        return (0..8).filter {
            isValidBlock(player.inventory.getStack(it))
        }.mapNotNull {
            val stack = player.inventory.getStack(it)

            if (stack.item is BlockItem) Pair(it, stack) else null
        }.maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.second, o2.second) }?.first
    }

    private fun isValidCrosshairTarget(rayTraceResult: BlockHitResult): Boolean {
        val diff = rayTraceResult.pos - player.eyes

        val side = rayTraceResult.side

        // Apply minDist
        if (side.axis != Direction.Axis.Y) {
            val dist = if (side == Direction.NORTH || side == Direction.SOUTH) diff.z else diff.x

            if (abs(dist) < minDist) {
                return false
            }
        }

        return true
    }

    fun isValidBlock(stack: ItemStack?): Boolean {
        if (stack == null) return false

        val item = stack.item

        if (item !is BlockItem) {
            return false
        }

        val block = item.block

        if (!block.defaultState.isSideSolid(world, BlockPos.ORIGIN, Direction.UP, SideShapeType.CENTER)) {
            return false
        }

        // We don't want to suicide...
        if (block is FallingBlock) {
            return false
        }

        return !DISALLOWED_BLOCKS_TO_PLACE.contains(block)
    }

    /**
     * Special handling for unfavourable blocks (like crafting tables, slabs, etc.):
     * - [ModuleScaffold]: Unfavourable blocks are only used when there is no other option left
     * - [ModuleInventoryCleaner]: Unfavourable blocks are not used as blocks by inv-cleaner.
     */
    fun isBlockUnfavourable(stack: ItemStack): Boolean {
        val item = stack.item

        if (item !is BlockItem)
            return true

        val block = item.block

        return when {
            // We dislike slippery blocks...
            block.slipperiness > 0.6F -> true
            // We dislike soul sand and slime...
            block.velocityMultiplier < 1.0F -> true
            // We hate honey...
            block.jumpVelocityMultiplier < 1.0F -> true
            // We don't want to place bee hives, chests, spawners, etc.
            block is BlockWithEntity -> true
            // We don't like slabs etc.
            !block.defaultState.isFullCube(mc.world!!, BlockPos.ORIGIN) -> true
            // Is there a hard coded answer?
            else -> block in UNFAVORABLE_BLOCKS_TO_PLACE
        }
    }

    private fun getTargetedPosition(): BlockPos {
        if (ScaffoldDownFeature.shouldGoDown) {
            return player.blockPos.add(0, -2, 0)
        }

        // In case of SameY we do want to stay at the placement Y
        return if (sameY) {
            BlockPos(player.blockPos.x, placementY, player.blockPos.z)
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
        val stack = if (suitableHand == Hand.MAIN_HAND) {
            player.mainHandStack
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
                context.blockPos.y == placementY && (hitResult.side != Direction.UP || !canPlaceOnFace)
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

    private fun handleSilentBlockSelection(hasBlockInMainHand: Boolean, hasBlockInOffHand: Boolean): Boolean {
        // Handle silent block selection
        if (autoBlock && !hasBlockInMainHand && !hasBlockInOffHand) {
            val bestMainHandSlot = findBestValidHotbarSlotForTarget()

            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(this, bestMainHandSlot, slotResetDelay)

                return true
            } else {
                SilentHotbar.resetSlot(this)
            }
        } else {
            SilentHotbar.resetSlot(this)
        }

        return hasBlockInMainHand
    }

    /**
     * Checks if the player has a block to place
     */
    fun hasBlockToBePlaced(): Boolean {
        val hasBlockInMainHand = isValidBlock(player.inventory.getStack(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offHandStack)

        return hasBlockInMainHand || hasBlockInOffHand ||
            (autoBlock && findBestValidHotbarSlotForTarget() != null)
    }

    enum class AimTimingMode(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        ON_TICK("OnTick")
    }

}
