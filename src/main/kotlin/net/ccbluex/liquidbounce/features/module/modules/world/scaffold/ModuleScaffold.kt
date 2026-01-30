/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleSafeWalk
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.considerInventory
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAccelerationFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoBlockFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldBlinkFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldJumpStrafe
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldLedgeExtension
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSpeedLimiterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStrafeFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ledge
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldBreezilyTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldExpandTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldGodBridgeTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.ScaffoldNormalTechnique
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.techniques.normal.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerNone
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerVulcan
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetBlockPos
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.item.PreferAverageHardBlocks
import net.ccbluex.liquidbounce.utils.item.PreferFavourableBlocks
import net.ccbluex.liquidbounce.utils.item.PreferFullCubeBlocks
import net.ccbluex.liquidbounce.utils.item.PreferSolidBlocks
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.item.PreferWalkableBlocks
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.geometry.Line
import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.toVec3d
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.render.placement.PlacementRenderer
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket.PosRot
import net.minecraft.world.InteractionHand
import net.minecraft.world.entity.Pose
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.item.context.UseOnContext
import net.minecraft.world.level.block.Block
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.shapes.Shapes
import kotlin.math.abs

/**
 * Scaffold module
 *
 * Places blocks under you.
 */
@Suppress("TooManyFunctions")
object ModuleScaffold : ClientModule("Scaffold", ModuleCategories.WORLD) {

    private val delay by intRange("Delay", 0..0, 0..40, "ticks")
    private val minDist by float("MinDist", 0.0f, 0.0f..0.25f)
    private val timer by float("Timer", 1f, 0.01f..10f)

    init {
        tree(ScaffoldBlockItemSelection)
        tree(ScaffoldAutoBlockFeature)
        tree(ScaffoldMovementPrediction)
    }

    internal val technique = choices(
        "Technique",
        ScaffoldNormalTechnique,
        arrayOf(
            ScaffoldNormalTechnique,
            ScaffoldExpandTechnique,
            ScaffoldGodBridgeTechnique,
            ScaffoldBreezilyTechnique
        )
    ).apply(::tagBy)

    private val sameYMode by enumChoice("SameY", SameYMode.OFF)

    @Suppress("unused")
    private enum class SameYMode(
        override val tag: String,
        val getTargetedBlockPos: (BlockPos) -> BlockPos?
    ) : Tagged {

        OFF("Off", { null }),

        /**
         * Places blocks at the same Y level as the player
         */
        ON("On", { blockPos -> blockPos.copy(y = placementY) }),

        /**
         * Places blocks at the same Y level as the player, but only if the player is not falling
         */
        FALLING("Falling", { blockPos -> blockPos.copy(y = placementY).takeIf { player.deltaMovement.y < 0.2 } }),

        /**
         * Similar to FALLING, but only when a certain velocity is triggered and after
         * 2 jumps
         */
        HYPIXEL("Hypixel", { blockPos ->
            if (player.deltaMovement.y == -0.15233518685055708 && jumps >= 2) {
                jumps = 0

                blockPos.copy(y = startY)
            } else {
                blockPos.copy(y = startY - 1)
            }
        })

    }

    /**
     * Scaffold tower mode
     */
    val towerMode = choices("Tower", 0) {
        arrayOf(
            ScaffoldTowerNone,
            ScaffoldTowerMotion,
            ScaffoldTowerPulldown,
            ScaffoldTowerKarhu,
            ScaffoldTowerVulcan,
            ScaffoldTowerHypixel
        )
    }

    internal val isTowering: Boolean
        get() = if (towerMode.activeMode != ScaffoldTowerNone && mc.options.keyJump.isDown) {
            this.wasTowering = true
            true
        } else {
            false
        }
    private var wasTowering: Boolean = false

    // SafeWalk feature - uses the SafeWalk module as a base
    @Suppress("unused")
    private val safeWalkMode = choices("SafeWalk", 1, ModuleSafeWalk::safeWalkChoices)

    internal object ScaffoldRotationValueGroup : RotationsValueGroup(this) {

