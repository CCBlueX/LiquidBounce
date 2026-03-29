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
package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.RotationUpdateEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.aiming.RotationManager
import net.ccbluex.liquidbounce.utils.aiming.RotationsValueGroup
import net.ccbluex.liquidbounce.utils.block.doPlacement
import net.ccbluex.liquidbounce.utils.block.liquid.TimedPickupTracker
import net.ccbluex.liquidbounce.utils.block.liquid.planPlacementAtPos
import net.ccbluex.liquidbounce.utils.block.targetfinding.PlacementPlan
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.ccbluex.liquidbounce.utils.combat.CombatManager
import net.ccbluex.liquidbounce.utils.entity.PlayerSimulationCache
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findClosestSlot
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.ccbluex.liquidbounce.utils.raytracing.traceFromPlayer
import net.ccbluex.liquidbounce.utils.world.waterEvaporates
import net.minecraft.core.BlockPos
import net.minecraft.world.effect.MobEffects
import net.minecraft.world.item.Items

/**
 * Module Extinguish
 *
 * Automatically extinguishes yourself when you're burning.
 */
object ModuleExtinguish: ClientModule("Extinguish", ModuleCategories.WORLD) {

    private val cooldown by float("Cooldown", 1.0F, 0.0F..20.0F, "s")
    private val notDuringCombat by boolean("NotDuringCombat", true)

    private object Pickup : ToggleableValueGroup(ModuleExtinguish, "Pickup", true) {
        val pickupSpan by floatRange("PickupSpan", 0.1F..10.0F, 0.0F..20.0F, "s")
    }

    init {
        tree(Pickup)
    }

    private var currentTarget: PlacementPlan? = null

    private val rotations = tree(RotationsValueGroup(this))

    private val cooldownTimer = Chronometer()
    private val pickupTracker = TimedPickupTracker(capacity = 1)

    override fun onEnabled() {
        currentTarget = null
        pickupTracker.clear()
    }

    override fun onDisabled() {
        SilentHotbar.resetSlot(this)
    }

    @Suppress("unused")
    private val rotationUpdateHandler = handler<RotationUpdateEvent> {
        // we can't place water in the nether
        if (world.waterEvaporates) {
            return@handler
        }

        this.currentTarget = null

        val target = findAction() ?: return@handler

        this.currentTarget = target

        RotationManager.setRotationTarget(
            target.placementTarget.rotation,
            valueGroup = rotations,
            priority = Priority.IMPORTANT_FOR_PLAYER_LIFE,
            provider = ModuleExtinguish
        )
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> {
        currentTarget = null
    }

    private fun findAction(): PlacementPlan? {
        val pickupSpanStart = (Pickup.pickupSpan.start * 1000.0F).toLong()
        val pickupSpanEnd = (Pickup.pickupSpan.endInclusive * 1000.0F).toLong()

        pickupTracker.prune(pickupSpanEnd) { true }

        if (player.hasEffect(MobEffects.FIRE_RESISTANCE) || (notDuringCombat && CombatManager.isInCombat)) {
            return null
        }

        val pickupPos = pickupTracker.firstEligible(pickupSpanStart)

        if (pickupPos != null && Pickup.enabled) {
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
    private val tickHandler = handler<GameTickEvent> {
        val target = currentTarget ?: return@handler

        val rayTraceResult = traceFromPlayer()

        if (!target.doesCorrespondTo(rayTraceResult)) {
            return@handler
        }

        SilentHotbar.selectSlotSilently(this, target.hotbarItemSlot, 1)

        val successFunction = {
            cooldownTimer.waitForAtLeast((cooldown * 1000.0F).toLong())
            pickupTracker.record(target.placementTarget.placedBlock)

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
        return planPlacementAtPos(playerPos, waterBucketSlot, frameOnGround.pos)
    }

    private fun planPickup(blockPos: BlockPos): PlacementPlan? {
        val bucket = Slots.OffhandWithHotbar.findClosestSlot(Items.BUCKET) ?: return null
        return planPlacementAtPos(blockPos, bucket)
    }

}
