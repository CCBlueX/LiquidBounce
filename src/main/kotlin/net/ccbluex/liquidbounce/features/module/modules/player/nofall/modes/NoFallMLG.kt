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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.fallDamageMultiplier
import net.ccbluex.liquidbounce.utils.block.isInteractable
import net.ccbluex.liquidbounce.utils.block.state
import net.ccbluex.liquidbounce.utils.block.liquid.canPlaceStandaloneFluid
import net.ccbluex.liquidbounce.utils.block.liquid.requiresSneakForAdjacentFluidPlacement
import net.ccbluex.liquidbounce.utils.block.liquid.TimedPickupTracker
import net.ccbluex.liquidbounce.utils.block.liquid.planPlacementAtPos
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.client.isOlderThan1_21_2
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.rotation
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FINAL_DECISION
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.world.waterEvaporates
import net.minecraft.core.BlockPos
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.ScaffoldingBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.material.Fluids

internal object NoFallMLG : NoFallMode("MLG") {
    private const val PICKUP_TRACKER_CAPACITY = 8
    private const val SCAFFOLDING_ATTEMPT_TIMEOUT_TICKS = 20

    private val minFallDist by float("MinFallDistance", 5f, 2f..50f)

    private object PickupWater : ToggleableValueGroup(NoFallMLG, "PickUpWater", true) {
        /**
         * Don't pick up before the lower bound, don't pick up after the upper bound
         */
        val pickupSpan by intRange("PickupSpan", 200..1000, 0..10000, "ms")
    }

    private val rotations = tree(RotationsValueGroup(this))

    private var currentTarget: PlacementAction? = null
    private val pickupTracker = TimedPickupTracker(PICKUP_TRACKER_CAPACITY)
    private var scaffoldingTarget: BlockPos? = null
    private var scaffoldingPlacedAtTick = 0
    private var forceSneak = false

    private val netherItems =
        setOf(
            // overworld
            Items.SCAFFOLDING,
            Items.COBWEB,
            Items.POWDER_SNOW_BUCKET,
            Items.HAY_BLOCK,
            Items.SLIME_BLOCK,
            Items.HONEY_BLOCK,
            // nether
            Items.TWISTING_VINES,
        )
    private val normalItems = netherItems + Items.WATER_BUCKET

    private val itemsForMLG
        get() = if (world.waterEvaporates) netherItems else normalItems

    init {
        tree(PickupWater)
    }

    override val running: Boolean
        get() = super.running && !ModuleFreeze.running

    override fun disable() {
        SilentHotbar.resetSlot(this)
        resetState()
    }

    private fun resetState() {
        currentTarget = null
        pickupTracker.clear()
        scaffoldingTarget = null
        forceSneak = false
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        resetState()
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent>(priority = FINAL_DECISION) { event ->
        if (forceSneak) {
            event.sneak = true
        }
    }

    @Suppress("unused")
    private val tickMovementHandler =
        handler<RotationUpdateEvent> {
            forceSneak = false

            if (maintainScaffoldingAttempt()) {
                currentTarget = null
                return@handler
            }

            val currentGoal = this.getCurrentGoal()

            forceSneak = currentGoal?.requiresSneak == true
            currentTarget = currentGoal?.takeUnless { it.requiresSneak && !player.isShiftKeyDown }

            if (currentGoal == null) {
                return@handler
            }

            RotationManager.setRotationTarget(
                currentGoal.plan.placementTarget.rotation,
                valueGroup = rotations,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                provider = ModuleNoFall,
            )
        }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val action = currentTarget ?: return@handler
        val target = action.plan

        val rotation = RotationManager.currentRotation ?: player.rotation
        val rayTraceResult = traceFromPlayer(rotation)

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@handler
        }