        val considerInventory by boolean("ConsiderInventory", false)
        val rotationTiming by enumChoice("RotationTiming", NORMAL)

        enum class RotationTimingMode(override val tag: String) : Tagged {

            /**
             * Rotates the player before the block is placed
             */
            NORMAL("Normal"),

            /**
             * Rotates the player on the tick the block is placed
             */
            ON_TICK("OnTick"),

            /**
             * Similar to ON_TICK, but the player will keep the rotation after placing
             */
            ON_TICK_SNAP("OnTickSnap")

        }

    }

    private var currentTarget: BlockPlacementTarget? = null

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE)

    private object SimulatePlacementAttempts : ToggleableValueGroup(this, "SimulatePlacementAttempts", false) {
        val clicker = tree(Clicker(ModuleScaffold, mc.options.keyUse, null, maxCps = 100))
        val failedAttemptsOnly by boolean("FailedAttemptsOnly", true)
    }

    init {
        tree(ScaffoldRotationValueGroup)
        tree(ScaffoldSprintControlFeature)
        tree(SimulatePlacementAttempts)
        tree(ScaffoldAccelerationFeature)
        tree(ScaffoldStrafeFeature)
        tree(ScaffoldJumpStrafe)
        tree(ScaffoldSpeedLimiterFeature)
        tree(ScaffoldBlinkFeature)
    }

    /**
     * Temporarily turns on [net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed]
     * while Scaffold is enabled.
     */
    val autoSpeed by boolean("AutoSpeed", false)

    private var ledge by boolean("Ledge", true)

    private val renderer = tree(PlacementRenderer("Render", true, this, keep = false))

    private var placementY = 0
    private var forceSneak = 0
    private var startY = 0
    private var jumps = 0

    private var nextBlock: Block? = null

    val blockCount: Int
        get() {
            fun ItemStack.blockCount() = if (isValidBlock(this)) this.count else 0

            return player.offhandItem.blockCount() + if (ScaffoldAutoBlockFeature.enabled) {
                findPlaceableSlots().sumOf { it.value.blockCount() }
            } else {
                player.inventory.getItem(player.inventory.selectedSlot).blockCount()
            }
        }

    val isBlockBelow: Boolean
        get() {
            // Check if there is a collision box below the player
            // In this case we expand the bounding box by 0.5 in all directions and check if there is a collision
            // This might cause for "Spider-like" behavior, but it's the most reliable way to check
            // and usually the scaffold should start placing blocks
            return world.getBlockCollisions(
                player,
                player.boundingBox.inflate(0.5, 0.0, 0.5).move(0.0, -1.05, 0.0)
            ).any { shape -> shape != Shapes.empty() }
        }

    /**
     * This comparator will estimate the value of a block. If this comparator says that Block A > Block B, Scaffold will
     * prefer Block A over Block B.
     * The chain will prefer the block that is solid. If both are solid, it goes to the next criteria
     * (in this case full cube) and so on
     */
    private val BLOCK_COMPARATOR_FOR_HOTBAR =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks(neutralRange = true),
            PreferStackSize.PREFER_MORE,
            PreferAverageHardBlocks(neutralRange = false),
        )
    @JvmField
    val BLOCK_COMPARATOR_FOR_INVENTORY =
        ComparatorChain(
            PreferFavourableBlocks,
            PreferSolidBlocks,
            PreferFullCubeBlocks,
            PreferWalkableBlocks,
            PreferAverageHardBlocks(neutralRange = true),
            PreferStackSize.PREFER_FEWER,
            PreferAverageHardBlocks(neutralRange = false),
        )

    override fun onEnabled() {
        // Placement Y is the Y coordinate of the block below the player
        placementY = player.blockPosition().y - 1
        startY = player.blockPosition().y
        jumps = 2

        ScaffoldMovementPlanner.reset()
        ScaffoldMovementPrediction.reset()

        super.onEnabled()
    }

    override fun onDisabled() {
        NoFallBlink.waitUntilGround = false
        ScaffoldMovementPlanner.reset()
        SilentHotbar.resetSlot(this)
        nextBlock = null
        updateRenderCount(null)
        forceSneak = 0
        renderer.clearSilently()
    }

    private fun updateRenderCount(count: Int?) {
        EventManager.callEvent(BlockCountChangeEvent(nextBlock, count))
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        NoFallBlink.waitUntilGround = true

        val blockInHotbar = findBestValidHotbarSlotForTarget()

        val bestStack = if (blockInHotbar == null) {
            nextBlock = null
            ItemStack(Items.SANDSTONE, 64)
        } else {
            player.inventory.getItem(blockInHotbar).also {
                nextBlock = it.getBlock()
            }
        }

        val optimalLine = this.currentOptimalLine

        val predictedPos = ScaffoldMovementPrediction.getPredictedPlacementPos(optimalLine) ?: player.position()
        // Check if the player is probably going to sneak at the predicted position
        val predictedPose =
            if (ScaffoldEagleFeature.enabled && ScaffoldEagleFeature.shouldEagle(DirectionalInput(player.input))) {
                Pose.CROUCHING
            } else {
                Pose.STANDING
            }

        debugGeometry("predictedPos") {
            ModuleDebug.DebuggedPoint(predictedPos, Color4b(0, 255, 0, 255), size = 0.1)
        }

        val technique = if (isTowering) {
            ScaffoldNormalTechnique
        } else {
            technique.activeMode
        }

        val target = technique.findPlacementTarget(predictedPos, predictedPose, optimalLine, bestStack)
            .also { this.currentTarget = it }

        if (optimalLine != null && target != null) {
            debugGeometry("lineToBlock") {
                // Debug stuff
                val b = target.placedBlock.toVec3d(0.5, 1.0, 0.5)
                val a = optimalLine.getNearestPointTo(b)

                // Debug the line a-b
                ModuleDebug.DebuggedLineSegment(
                    from = a,
                    to = b,
                    Color4b(255, 0, 0, 255),
                )
            }
        }

        // Do not aim yet in SKIP mode, since we want to aim at the block only when we are about to place it
        if (rotationTiming == NORMAL) {
            val rotation = technique.getRotations(target)

            RotationManager.setRotationTarget(
                rotation ?: return@handler,
                considerInventory = considerInventory,
                valueGroup = ScaffoldRotationValueGroup,
                provider = this@ModuleScaffold,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
        }
    }

    var currentOptimalLine: Line? = null
    var rawInput = DirectionalInput.NONE

    @Suppress("unused")
    private val handleMovementInput = handler<MovementInputEvent>(
        priority = EventPriorityConvention.MODEL_STATE
    ) { event ->
        this.currentOptimalLine = null
        this.rawInput = event.directionalInput

        val currentInput = event.directionalInput

        if (currentInput == DirectionalInput.NONE) {
            return@handler
        }

        this.currentOptimalLine = ScaffoldMovementPlanner.getOptimalMovementLine(event.directionalInput)
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(
        // Runs after the model state
        priority = EventPriorityConvention.SAFETY_FEATURE
    ) { event ->
        if (forceSneak > 0) {
            event.sneak = true
            forceSneak--
        }

        // Ledge feature - AutoJump and AutoSneak
        if (ledge) {
            val technique = if (isTowering) {
                ScaffoldNormalTechnique
            } else {
                technique.activeMode
            }

            val ledgeAction = ledge(
                this.currentTarget,
                RotationManager.currentRotation ?: player.rotation,
                technique as? ScaffoldLedgeExtension
            )

            if (ledgeAction.jump) {
                event.jump = true
            }

            if (ledgeAction.stopInput) {
                event.directionalInput = DirectionalInput.NONE
            }

            if (ledgeAction.stepBack) {
                event.directionalInput = event.directionalInput.copy(
                    forwards = false,
                    backwards = true
                )
            }

            if (ledgeAction.sneakTime > forceSneak) {
                event.sneak = true
                forceSneak = ledgeAction.sneakTime
            }
        }
    }

    @Suppress("unused")
    private val timerHandler = handler<GameTickEvent> {
        if (timer != 1f) {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleScaffold)
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        updateRenderCount(blockCount)

        if (player.onGround()) {
            // Placement Y is the Y coordinate of the block below the player
            placementY = player.blockPosition().y - 1
            jumps++
            wasTowering = false
        }

        if (mc.options.keyJump.isDown) {
            startY = player.blockPosition().y
            jumps = 2
        }

        debugParameter("IsTowering") { isTowering }
        debugParameter("WasTowering") { wasTowering }

        val target = currentTarget

        val computedRotation = if (target != null) {
            technique.activeMode.getRotations(target)
        } else {
            null
        }

        val currentRotation = if ((rotationTiming == ON_TICK || rotationTiming == ON_TICK_SNAP) && target != null) {
            computedRotation ?: (RotationManager.currentRotation ?: player.rotation)
        } else {
            RotationManager.currentRotation ?: player.rotation
        }.normalize()
        val currentCrosshairTarget = technique.activeMode.getCrosshairTarget(target, currentRotation)
        val currentDelay = delay.random()

        var hasBlockInMainHand = isValidBlock(player.inventory.getItem(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offhandItem)

        if (ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        // Prioritize by all means the main hand if it has a block
        val suitableHand = InteractionHand.entries.firstOrNull {
            isValidBlock(player.getItemInHand(it))
        }

        fun commonPlaceSucceed(placed: BlockPos) {
            ScaffoldMovementPlanner.trackPlacedBlock(placed)
            renderer.addBlock(placed)
            ScaffoldEagleFeature.onBlockPlacement()
            ScaffoldBlinkFeature.onBlockPlacement()
            ScaffoldSprintControlFeature.onBlockPlacement()
        }

        if (simulatePlacementAttempts(currentCrosshairTarget, suitableHand) && player.moving
            && SimulatePlacementAttempts.clicker.isClickTick
        ) {
            SimulatePlacementAttempts.clicker.click {
                doPlacement(currentCrosshairTarget!!, suitableHand!!, {
                    commonPlaceSucceed(currentCrosshairTarget.targetBlockPos)
                    true
                }, swingMode = swingMode)
                true
            }
        }

        if (target == null || currentCrosshairTarget == null) {
            return@tickHandler
        }

        // Does the crosshair target meet the requirements?
        if (!target.doesCrosshairTargetMatchRequirements(currentCrosshairTarget) ||
            !isValidCrosshairTarget(currentCrosshairTarget)
        ) {
            return@tickHandler
        }

        if (!ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        if (!hasBlockInMainHand && !hasBlockInOffHand) {
            return@tickHandler
        }

        val handToInteractWith = if (hasBlockInMainHand) InteractionHand.MAIN_HAND else InteractionHand.OFF_HAND
        var wasSuccessful = false

        if (rotationTiming == ON_TICK || rotationTiming == ON_TICK_SNAP) {
            // Check if server rotation matches the current rotation
            if (currentRotation != RotationManager.serverRotation) {
                network.send(
                    PosRot(
                        player.x, player.y, player.z,
                        currentRotation.yaw,
                        currentRotation.pitch,
                        player.onGround(),
                        player.horizontalCollision
                    )
                )
            }

            if (rotationTiming == ON_TICK_SNAP) {
                RotationManager.setRotationTarget(
                    currentRotation,
                    considerInventory = considerInventory,
                    valueGroup = ScaffoldRotationValueGroup,
                    provider = this@ModuleScaffold,
                    priority = Priority.IMPORTANT_FOR_PLAYER_LIFE
                )
            }
        }

        // Take the fall off position before placing the block
        val previousFallOffPos = currentOptimalLine?.let { l -> ScaffoldMovementPrediction.getFallOffPositionOnLine(l) }

        doPlacement(currentCrosshairTarget, handToInteractWith, {
            commonPlaceSucceed(target.placedBlock)
            currentTarget = null
            wasSuccessful = true
            true
        }, swingMode = swingMode)

        if (rotationTiming == ON_TICK && RotationManager.serverRotation != player.rotation) {
            network.send(
                PosRot(
                    player.x, player.y, player.z, player.withFixedYaw(currentRotation), player.xRot, player.onGround(),
                    player.horizontalCollision
                )
            )
        }

        if (wasSuccessful) {
            ScaffoldMovementPrediction.onPlace(currentOptimalLine, previousFallOffPos)

            waitTicks(currentDelay)
        }
    }

    private fun findPlaceableSlots() = buildList(9) {
        for (i in 0..8) {
            val stack = player.inventory.getItem(i)

            if (isValidBlock(stack)) {
                add(IndexedValue(i, stack))
            }
        }
    }

    private fun findBestValidHotbarSlotForTarget(): Int? {
        val placeableSlots = findPlaceableSlots()
        val doNotUseBelowCount = ScaffoldAutoBlockFeature.doNotUseBelowCount

        val (slot, _) = placeableSlots
            .filter { (_, stack) -> stack.count > doNotUseBelowCount }
            .maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.value, o2.value) }
            ?: placeableSlots.maxWithOrNull { o1, o2 -> BLOCK_COMPARATOR_FOR_HOTBAR.compare(o1.value, o2.value) }
            ?: return null

        return slot
    }

    internal fun isValidCrosshairTarget(rayTraceResult: BlockHitResult): Boolean {
        val diff = rayTraceResult.location - player.eyePosition

        val side = rayTraceResult.direction

        // Apply minDist
        if (side.axis != Direction.Axis.Y) {
            val dist = if (side == Direction.NORTH || side == Direction.SOUTH) diff.z else diff.x

            if (abs(dist) < minDist) {
                return false
            }
        }

        return true
    }

    internal fun getTargetedPosition(blockPos: BlockPos) = when {
        isTowering || wasTowering -> towerMode.activeMode.getTargetedPosition(blockPos)
        ScaffoldDownFeature.running && ScaffoldDownFeature.shouldGoDown ->
            blockPos.offset(0, -2, 0)
        ScaffoldCeilingFeature.running && ScaffoldCeilingFeature.canConstructCeiling() ->
            blockPos.offset(0, 3, 0)
        player.input.keyPresses.jump && (!player.moving || player.horizontalCollision) ->
            blockPos.offset(0, -1, 0)
        else -> sameYMode.getTargetedBlockPos(blockPos)
            ?: blockPos.offset(0, -1, 0)
    }

    private fun simulatePlacementAttempts(
        hitResult: BlockHitResult?,
        suitableHand: InteractionHand?,
    ): Boolean {
        val stack = suitableHand?.let(player::getItemInHand) ?: return false

        if (hitResult == null || !SimulatePlacementAttempts.enabled) {
            return false
        }

        if (hitResult.type != HitResult.Type.BLOCK) {
            return false
        }

        val context = UseOnContext(player, suitableHand, hitResult)

        val canPlaceOnFace = (stack.item as BlockItem).getPlacementState(BlockPlaceContext(context)) != null

        return when {
            SimulatePlacementAttempts.failedAttemptsOnly -> {
                !canPlaceOnFace
            }

            sameYMode != SameYMode.OFF -> {
                context.clickedPos.y == placementY && (hitResult.direction != Direction.UP || !canPlaceOnFace)
            }

            else -> {
                val isTargetUnderPlayer = context.clickedPos.y <= player.blockY - 1
                val isTowering =
                    context.clickedPos.y == player.blockY - 1 &&
                        canPlaceOnFace &&
                        context.clickedFace == Direction.UP

                isTargetUnderPlayer && !isTowering
            }
        }
    }

    private fun handleSilentBlockSelection(hasBlockInMainHand: Boolean, hasBlockInOffHand: Boolean): Boolean {
        // Handle silent block selection
        if (ScaffoldAutoBlockFeature.enabled && !hasBlockInMainHand && !hasBlockInOffHand) {
            val bestMainHandSlot = findBestValidHotbarSlotForTarget()

            if (bestMainHandSlot != null) {
                SilentHotbar.selectSlotSilently(
                    this, bestMainHandSlot,
                    ScaffoldAutoBlockFeature.slotResetDelay
                )

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
