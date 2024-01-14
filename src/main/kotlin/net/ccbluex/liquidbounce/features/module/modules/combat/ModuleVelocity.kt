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

import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerFeature
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
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.SideShapeType
import net.minecraft.item.*
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
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

    private val grimMoment by boolean("GrimExploit", true)

    object SimulatePlacementAttempts : ToggleableConfigurable(this, "SimulatePlacementAttempts", false) {
        internal val clickScheduler = tree(ClickScheduler(ModuleScaffold, false, maxCps = 100))
        val failedAttemptsOnly by boolean("FailedAttemptsOnly", true)
    }

    private val silent by boolean("Silent", true)
    private val alwaysHoldBlock by boolean("AlwaysHoldBlock", false)
    private val slotResetDelay by int("SlotResetDelay", 5, 0..40)
    private var delay by intRange("Delay", 3..5, 0..40)

    private val swing by boolean("Swing", true)

    // Rotation
    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val aimMode by enumChoice("RotationMode", AimMode.STABILIZED, AimMode.values())

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

    val sameY by boolean("SameY", false)
    private var currentTarget: BlockPlacementTarget? = null

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    private val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    init {
        tree(SimulatePlacementAttempts)
        tree(ScaffoldSlowFeature)
        tree(ScaffoldSpeedLimiterFeature)
        tree(ScaffoldEagleFeature)
        tree(ScaffoldDownFeature)
        tree(ScaffoldAutoJumpFeature)
        tree(AdvancedRotation)
        tree(ScaffoldStabilizeMovementFeature)
        tree(ScaffoldTowerFeature)
    }

    private var randomization = Random.nextDouble(-0.02, 0.02)
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
        ScaffoldMovementPlanner.reset()
        SilentHotbar.resetSlot(this)
    }

    private val rotationUpdateHandler = handler<SimulatedTickEvent> {
        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStack = if (blockInHotbar == null) {
            ItemStack(Items.SANDSTONE, 64)
        } else {
            player.inventory.getStack(blockInHotbar)
        }

        val optimalLine = this.currentOptimalLine

        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityGetter: (Vec3i) -> Double = if (optimalLine != null) {
            { vec -> -optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        val searchOptions = BlockPlacementTargetFindingOptions(
            if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
            bestStack,
            getFacePositionFactoryForConfig(),
            priorityGetter,
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

        val rotation = if (aimMode == AimMode.GODBRIDGE) {
            ScaffoldGodBridgeFeature.optimizeRotation(target)
        } else {
            target?.rotation
        } ?: return@handler

        if (grimMoment) {
            return@handler
        }

        RotationManager.aimAt(
            rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotationsConfigurable,
            provider = this@ModuleScaffold,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
        )
    }

    var currentOptimalLine: Line? = null

    val moveEvent = handler<MovementInputEvent> { event ->
        this.currentOptimalLine = null

        val currentInput = event.directionalInput

        if (currentInput == DirectionalInput.NONE) {
            return@handler
        }

        this.currentOptimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(event.directionalInput)
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
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.GODBRIDGE -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(config, this.currentOptimalLine)
            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
        }
    }

    val timerHandler = repeatable {
        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleScaffold)
    }

    val networkTickHandler = repeatable {
        val target = currentTarget

        val currentRotation = RotationManager.serverRotation
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
                doPlacement(currentCrosshairTarget!!, suitableHand!!, ModuleScaffold::swing, ModuleScaffold::swing)
                true
            }
        }

        if (target == null || currentCrosshairTarget == null) {
            return@repeatable
        }

        // Does the crosshair target meet the requirements?
        if ((!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
                || !isValidCrosshairTarget(currentCrosshairTarget)) && !grimMoment
        ) {
            ScaffoldAutoJumpFeature.jumpIfNeeded(currentDelay)

            return@repeatable
        }

        if (ScaffoldAutoJumpFeature.shouldJump(currentDelay) &&
            currentCrosshairTarget.blockPos.offset(currentCrosshairTarget.side).y + 0.9 > player.pos.y
        ) {
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

        val shouldGrim = raycast(4.5, target.rotation)

        if (grimMoment && shouldGrim != null && isValidCrosshairTarget(shouldGrim) && target.doesCrosshairTargetFullFillRequirements(shouldGrim)) {

            network.sendPacket(PlayerMoveC2SPacket.Full(
                player.x, player.y, player.z,
                target.rotation.yaw, target.rotation.pitch,
                player.isOnGround
            ))

            doPlacement(shouldGrim, handToInteractWith, {
                ScaffoldMovementPlanner.trackPlacedBlock(target)
                ScaffoldEagleFeature.onBlockPlacement()
                ScaffoldAutoJumpFeature.onBlockPlacement()

                currentTarget = null

                wasSuccessful = true

                swing
            }, ModuleScaffold::swing)

            network.sendPacket(PlayerMoveC2SPacket.Full(
                player.x, player.y, player.z,
                player.yaw, player.pitch,
                player.isOnGround
            ))
        } else if (!grimMoment) {
            doPlacement(currentCrosshairTarget, handToInteractWith, {
                ScaffoldMovementPlanner.trackPlacedBlock(target)
                ScaffoldEagleFeature.onBlockPlacement()
                ScaffoldAutoJumpFeature.onBlockPlacement()

                currentTarget = null

                wasSuccessful = true

                swing
            }, ModuleScaffold::swing)
        }

        if (wasSuccessful) {
            waitTicks(currentDelay)
        }
    }

    private fun findBestValidHotbarSlotForTarget(): Int? {
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

    private fun handleSilentBlockSelection(hasBlockInMainHand: Boolean, hasBlockInOffHand: Boolean): Boolean {
        // Handle silent block selection
        if (silent && !hasBlockInMainHand && !hasBlockInOffHand) {
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
}