        if (target.hotbarItemSlot.itemStack.item != action.item ||
            !SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)
        ) {
            currentTarget = null
            return@handler
        }

        val targetStateBefore = target.targetPos.state

        val onSuccess: () -> Boolean = {
            if (!action.wasApplied(targetStateBefore)) {
                false
            } else {
                if (action.type == MlgPlacementActionType.MLG && action.item === Items.WATER_BUCKET) {
                    pickupTracker.record(target.targetPos)
                }

                if (action.type == MlgPlacementActionType.SCAFFOLDING) {
                    scaffoldingTarget = target.targetPos
                    scaffoldingPlacedAtTick = player.tickCount
                    forceSneak = true
                }

                true
            }
        }

        doPlacement(
            rayTraceResult,
            rotation,
            hand = target.hotbarItemSlot.useHand,
            onItemUseSuccess = onSuccess,
            onPlacementSuccess = onSuccess,
        )

        currentTarget = null
    }

    /**
     * Finds something to do, either
     * 1. Preventing fall damage by placing something
     * 2. Picking up water which we placed earlier to prevent fall damage
     */
    private fun getCurrentGoal(): PlacementAction? {
        getCurrentMLGPlacementPlan()?.let {
            return it
        }

        if (PickupWater.enabled) {
            return getCurrentPickupTarget()
        }

        return null
    }

    /**
     * Finds a position to pickup placed water from
     */
    private fun getCurrentPickupTarget(): PlacementAction? {
        if (!canPickUpWaterSafely()) {
            return null
        }

        val bestPickupItem = Slots.OffhandWithHotbar.findClosestSlot(Items.BUCKET) ?: return null

        // Remove all time outed/invalid pickup targets from the list
        pickupTracker.prune(PickupWater.pickupSpan.last.toLong(), TimedPickupTracker.PickupFilter.WATER)

        val pickupPos = pickupTracker.firstEligible(PickupWater.pickupSpan.first.toLong()) ?: return null
        val plan = planPlacementAtPos(pickupPos, bestPickupItem) ?: return null
        return PlacementAction(
            plan,
            MlgPlacementActionType.PICKUP_WATER,
            Items.BUCKET,
            requiresSneak = plan.interactedBlockIsInteractable,
        )
    }

    private fun canPickUpWaterSafely(): Boolean {
        return player.isInWater || player.onGround() || player.fallDistance <= minFallDist
    }

    /**
     * Find a way to prevent fall damage if we are falling.
     */
    @Suppress("CognitiveComplexMethod", "LoopWithTooManyJumpStatements")
    private fun getCurrentMLGPlacementPlan(): PlacementAction? {
        if (player.fallDistance <= minFallDist) {
            return null
        }

        val collision = FallingPlayer.fromPlayer(player, RotationManager.movementYaw).findCollision(20) ?: return null
        val collisionPos = collision.pos ?: return null

        if (collisionPos.fallDamageMultiplier(player) <= 0f) {
            return null
        }

        val targetPos = collisionPos.above()
        val candidates = Slots.OffhandWithHotbar
            .filter { it.itemStack.item in itemsForMLG }
            .sortedWith(HotbarItemSlot.PREFER_NEARBY)

        for (slot in candidates) {
            val plan = planPlacementAtPos(targetPos, slot) ?: continue
            val item = slot.itemStack.item

            if (item === Items.WATER_BUCKET && !plan.canPlaceExposedWaterAtTarget()) {
                continue
            }

            val requiresSneak = item === Items.SCAFFOLDING || plan.interactedBlockIsInteractable ||
                item === Items.WATER_BUCKET && plan.placementTarget.interactedBlockPos.state
                    ?.requiresSneakForAdjacentFluidPlacement(Fluids.WATER) == true

            if (requiresSneak && !player.isShiftKeyDown) {
                continue
            }

            if (!plan.canPlaceBlockItemAtTarget()) {
                continue
            }

            if (item === Items.SCAFFOLDING && !canUseScaffoldingAt(targetPos)) {
                continue
            }

            // On pre-1.21.2 servers, sneaking on slime still causes fall damage.
            if (item === Items.SLIME_BLOCK && isOlderThan1_21_2 &&
                (requiresSneak || player.isShiftKeyDown)
            ) {
                continue
            }

            val type = if (item === Items.SCAFFOLDING) {
                MlgPlacementActionType.SCAFFOLDING
            } else {
                MlgPlacementActionType.MLG
            }
            return PlacementAction(plan, type, item, requiresSneak)
        }

        return null
    }

    private fun canUseScaffoldingAt(targetPos: BlockPos): Boolean {
        if (targetPos.state?.block === Blocks.SCAFFOLDING || ScaffoldingBlock.getDistance(world, targetPos) >= 7) {
            return false
        }

        return FallingPlayer.fromPlayer(player, RotationManager.movementYaw).willStartTickInBlockBeforeCollision(
            targetPos,
            ticks = 20,
            forceDescending = true,
        )
    }

    private fun maintainScaffoldingAttempt(): Boolean {
        val targetPos = scaffoldingTarget ?: return false
        val hasTimedOut = player.tickCount - scaffoldingPlacedAtTick >= SCAFFOLDING_ATTEMPT_TIMEOUT_TICKS
        val hasResolved = player.onGround() || player.fallDistance <= playerSafeFallDistance
        val hasPassedTarget = player.y < targetPos.y
        val blockWasRemoved = targetPos.state?.block != Blocks.SCAFFOLDING

        if (hasTimedOut || hasResolved || hasPassedTarget || blockWasRemoved) {
            scaffoldingTarget = null
            return false
        }

        forceSneak = true
        return true
    }

    private val PlacementPlan.interactedBlockIsInteractable: Boolean
        get() {
            val blockState = placementTarget.interactedBlockPos.state ?: return false
            return blockState.block.isInteractable(blockState)
        }

    private fun PlacementPlan.canPlaceBlockItemAtTarget(): Boolean {
        val stack = hotbarItemSlot.itemStack
        val blockItem = stack.item as? BlockItem ?: return true
        val context = BlockPlaceContext(
            world,
            player,
            hotbarItemSlot.useHand,
            stack,
            placementTarget.blockHitResult,
        )
        if (!blockItem.block.isEnabled(world.enabledFeatures()) || !context.canPlace()) {
            return false
        }

        val updatedContext = blockItem.updatePlacementContext(context) ?: return false
        return updatedContext.clickedPos == targetPos && blockItem.getPlacementState(updatedContext) != null
    }

    private fun PlacementPlan.canPlaceExposedWaterAtTarget(): Boolean {
        val bucketTarget = placementTarget.interactedBlockPos.relative(placementTarget.direction)
        return bucketTarget == targetPos && targetPos.state?.canPlaceStandaloneFluid(Fluids.WATER) == true
    }

    private fun PlacementAction.wasApplied(targetStateBefore: BlockState?): Boolean {
        return wasMlgPlacementApplied(type, item, targetStateBefore, plan.targetPos.state)
    }

    private data class PlacementAction(
        val plan: PlacementPlan,
        val type: MlgPlacementActionType,
        val item: Item,
        val requiresSneak: Boolean,
    )
}

internal enum class MlgPlacementActionType {
    MLG,
    SCAFFOLDING,
    PICKUP_WATER,
}

internal fun wasMlgPlacementApplied(
    type: MlgPlacementActionType,
    item: Item,
    before: BlockState?,
    after: BlockState?,
): Boolean {
    before ?: return false
    after ?: return false

    return when (type) {
        MlgPlacementActionType.PICKUP_WATER -> {
            before.fluidState.isSourceOfType(Fluids.WATER) &&
                !after.fluidState.isSourceOfType(Fluids.WATER)
        }
        MlgPlacementActionType.SCAFFOLDING -> before.block !== Blocks.SCAFFOLDING && after.block === Blocks.SCAFFOLDING
        MlgPlacementActionType.MLG -> when (item) {
            Items.WATER_BUCKET -> {
                !before.fluidState.isSourceOfType(Fluids.WATER) &&
                    after.block === Blocks.WATER && after.fluidState.isSourceOfType(Fluids.WATER)
            }
            Items.POWDER_SNOW_BUCKET -> before.block !== Blocks.POWDER_SNOW && after.block === Blocks.POWDER_SNOW
            is BlockItem -> before.block !== item.block && after.block === item.block
            else -> false
        }
    }
}
