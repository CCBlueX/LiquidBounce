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

import kotlinx.coroutines.Job
import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.v26_3.toAgent
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.BlockCountChangeEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.launchSequence
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes.NoFallBlink
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedLineSegment
import net.ccbluex.liquidbounce.features.module.modules.render.debug.DebuggedPoint
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugGeometry
import net.ccbluex.liquidbounce.features.module.modules.render.debug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleClutch
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.NORMAL
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.RotationTimingMode.ON_TICK_SNAP
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.considerInventory
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold.ScaffoldRotationValueGroup.rotationTiming
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection.isValidBlock
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridging
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingBreezily
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingGodBridge
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingNinja
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingNormal
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.bridging.ScaffoldBridgingTelly
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAccelerationFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldAutoBlockFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldBlinkFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldCeilingFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldClutchFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldDownFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldEagleFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldExpandFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldHeadHitterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldJumpStrafe
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldMovementPrediction
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSpeedLimiterFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldSprintControlFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStabilizeMovementFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features.ScaffoldStrafeFeature
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerHypixel
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerKarhu
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerMotion
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerNone
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerPulldown
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.tower.ScaffoldTowerVulcan
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.aiming.data.Surface
import net.ccbluex.liquidbounce.utils.aiming.lookAt
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil
import net.ccbluex.liquidbounce.utils.aiming.utils.RotationUtil.angleDifference
import net.ccbluex.liquidbounce.utils.aiming.utils.withFixedYaw
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetBlockPos
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTarget
import net.ccbluex.liquidbounce.utils.block.targetfinding.Visibility
import net.ccbluex.liquidbounce.utils.clicking.Clicker
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.fromClientPlayer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.entity.wouldBeCloseToFallOff
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
import net.ccbluex.liquidbounce.utils.math.allEmpty
import net.ccbluex.liquidbounce.utils.math.topCenter
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
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
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max

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

    /** How the player bridges. Old configs saved Normal, GodBridge or Breezily as the technique. */
    val bridging = modes(object : EventListener {
        override fun parent() = ModuleScaffold

        // Not during a catch by [ModuleClutch]: stopping at an edge without sneaking gets flagged by Grim
        override val running: Boolean
            get() = super.running && ModuleClutch.fall == null
    }, "Bridging", listOf("Technique"), { 0 }) {
        arrayOf<ScaffoldBridging>(
            ScaffoldBridgingNormal,
            ScaffoldBridgingNinja,
            ScaffoldBridgingGodBridge,
            ScaffoldBridgingBreezily,
            ScaffoldBridgingTelly,
        )
    }.apply(::tagBy)
    internal val visibility by enumChoice("Visibility", Visibility.VISIBLE)

    init {
        tree(ScaffoldDownFeature)
        tree(ScaffoldStabilizeMovementFeature)
        tree(ScaffoldCeilingFeature)
        tree(ScaffoldHeadHitterFeature)
        tree(ScaffoldExpandFeature)
    }

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
         * Places blocks at the same Y level only while the physical jump key is not held
         */
        JUMP_KEY("JumpKey", { blockPos ->
            (if (mc.options.keyJump.isDown) OFF else ON).getTargetedBlockPos(blockPos)
        }),

        /**
         * Places blocks at the same Y level as the player, but only if the player is not falling
         */
        FALLING("Falling", { blockPos ->
            (if (player.deltaMovement.y < 0.2) ON else OFF).getTargetedBlockPos(blockPos)
        }),

        /**
         * Similar to FALLING, but only when a certain velocity is triggered and after
         * 2 jumps
         */
        @Deprecated("Will be removed with the release of 0.43.0 and ported to LiquidBounce Legacy Add-on")
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

    /** Whether the next block comes from [ScaffoldExpandFeature]; a tower always builds right under the player. */
    private val isExpanding get() = ScaffoldExpandFeature.running && !isTowering

    /**
     * [ModuleClutch] has scaffold build under a fall without enabling it.
     */
    override val running: Boolean
        get() = super.running || ModuleClutch.fall != null

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

    /** The block scaffold wants to place this tick; null again once it is placed. */
    internal var currentTarget: BlockPlacementTarget? = null
        private set

    /** Whether [currentTarget] waits for the rotation to hit it rather than going down with the view as it is. */
    private var isAimed = false

    /** Where scaffold aimed last, kept over the ticks it has nothing to aim at. */
    internal var aim: Rotation? = null
        private set

    /** Ticks until the next block may go down, counted down at the start of every tick, see [delay]. */
    private var cooldown = 0

    /** The rotation asked for this tick under [NORMAL] timing; null on ticks scaffold asks for none. */
    private var planned: Rotation? = null
    private var placing: Job? = null

    private val swingMode by enumChoice("Swing", SwingMode.DO_NOT_HIDE, SwingMode.INTERACT_CHOICES)

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

    private val renderer = tree(PlacementRenderer("Render", true, this, keep = false))

    private var placementY = 0
    private var startY = 0
    private var jumps = 0

    /** The block SafeWalk was told goes down in time, which the next tick then places rather than choosing again. */
    private var promised: BlockPlacementTarget? = null

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
            return !world.getBlockCollisions(
                player,
                player.boundingBox.inflate(0.5, 0.0, 0.5).move(0.0, -1.05, 0.0)
            ).allEmpty()
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

    override fun onEnabled() {
        // Placement Y is the Y coordinate of the block below the player
        placementY = player.blockPosition().y - 1
        startY = player.blockPosition().y
        jumps = 2

        ScaffoldMovementPlanner.reset()

        super.onEnabled()
    }

    override fun onDisabled() {
        reset()
    }

    internal fun reset() {
        NoFallBlink.waitUntilGround = false
        ScaffoldMovementPlanner.reset()
        ScaffoldMovementPrediction.reset()
        ScaffoldPlacement.reset()
        SilentHotbar.resetSlot(this)
        nextBlock = null
        updateRenderCount(null)
        currentTarget = null
        promised = null
        isAimed = false
        aim = null
        planned = null
        cooldown = 0
        hasStarted = false
        rawInput = DirectionalInput.NONE
        renderer.clearSilently()
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        reset()
    }

    private fun updateRenderCount(count: Int?) {
        EventManager.callEvent(BlockCountChangeEvent(nextBlock, count))
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        NoFallBlink.waitUntilGround = true
        if (cooldown > 0) {
            cooldown--
        }

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

        val prediction = ScaffoldBridgingTelly.placementPos
            ?: ScaffoldMovementPrediction.getPredictedPlacementPos(optimalLine)
        val predictedPos = prediction ?: player.position()
        // Check if the player is probably going to sneak at the predicted position
        val predictedPose = when {
            ScaffoldEagleFeature.running && ScaffoldEagleFeature.shouldEagle(DirectionalInput(player.input)) ->
                Pose.CROUCHING
            // Without a prediction the click comes from where the player is, sneaking at the ledge or not
            prediction == null -> player.pose
            else -> Pose.STANDING
        }

        debugGeometry("predictedPos") {
            DebuggedPoint(predictedPos, Color4b.GREEN, size = 0.1)
        }

        val view = RotationManager.currentRotation ?: player.rotation
        val target = findPlacementTarget(predictedPos, predictedPose, optimalLine, bestStack)
            // On a diagonal the way can ask for the other neighbour by now, and the player already left for this one
            .let { found -> promised?.takeIf { hitFor(it, view) != null } ?: found }
            .also { this.currentTarget = it }
        promised = null

        debugGeometry("lineToBlock") {
            // Debug stuff
            val b = target?.placedBlock?.topCenter ?: return@debugGeometry null
            val a = optimalLine?.getNearestPointTo(b)  ?: return@debugGeometry null

            // Debug the line a-b
            DebuggedLineSegment(
                from = a,
                to = b,
                Color4b.RED,
            )
        }

        isAimed = false
        planned = null
        if (rotationTiming == NORMAL) {
            val rotation = aimAt(target) ?: return@handler
            isAimed = target != null
            aim = rotation
            planned = rotation
            placing = launchSequence(placing) { placeOnHit() }
        }
    }

    /**
     * Asks for the planned rotation every tick and places the current target on the tick the rotation going out
     * clicks it.
     */
    private suspend fun placeOnHit() {
        val rotation by requires { planned }
        val (target, hit, going) = lookAt(Priority.IMPORTANT_FOR_PLAYER_LIFE, {
            ScaffoldRotationValueGroup.toRotationTarget(
                rotation,
                considerInventory = considerInventory,
                surface = currentTarget?.let(::surface),
                tolerance = if (ScaffoldPlacement.isHoldingLook) HELD_LOOK_TOLERANCE else bridging.activeMode.tolerance,
                // Telly's turns round are the rehearsed ones of a telly bridger
                isRehearsed = ScaffoldBridgingTelly.isTellying,
            )
        }) { going ->
            val target = currentTarget
            if (target == null || cooldown > 0) {
                null
            } else {
                hitFor(target, going)?.let { hit -> Triple(target, hit, going.normalize()) }
            }
        }

        place(target, hit, going)
    }

    private fun hitFor(target: BlockPlacementTarget?, rotation: Rotation) =
        crosshairTarget(target, rotation)?.takeIf(::isValidCrosshairTarget)

    /** The block to place next, see [aimAt] and [crosshairTarget]. */
    private fun findPlacementTarget(
        predictedPos: Vec3,
        predictedPose: Pose,
        optimalLine: Line?,
        bestStack: ItemStack,
    ): BlockPlacementTarget? = when {
        ModuleClutch.fall != null -> ScaffoldClutchFeature.findPlacementTarget(bestStack)
        isExpanding -> ScaffoldExpandFeature.findPlacementTarget(predictedPos, predictedPose, bestStack)
        else -> ScaffoldPlacement.findTarget(predictedPos, predictedPose, optimalLine, bestStack)
    }

    /** Where scaffold looks to place [target]. */
    private fun aimAt(target: BlockPlacementTarget?): Rotation? = when {
        ModuleClutch.fall != null -> target?.rotation
        isExpanding -> ScaffoldExpandFeature.rotation(target)
        else -> ScaffoldPlacement.rotation(target)
    }

    /** What a look has to meet to place [target], see [crosshairTarget]; nothing for the exact held look. */
    private fun surface(target: BlockPlacementTarget): Surface? = when {
        ModuleClutch.fall != null -> ScaffoldClutchFeature.surface(target) ?: ScaffoldPlacement.surface(target)
        isExpanding -> ScaffoldExpandFeature.surface(target)
        ScaffoldPlacement.isHoldingLook || ScaffoldBridgingTelly.isFacingForward -> null
        else -> ScaffoldPlacement.surface(target)
    }

    /** The hit a click with [rotation] places [target] with, if it does. */
    private fun crosshairTarget(target: BlockPlacementTarget?, rotation: Rotation): BlockHitResult? = when {
        ModuleClutch.fall != null -> ScaffoldClutchFeature.hitFor(target, rotation)
        isExpanding -> ScaffoldExpandFeature.hitFor(target, rotation)
        else -> ScaffoldPlacement.hitFor(target, rotation)
    }

    /**
     * Clicks [hit] to place [target]. [beforeClick] runs once there is a block to place.
     *
     * @return whether it clicked
     */
    private fun place(
        target: BlockPlacementTarget,
        hit: BlockHitResult,
        rotation: Rotation,
        beforeClick: () -> Unit = {},
    ): Boolean {
        val hand = handToPlaceWith() ?: return false
        beforeClick()

        // Taken before the block changes it
        val previousFallOffPos = currentOptimalLine?.let(ScaffoldMovementPrediction::getFallOffPositionOnLine)

        doPlacement(hit, rotation, hand, {
            onPlaced(target.placedBlock)
            currentTarget = null
            ScaffoldMovementPrediction.onPlace(currentOptimalLine, previousFallOffPos)
            cooldown = delay.random() + 1
            true
        }, swingMode = swingMode)
        return true
    }

    private fun handToPlaceWith(): InteractionHand? {
        var hasBlockInMainHand = isValidBlock(player.inventory.getItem(player.inventory.selectedSlot))
        val hasBlockInOffHand = isValidBlock(player.offhandItem)

        if (!ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            hasBlockInMainHand = handleSilentBlockSelection(hasBlockInMainHand, hasBlockInOffHand)
        }

        return when {
            hasBlockInMainHand -> InteractionHand.MAIN_HAND
            hasBlockInOffHand -> InteractionHand.OFF_HAND
            else -> null
        }
    }

    private fun onPlaced(placed: BlockPos) {
        hasStarted = true
        ScaffoldMovementPlanner.trackPlacedBlock(placed)
        renderer.addBlock(placed)
        ScaffoldEagleFeature.onBlockPlacement()
        ScaffoldBlinkFeature.onBlockPlacement()
        ScaffoldSprintControlFeature.onBlockPlacement()
    }

    /** Whether a block went down since Scaffold was enabled; until then the aim may still be turning onto the face. */
    internal var hasStarted = false
        private set

    /** Whether the aim turns around within a tick. */
    internal val hasInstantAim
        get() = ScaffoldRotationValueGroup.calculateTicks(Rotation(180f, 0f), from = Rotation.ZERO) == 0

    /**
     * Whether a block can go down while the player is in the air, where nothing stops a fall: the bridge has started
     * and the aim is instant, or Telly turns around in the first ticks of its jumps. A slower aim, or one that has to
     * find the face anywhere, has the block down too late.
     */
    internal val canPlaceInAir
        get() = hasStarted && (hasInstantAim || ScaffoldBridgingTelly.running && ScaffoldBridgingTelly.canTurnAround)

    /** Whether the player is in the air above the row it stood on, with an aim too slow to place before it lands. */
    internal val isHopping
        get() = !player.onGround() && !canPlaceInAir && player.y >= placementY + 1

    /**
     * Whether a jump at an edge, moving with [input], gets a block to come down on: from an aim that follows in the
     * air, or from a held look that meets the face on the first tick of the jump. The next block then goes on top of
     * that one, under the player, unless SameY keeps the row.
     */
    internal fun canJumpAtEdge(input: DirectionalInput): Boolean =
        canPlaceInAir || hasStarted && sameYMode == SameYMode.OFF && bridging.activeMode.shouldHoldLook &&
            currentTarget?.let {
                ScaffoldPlacement.canPlaceInJump(it, RotationManager.currentRotation ?: player.rotation, input)
            } == true

    /**
     * A jump off the end of the bridge is left out while no block can go down in the air. Decided before
     * [ScaffoldStabilizeMovementFeature] steers, which leaves a jumping player alone. A jump against a block is one
     * up a step, such as a bridge leaves where it jumped at an edge.
     */
    @Suppress("unused")
    private val jumpHandler = handler<MovementInputEvent>(
        priority = (EventPriorityConvention.MODEL_STATE + 1).toShort()
    ) { event ->
        val jumps = event.jump && player.onGround() && !isTowering && ModuleClutch.fall == null &&
            !player.horizontalCollision
        if (jumps && !canPlaceInAir && landsOffBridge(event.directionalInput)) {
            event.jump = false
        }
    }

    /**
     * A held look puts the blocks of a jump under the player, not ahead of it. In the air the keys are let go until
     * the jump comes down on the bridge. Not after a hit: where that throws the player is the player's to steer.
     */
    @Suppress("unused")
    private val hopHandler = handler<MovementInputEvent>(
        priority = EventPriorityConvention.OBJECTION_AGAINST_EVERYTHING
    ) { event ->
        val hops = isHopping && bridging.activeMode.shouldHoldLook && ModuleClutch.fall == null &&
            player.hurtTime == 0
        if (hops && event.directionalInput != DirectionalInput.NONE && landsOffBridge(event.directionalInput)) {
            event.directionalInput = DirectionalInput.NONE
        }
    }

    /**
     * Whether a jump now, or the one under way, comes down where no block is, or so close to an edge that the landing
     * runs over it. A view that turns steers a silent walk up to half a key step off its way, so then a jump to either
     * side counts too.
     */
    internal fun landsOffBridge(input: DirectionalInput): Boolean {
        // Without a silent view, or with one resting on the held look, the walk goes where the keys say
        val isSteady = RotationManager.currentRotation == null ||
            !isExpanding && ScaffoldPlacement.heldLook != null && !isLiningUp()
        val ways = if (isSteady) floatArrayOf(0f) else floatArrayOf(-30f, 0f, 30f)
        return ways.any { offset ->
            // Built by hand: asking for SafeWalk here would ask scaffold again. No sneak, which would cut it short
            val simulation = SimulatedPlayer.fromClientPlayer(
                SimulatedInput(input.toInput(jump = true, sprint = player.isSprinting).toAgent())
            )
            simulation.yRot += offset
            // A jump that stays on the bridge, or reaches other ground, has landed within a second
            val lands = (1..20).any {
                simulation.tick()
                simulation.onGround()
            }
            if (lands) {
                // The landing carries the player on for a few ticks
                simulation.input = SimulatedInput(input.toInput(sprint = player.isSprinting).toAgent())
                repeat(3) { simulation.tick() }
            }
            !lands || player.wouldBeCloseToFallOff(simulation.position())
        }
    }

    var currentOptimalLine: Line? = null
        private set

    /** The movement the player asks for, before [ScaffoldStabilizeMovementFeature] steers it towards the line. */
    var rawInput = DirectionalInput.NONE
        private set

    /**
     * Before [ScaffoldStabilizeMovementFeature] steers: the line leads where the keys ask to go, not where the
     * steering of the last tick went.
     */
    @Suppress("unused")
    private val rawInputHandler = handler<MovementInputEvent>(
        priority = (EventPriorityConvention.MODEL_STATE + 1).toShort()
    ) { event ->
        rawInput = event.directionalInput
        currentOptimalLine = if (rawInput == DirectionalInput.NONE) {
            null
        } else {
            ScaffoldMovementPlanner.getOptimalMovementLine(rawInput)?.let(bridging.activeMode::wantedLine)
        }
    }

    /**
     * Whether an edge has to stop the player: no blocks left, or the next block does not go down before the player,
     * moving with [input], walks off.
     */
    internal fun shouldStopAtEdge(input: DirectionalInput): Boolean {
        val target = currentTarget
        // A click waits out the delay, however well the view meets the face
        if (blockCount <= 0 || target == null || cooldown > 1) {
            return true
        }
        if (rotationTiming != NORMAL) {
            // These place with the aim itself, on the tick it clicks
            return aimAt(target)?.let { hitFor(target, it) } == null
        }
        val view = RotationManager.currentRotation ?: player.rotation
        // A view still turning onto the held look may pass over the face for a tick and be gone by the click
        if (isLiningUp()) {
            return true
        }
        if (hitFor(target, view) != null) {
            return false
        }
        if (ModuleClutch.fall != null || isExpanding) {
            return true
        }
        promised = target.takeIf { ScaffoldPlacement.canPlaceInTime(it, view, input) }
        return promised == null
    }

    /**
     * Whether the view still turns to the look the bridging holds (bridge start, a corner, stop and go), during which
     * an edge is only sneaked at, see [ScaffoldBridging.look]. A hand that came within the tolerance may still be
     * settling: until it rests, a click the walk counts on can miss the face.
     */
    internal fun isLiningUp(): Boolean {
        val look = ScaffoldPlacement.heldLook?.takeUnless { isExpanding } ?: return false
        val view = RotationManager.currentRotation ?: player.rotation
        val margin = max(HELD_LOOK_TOLERANCE, RotationUtil.gcd.toFloat())
        val moves = RotationManager.lastRotation?.let { view.rotationDeltaLengthTo(it) >= RotationUtil.gcd / 2 } == true
        return abs(angleDifference(view.yaw, look.yaw)) > margin || abs(view.pitch - look.pitch) > margin || moves
    }

    /**
     * Degrees a hand may rest off the held look. Ending a turn exactly on a 45 degree angle is what Intave's placement
     * analysis reads as a snap, and the look still places from this far off.
     */
    private const val HELD_LOOK_TOLERANCE = 1.5f

    @Suppress("unused")
    private val timerHandler = handler<GameTickEvent> {
        if (timer != 1f) {
            Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, this@ModuleScaffold)
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
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
        val onTick = rotationTiming == ON_TICK || rotationTiming == ON_TICK_SNAP

        val currentRotation = (target?.takeIf { onTick }?.let(::aimAt)
            ?: RotationManager.currentRotation ?: player.rotation).normalize()

        if (ScaffoldAutoBlockFeature.alwaysHoldBlock) {
            handleSilentBlockSelection(
                isValidBlock(player.inventory.getItem(player.inventory.selectedSlot)),
                isValidBlock(player.offhandItem),
            )
        }

        if (SimulatePlacementAttempts.enabled) {
            // The clicker decides on the hit scaffold would place with, falling back to the raw crosshair hit when
            // there is none: it also fires without a placement target, and on hits scaffold rejected.
            simulatePlacementAttempt(
                crosshairTarget(target, currentRotation) ?: traceFromPlayer(currentRotation),
                currentRotation,
            )
        }

        // An aimed target goes down once the rotation hits it, see placeOnHit
        if (target == null || isAimed || cooldown > 0) {
            return@handler
        }

        val hit = hitFor(target, currentRotation) ?: return@handler

        val clicked = place(target, hit, currentRotation) {
            if (!onTick) {
                return@place
            }

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

        if (clicked && rotationTiming == ON_TICK && RotationManager.serverRotation != player.rotation) {
            network.send(
                PosRot(
                    player.x, player.y, player.z, player.withFixedYaw(currentRotation), player.xRot, player.onGround(),
                    player.horizontalCollision
                )
            )
        }
    }

    private fun simulatePlacementAttempt(clickerTarget: BlockHitResult, rotation: Rotation) {
        // Prioritize by all means the main hand if it has a block
        val suitableHand = InteractionHand.entries.firstOrNull {
            isValidBlock(player.getItemInHand(it))
        }

        if (simulatePlacementAttempts(clickerTarget, suitableHand) && player.moving
            && SimulatePlacementAttempts.clicker.isClickTick
        ) {
            SimulatePlacementAttempts.clicker.click {
                doPlacement(clickerTarget, rotation, suitableHand!!, {
                    onPlaced(clickerTarget.targetBlockPos)
                    true
                }, swingMode = swingMode)
                true
            }
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
        // The keys the player holds: Telly lets go of W in the air, which is no standing jump
        player.input.keyPresses.jump && (!DirectionalInput(mc.options).isMoving || player.horizontalCollision) ->
            blockPos.offset(0, -1, 0)
        // Telly places the row it jumped off, not one under the jump, unless SameY has it build up
        ScaffoldBridgingTelly.running ->
            (if (sameYMode == SameYMode.OFF) SameYMode.ON else sameYMode).getTargetedBlockPos(blockPos)
                ?: blockPos.offset(0, -1, 0)
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

            sameYMode != SameYMode.OFF &&
                (sameYMode != SameYMode.JUMP_KEY || mc.options.keyJump.isDown) -> {
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
