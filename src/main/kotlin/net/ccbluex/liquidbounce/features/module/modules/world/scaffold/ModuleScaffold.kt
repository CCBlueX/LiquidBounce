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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.config.NoneChoice
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
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
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoJumpFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldBreezilyFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldGodBridgeFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSlowFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSpeedLimiterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldEagleTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldTellyTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown
import net.ccbluex.liquidbounce.render.engine.Color4b
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
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.*
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.block.BlockWithEntity
import net.minecraft.block.FallingBlock
import net.minecraft.block.SideShapeType
import net.minecraft.entity.EntityPose
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

    // Silent block selection
    object AutoBlock : ToggleableConfigurable(this, "AutoBlock", true) {
        val alwaysHoldBlock by boolean("Always", false)
        val slotResetDelay by int("SlotResetDelay", 5, 0..40, "ticks")
    }

    init {
        tree(AutoBlock)
    }

    // Aim mode
    private val aimMode by enumChoice("RotationMode", AimMode.STABILIZED)
    private val aimTimingMode by enumChoice("AimTiming", AimTimingMode.NORMAL)
    internal val technique = choices(
        "Technique", ScaffoldNormalTechnique,
        arrayOf(ScaffoldNormalTechnique, ScaffoldEagleTechnique, ScaffoldTellyTechnique)
    )

    init {
        tree(ScaffoldMovementPrediction)
        tree(ScaffoldAutoJumpFeature)
        tree(ScaffoldBreezilyFeature)
    }

    @Suppress("UnusedPrivateProperty")
    val towerMode = choices("Tower", {
        it.choices[0] // None
    }) {
        arrayOf(NoneChoice(it), ScaffoldTowerMotion, ScaffoldTowerPulldown)
    }

    // SafeWalk feature - uses the SafeWalk module as a base
    @Suppress("UnusedPrivateProperty")
    private val safeWalkMode = choices("SafeWalk", {
        it.choices[1] // Safe mode
    }, ModuleSafeWalk::createChoices)

    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    private val timer by float("Timer", 1f, 0.01f..10f)
    private val sameY by boolean("SameY", false)

    private val rotationsConfigurable = tree(RotationsConfigurable())
    private val ignoreOpenInventory by boolean("IgnoreOpenInventory", true)

    private var currentTarget: BlockPlacementTarget? = null

    private val INVESTIGATE_DOWN_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1, -2, 2))
    private val NORMAL_INVESTIGATION_OFFSETS: List<Vec3i> = commonOffsetToInvestigate(listOf(0, -1, 1))

    object Swing : ToggleableConfigurable(this, "Swing", true) {
        val swingSilent by boolean("Silent", false)
    }

    init {
        tree(SimulatePlacementAttempts)
        tree(Swing)
        tree(ScaffoldSlowFeature)
        tree(ScaffoldSpeedLimiterFeature)
        tree(ScaffoldDownFeature)
        tree(ScaffoldStabilizeMovementFeature)
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
        ScaffoldMovementPrediction.reset()

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

        val predictedPos = ScaffoldMovementPrediction.getPredictedPlacementPos(optimalLine) ?: player.pos
        // Check if the player is probably going to sneak at the predicted position
        val predictedPose =
            if (ScaffoldEagleTechnique.isActive && ScaffoldEagleTechnique.shouldEagle(DirectionalInput(player.input))) {
                EntityPose.CROUCHING
            } else {
                EntityPose.STANDING
            }

        ModuleDebug.debugGeometry(
            ModuleScaffold,
            "predictedPos",
            ModuleDebug.DebuggedPoint(predictedPos, Color4b(0, 255, 0, 255), size = 0.1)
        )

        // Prioritize the block that is closest to the line, if there was no line found, prioritize the nearest block
        val priorityGetter: (Vec3i) -> Double = if (optimalLine != null) {
            { vec -> -optimalLine.squaredDistanceTo(Vec3d.of(vec).add(0.5, 0.5, 0.5)) }
        } else {
            BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE
        }

        // Face position factory for current config
        val facePositionFactory = getFacePositionFactoryForConfig(predictedPos, predictedPose)

        val searchOptions =
            BlockPlacementTargetFindingOptions(
                if (ScaffoldDownFeature.shouldGoDown) INVESTIGATE_DOWN_OFFSETS else NORMAL_INVESTIGATION_OFFSETS,
                bestStack,
                facePositionFactory,
                priorityGetter,
                predictedPos,
                predictedPose
            )

        currentTarget = findBestBlockPlacementTarget(getTargetedPosition(predictedPos.toBlockPos()), searchOptions)

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
                    from = a,
                    to = b,
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

    fun getFacePositionFactoryForConfig(predictedPos: Vec3d, predictedPose: EntityPose): FaceTargetPositionFactory {
        val config = PositionFactoryConfiguration(
            predictedPos.add(0.0, player.getEyeHeight(predictedPose).toDouble(), 0.0),
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

        if (AutoBlock.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        // Prioritize by all means the main hand if it has a block
        val suitableHand =
            arrayOf(Hand.MAIN_HAND, Hand.OFF_HAND).firstOrNull { isValidBlock(player.getStackInHand(it)) }

        if (simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving
            && SimulatePlacementAttempts.clickScheduler.goingToClick
        ) {
            SimulatePlacementAttempts.clickScheduler.clicks {
                // By the time this reaches here, the variables are already non-null
                doPlacement(
                    currentCrosshairTarget!!, suitableHand!!, Swing.swingSilent,
                    Swing::enabled, Swing::enabled
                )
                true
            }
        }


        if (target == null || currentCrosshairTarget == null) {
            return@repeatable
        }

        // Does the crosshair target meet the requirements?
        if (!target.doesCrosshairTargetFullFillRequirements(currentCrosshairTarget)
            || !isValidCrosshairTarget(currentCrosshairTarget)
        ) {
            ScaffoldAutoJumpFeature.jumpIfNeeded(currentDelay)

            return@repeatable
        }

        if (ScaffoldAutoJumpFeature.shouldJump(currentDelay) &&
            currentCrosshairTarget.blockPos.offset(currentCrosshairTarget.side).y + 0.9 > player.pos.y
        ) {
            ScaffoldAutoJumpFeature.jumpIfNeeded(currentDelay)
        }

        if (!AutoBlock.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        if (!hasBlockInMainHand && !hasBlockInOffHand) {
            return@repeatable
        }

        val handToInteractWith = if (hasBlockInMainHand) Hand.MAIN_HAND else Hand.OFF_HAND
        var wasSuccessful = false

        if (aimTimingMode == AimTimingMode.ON_TICK) {
            network.sendPacket(
                Full(
                    player.x, player.y, player.z, currentRotation.yaw, currentRotation.pitch,
                    player.isOnGround
                )
            )
        }

        // Take the fall off position before placing the block
        val previousFallOffPos = currentOptimalLine?.let { l -> ScaffoldMovementPrediction.getFallOffPositionOnLine(l) }

        doPlacement(currentCrosshairTarget, handToInteractWith, Swing.swingSilent, {
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
            ScaffoldMovementPrediction.onPlace(currentOptimalLine, previousFallOffPos)

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

    private fun getTargetedPosition(blockPos: BlockPos): BlockPos {
        if (ScaffoldDownFeature.shouldGoDown) {
            return blockPos.add(0, -2, 0)
        }

        // In case of SameY we do want to stay at the placement Y
        return if (sameY) {
            BlockPos(blockPos.x, placementY, blockPos.z)
        } else {
            blockPos.add(0, -1, 0)
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
        if (AutoBlock.enabled && !hasBlockInMainHand && !hasBlockInOffHand) {
            val bestMainHandSlot = findBestValidHotbarSlotForTarget()

            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(this, bestMainHandSlot, AutoBlock.slotResetDelay)

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
            (AutoBlock.enabled && findBestValidHotbarSlotForTarget() != null)
    }

    enum class AimTimingMode(override val choiceName: String) : NamedChoice {
        NORMAL("Normal"),
        ON_TICK("OnTick")
    }

}
