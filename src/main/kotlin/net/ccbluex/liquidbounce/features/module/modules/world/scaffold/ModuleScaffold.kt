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
import net.ccbluex.liquidbounce.event.events.SimulatedTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.ModuleNoFall
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.*
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.utils.aiming.Rotation
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.raycast
import net.ccbluex.liquidbounce.utils.block.targetFinding.*
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.combat.CpsScheduler
import net.ccbluex.liquidbounce.utils.entity.eyes
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.kotlin.toDouble
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.SideShapeType
import net.minecraft.item.*
import net.minecraft.util.Hand
import net.minecraft.util.hit.BlockHitResult
import net.minecraft.util.hit.HitResult
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import net.minecraft.util.math.Vec3d
import net.minecraft.util.math.Vec3i
import kotlin.math.abs
import kotlin.math.floor
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

    object AutoJump : ToggleableConfigurable(this, "AutoJump", false) {
        private val predictFactor by float("PredictFactor", 0.54f, 0f..2f)
        private val useDelay by boolean("UseDelay", true)

        private val maxBlocks by int("MaxBlocks", 8, 3..17)

        private var blocksPlaced = 0

        fun onBlockPlacement() {
            blocksPlaced++
        }

        fun jumpIfNeeded(ticksUntilNextBlock: Int) {
            if (shouldJump(ticksUntilNextBlock)) {
                TickStateManager.enforcedState.enforceJump = true
                blocksPlaced = 0
            }
        }

        fun shouldJump(ticksUntilNextBlock: Int): Boolean {
            if (!enabled)
                return false
            if (!player.isOnGround)
                return false
            if (player.isSneaking)
                return false

            val extraPrediction =
                if (blocksPlaced >= maxBlocks) 1
                else if (useDelay) ticksUntilNextBlock
                else 0

            val predictedBoundingBox = player.boundingBox.offset(0.0, -1.5, 0.0)
                .offset(
                    player.velocity.multiply(
                        predictFactor.toDouble() + extraPrediction
                    )
                )

            return world.getBlockCollisions(player, predictedBoundingBox).none()
        }
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
        tree(AutoJump)
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

    private val rotationUpdateHandler = handler<SimulatedTickEvent> {
        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStick =
            if (blockInHotbar == null) {
                ItemStack(Items.SANDSTONE, 64)
            } else {
                player.inventory.getStack(blockInHotbar)
            }

        val optimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(DirectionalInput(player.input))

        // Prioritze the block that is closest to the line, if there was no line found, prioritize the nearest block
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

        // Debug stuff
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

        chat(optimalLine?.direction.toString())

        RotationManager.aimAt(
            if (this.aimMode.get() == AimMode.GODBRIDGE)
                Rotation(floor(target.rotation.yaw / 90) * 90 + 45, 75f)
            else target.rotation,
            considerInventory = !ignoreOpenInventory,
            configurable = rotationsConfigurable,
        )
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

        return when (aimMode.get()) {
            AimMode.CENTER -> CenterTargetPositionFactory
            AimMode.GODBRIDGE -> CenterTargetPositionFactory
            AimMode.RANDOM -> RandomTargetPositionFactory(config)
            AimMode.STABILIZED -> StabilizedRotationTargetPositionFactory(
                config,
                ScaffoldMovementPlanner.getOptimalMovementLine(DirectionalInput(player.input)),
            )

            AimMode.NEAREST_ROTATION -> NearestRotationTargetPositionFactory(config)
        }
    }

    val moveHandler = repeatable {
        Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE)
    }

    val networkTickHandler = repeatable {
        val target = currentTarget

        val currentRotation = RotationManager.rotationForServer
        val currentCrosshairTarget = raycast(4.5, currentRotation)

        val currentDelay = delay.random()

        // Prioritize by all means the main hand if it has a block
        val suitableHand =
            arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull { isValidBlock(player.getStackInHand(it)) }

        repeat(
            cpsScheduler.clicks(
                { simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving },
                SimulatePlacementAttempts.cps,
            ),
        ) {
            // By the time this reaches here, the variables are already non-null
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

        // Does the crosshair target meet the requirements?
        if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget) ||
            !isValidCrosshairTarget(currentCrosshairTarget)
        ) {
            AutoJump.jumpIfNeeded(currentDelay)

            return@repeatable
        }

        if (AutoJump.shouldJump(currentDelay)
            && currentCrosshairTarget.blockPos.offset(currentCrosshairTarget.side).y + 0.9 > player.pos.y
        ) {
            AutoJump.jumpIfNeeded(currentDelay)
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
        AutoJump.onBlockPlacement()

        if (player.isOnGround) {
            player.velocity.x *= speedModifier
            player.velocity.z *= speedModifier
        }
        if (result.shouldSwingHand() && swing) {
            player.swingHand(handToInteractWith)
        }

        currentTarget = null

        waitTicks(currentDelay)
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
}
