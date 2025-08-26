/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isFallDamageBlocking
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.block.Blocks
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

internal object NoFallMLG : Choice("MLG") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private val minFallDist by float("MinFallDistance", 5f, 2f..50f)

    private object PickupWater : ToggleableConfigurable(NoFallMLG, "PickUpWater", true) {
        /**
         * Don't pick up before the lower bound, don't pick up after the upper bound
         */
        val pickupSpan by intRange("PickupSpan", 200..1000, 0..10000, "ms")
    }

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private var currentTarget: PlacementPlan? = null
    private var lastPlacements = mutableListOf<Pair<BlockPos, Chronometer>>()

    private val netherItems =
        arrayOf(
            // overworld
            Items.COBWEB,
            Items.POWDER_SNOW_BUCKET,
            Items.HAY_BLOCK,
            Items.SLIME_BLOCK,
            Items.HONEY_BLOCK,
            // nether
            Items.TWISTING_VINES,
        )
    private val normalItems = arrayOf(Items.WATER_BUCKET) + netherItems

    private val itemsForMLG
        get() = if (world.dimension.ultrawarm) netherItems else normalItems

    init {
        tree(PickupWater)
    }

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
                configurable = rotationsConfigurable,
                priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
                provider = ModuleNoFall,
            )
        }

    @Suppress("unused")
    private val tickHandler =
        tickHandler {
            val target = currentTarget ?: return@tickHandler

            val rayTraceResult = raycast()

            if (!target.doesCorrespondTo(rayTraceResult)) {
                return@tickHandler
            }

            SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)

            val onSuccess: () -> Boolean = {
                lastPlacements.add(target.targetPos to Chronometer().also { it.reset() })

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
        val bestPickupItem = Slots.Hotbar.findClosestSlot(Items.BUCKET) ?: return null

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

        return findPlacementPlanAtPos(collision.up(), itemForMLG)
    }

    private fun findPlacementPlanAtPos(
        pos: BlockPos,
        item: HotbarItemSlot,
    ): PlacementPlan? {
        val options =
            BlockPlacementTargetFindingOptions(
                BlockOffsetOptions(
                    listOf(Vec3i.ZERO),
                    BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
                ),
                FaceHandlingOptions(CenterTargetPositionFactory),
                stackToPlaceWith = item.itemStack,
                PlayerLocationOnPlacement(position = player.pos),
            )

        val bestPlacementPlan = findBestBlockPlacementTarget(pos, options) ?: return null

        return PlacementPlan(pos, bestPlacementPlan, item)
    }
}
