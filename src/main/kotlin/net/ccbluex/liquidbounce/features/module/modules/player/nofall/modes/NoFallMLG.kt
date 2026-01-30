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
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeated
import net.ccbluex.liquidbounce.features.module.modules.movement.ModuleFreeze
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isFallDamageBlocking
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockOffsetOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.BlockPlacementTargetFindingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.CenterTargetPositionFactory
import net.ccbluex.liquidbounce.utils.block.targetfinding.FaceHandlingOptions
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlayerLocationOnPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.findBestBlockPlacementTarget
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.world.waterEvaporates
import net.minecraft.core.BlockPos
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.Blocks

internal object NoFallMLG : NoFallMode("MLG") {
    private val minFallDist by float("MinFallDistance", 5f, 2f..50f)

    private object PickupWater : ToggleableValueGroup(NoFallMLG, "PickUpWater", true) {
        /**
         * Don't pick up before the lower bound, don't pick up after the upper bound
         */
        val pickupSpan by intRange("PickupSpan", 200..1000, 0..10000, "ms")
    }

    private val rotations = tree(RotationsValueGroup(this))

    private var currentTarget: PlacementPlan? = null
    private val lastPlacements = mutableListOf<Pair<BlockPos, Chronometer>>()

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

    /**
     * We need to sneak for at least 3 ticks to eliminate
     * the fall damage.
     */
    const val SCAFFOLDING_SNEAKING_TICKS = 3

    override val running: Boolean
        get() = super.running && !ModuleFreeze.running

    @Suppress("unused")
    private val tickMovementHandler =
        handler<RotationUpdateEvent> {
            val currentGoal = this.getCurrentGoal()

            this.currentTarget = currentGoal

            if (currentGoal == null) {
                return@handler
            }

            RotationManager.setRotationTarget(
                currentGoal.placementTarget.rotation,
                valueGroup = rotations,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                provider = ModuleNoFall,
            )
        }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        val target = currentTarget ?: return@handler

        val rayTraceResult = traceFromPlayer()

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@handler
        }

        SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)

        val onSuccess: () -> Boolean = {
            lastPlacements.add(target.targetPos to Chronometer(System.currentTimeMillis()))

            if (target.hotbarItemSlot.itemStack.item == Items.SCAFFOLDING) {
                repeated<MovementInputEvent>(SCAFFOLDING_SNEAKING_TICKS) { event ->
                    event.sneak = true
                }
            }

            true
        }

        doPlacement(
            rayTraceResult,
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
    private fun getCurrentGoal(): PlacementPlan? {
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
    private fun getCurrentPickupTarget(): PlacementPlan? {
        val bestPickupItem = Slots.OffhandWithHotbar.findClosestSlot(Items.BUCKET) ?: return null

        // Remove all time outed/invalid pickup targets from the list
        this.lastPlacements.removeIf {
            it.second.hasElapsed(PickupWater.pickupSpan.last.toLong()) || it.first.getState()?.block != Blocks.WATER
        }

        for (lastPlacement in this.lastPlacements) {
            if (!lastPlacement.second.hasElapsed(PickupWater.pickupSpan.first.toLong())) {
                continue
            }

            val possibleTarget = findPlacementPlanAtPos(lastPlacement.first, bestPickupItem)

            if (possibleTarget != null) {
                return possibleTarget
            }
        }

        return null
    }

    /**
     * Find a way to prevent fall damage if we are falling.
     */
    private fun getCurrentMLGPlacementPlan(): PlacementPlan? {
        val itemForMLG = Slots.OffhandWithHotbar.findClosestSlot(items = itemsForMLG)

        if (player.fallDistance <= minFallDist || itemForMLG == null) {
            return null
        }

        val collision = FallingPlayer.fromPlayer(player).findCollision(20)?.pos ?: return null

        if (collision.isFallDamageBlocking()) {
            return null
        }

        return findPlacementPlanAtPos(collision.above(), itemForMLG)
    }

    private fun findPlacementPlanAtPos(
        pos: BlockPos,
        item: HotbarItemSlot,
    ): PlacementPlan? {
        val options =
            BlockPlacementTargetFindingOptions(
                BlockOffsetOptions.Default,
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = item.itemStack,
                PlayerLocationOnPlacement(position = player.position()),
            )

        val bestPlacementPlan = findBestBlockPlacementTarget(pos, options) ?: return null

        return PlacementPlan(pos, bestPlacementPlan, item)
    }
}
