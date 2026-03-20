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

import net.ccbluex.fastutil.weightedMinByOrNullAtMost
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.once
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.utils.block.SwingMode
import net.ccbluex.liquidbounce.utils.entity.interactEntity
import net.ccbluex.liquidbounce.utils.entity.squaredBoxedDistanceTo
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.PlayerRideable
import net.minecraft.world.entity.animal.happyghast.HappyGhast
import net.minecraft.world.entity.vehicle.VehicleEntity
import kotlin.math.min

/**
 * Tries to prevent fall damage by mounting a nearby rideable entity while falling.
 */
internal object NoFallMount : NoFallMode("Mount") {

    private val minFallDistance by float("MinFallDistance", 5f, 2f..50f)
    private val searchRange by float("SearchRange", 4.5f, 1f..8f)
    private val retryDelay by int("RetryDelay", 2, 0..20, "ticks")
    private val swingMode by enumChoice("SwingMode", SwingMode.DO_NOT_HIDE)

    /**
     * Automatically dismounts after a configurable delay.
     */
    private object AutoDismount : ToggleableValueGroup(NoFallMount, "AutoDismount", false) {
        val delay by intRange("Delay", 0..0, 0..20, "ticks")
    }

    private var lastTargetId = -1
    private var pendingMountedTargetId = -1
    private var dismountTargetId = -1
    private var dismountTicksLeft = -1

    init {
        tree(AutoDismount)
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        updateAutoDismount()

        if (player.isPassenger) {
            scheduleAutoDismountIfNeeded()
            return@tickHandler
        }

        if (!shouldAttemptRide()) {
            resetRideState()
            return@tickHandler
        }

        val maxSearchRange = min(searchRange.toDouble(), player.entityInteractionRange())
        val eyePosition = player.eyePosition

        val target = world.entitiesForRendering()
            .filter { entity ->
                isRideableTarget(entity)
            }.weightedMinByOrNullAtMost(maxSearchRange.sq()) {
                it.squaredBoxedDistanceTo(eyePosition)
            } ?: run {
                lastTargetId = -1
                pendingMountedTargetId = -1
                return@tickHandler
            }

        interactEntity(
            target,
            swingMode = swingMode,
        )
        lastTargetId = target.id
        pendingMountedTargetId = target.id

        waitTicks(retryDelay)
    }

    private fun shouldAttemptRide(): Boolean {
        return player.fallDistance > minFallDistance &&
            player.deltaMovement.y < 0.0 &&
            !player.onGround() &&
            !player.isPassenger
    }

    private fun isRideableTarget(entity: Entity): Boolean {
        return (entity is PlayerRideable || entity is VehicleEntity || entity is HappyGhast)
            && entity.canAddPassenger(player)
    }

    override fun disable() {
        resetState()
        super.disable()
    }

    private fun resetState() {
        resetRideState()
        resetAutoDismountState()
    }

    private fun resetRideState() {
        lastTargetId = -1
        pendingMountedTargetId = -1
    }

    private fun resetAutoDismountState() {
        dismountTargetId = -1
        dismountTicksLeft = -1
    }

    private fun scheduleAutoDismountIfNeeded() {
        if (!AutoDismount.enabled || dismountTargetId != -1) {
            return
        }

        val vehicle = player.vehicle ?: return
        if (vehicle.id != pendingMountedTargetId) {
            return
        }

        dismountTargetId = vehicle.id
        dismountTicksLeft = AutoDismount.delay.random()
    }

    private fun updateAutoDismount() {
        if (dismountTargetId == -1) {
            return
        }

        if (!AutoDismount.enabled) {
            resetAutoDismountState()
            return
        }

        val vehicle = player.vehicle
        if (vehicle == null || vehicle.id != dismountTargetId) {
            resetAutoDismountState()
            return
        }

        if (dismountTicksLeft <= 0) {
            once<MovementInputEvent> {
                it.sneak = true
                player.stopRiding()
            }
            resetAutoDismountState()
            return
        }

        dismountTicksLeft--
    }

}
