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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsConfigurable
import net.ccbluex.liquidbounce.utils.aiming.utils.raycast
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.targetfinding.*
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.item.Items
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Vec3i

/**
 * Module Extinguish
 *
 * Automatically extinguishes yourself when you're burning.
 */
object ModuleExtinguish: ClientModule("Extinguish", Category.WORLD) {

    private val cooldown by float("Cooldown", 1.0F, 0.0F..20.0F, "s")
    private val notDuringCombat by boolean("NotDuringCombat", true)

    private object Pickup : ToggleableConfigurable(ModuleExtinguish, "Pickup", true) {
        val pickupSpan by floatRange("PickupSpan", 0.1F..10.0F, 0.0F..20.0F, "s")
    }

    init {
        tree(Pickup)
    }

    private var currentTarget: PlacementPlan? = null

    private val rotationsConfigurable = tree(RotationsConfigurable(this))

    private val cooldownTimer = Chronometer()

    private var lastExtinguishPos: BlockPos? = null
    private val lastAttemptTimer = Chronometer()

    override fun onEnabled() {
        currentTarget = null
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // we can't place water in the nether
        if (world.dimension.ultrawarm) {
            return@handler
        }

        this.currentTarget = null

        val target = findAction() ?: return@handler

        this.currentTarget = target

        RotationManager.setRotationTarget(
            target.placementTarget.rotation,
            configurable = rotationsConfigurable,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleNoFall
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        currentTarget = null
    }

    private fun findAction(): PlacementPlan? {
        val pickupSpanStart = (Pickup.pickupSpan.start * 1000.0F).toLong()
        val pickupSpanEnd = (Pickup.pickupSpan.endInclusive * 1000.0F).toLong()

        if (lastExtinguishPos != null && lastAttemptTimer.hasElapsed(pickupSpanEnd)) {
            lastExtinguishPos = null
        }

        if (player.hasStatusEffect(StatusEffects.FIRE_RESISTANCE) || (notDuringCombat && CombatManager.isInCombat)) {
            return null
        }

        val pickupPos = this.lastExtinguishPos

        if (pickupPos != null && Pickup.enabled && this.lastAttemptTimer.hasElapsed(pickupSpanStart)) {
            planPickup(pickupPos)?.let {
                return it
            }
        }

        if (!player.isOnFire || !cooldownTimer.hasElapsed()) {
            return null
        }

        return planExtinguishing()
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val target = currentTarget ?: return@tickHandler

        val rayTraceResult = raycast()

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@tickHandler
        }

        SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)

        val successFunction = {
            cooldownTimer.waitForAtLeast((cooldown * 1000.0F).toLong())
            lastAttemptTimer.reset()

            lastExtinguishPos = target.placementTarget.placedBlock

            true
        }

        doPlacement(rayTraceResult, hand = target.hotbarItemSlot.useHand,
            onItemUseSuccess = successFunction, onPlacementSuccess = successFunction)
    }

    private fun planExtinguishing(): PlacementPlan? {
        val waterBucketSlot = Slots.OffhandWithHotbar.findClosestSlot(Items.WATER_BUCKET) ?: return null

        val simulation = PlayerSimulationCache.getSimulationForLocalPlayer()

        val frameOnGround = simulation.simulateBetween(0..20).firstOrNull {
            it.onGround
        } ?: return null

        val playerPos = frameOnGround.pos.toBlockPos()

        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                listOf(Vec3i.ZERO),
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = waterBucketSlot.itemStack,
            PlayerLocationOnPlacement(position = frameOnGround.pos),
        )

        val bestPlacementPlan = findBestBlockPlacementTarget(playerPos, options) ?: return null

        return PlacementPlan(playerPos, bestPlacementPlan, waterBucketSlot)
    }

    private fun planPickup(blockPos: BlockPos): PlacementPlan? {
        val bucket = Slots.OffhandWithHotbar.findClosestSlot(Items.BUCKET) ?: return null

        val options = BlockPlacementTargetFindingOptions(
            BlockOffsetOptions(
                listOf(Vec3i.ZERO),
                BlockPlacementTargetFindingOptions.PRIORITIZE_LEAST_BLOCK_DISTANCE,
            ),
            FaceHandlingOptions(CenterTargetPositionFactory),
            stackToPlaceWith = bucket.itemStack,
            PlayerLocationOnPlacement(position = player.pos),
        )

        val bestPlacementPlan = findBestBlockPlacementTarget(blockPos, options) ?: return null

        return PlacementPlan(blockPos, bestPlacementPlan, bucket)
    }

}
