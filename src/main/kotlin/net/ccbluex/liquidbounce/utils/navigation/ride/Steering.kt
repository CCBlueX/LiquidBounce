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

package net.ccbluex.liquidbounce.utils.navigation.ride

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.features.client.clientState
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceToSqr
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs

/** Drives a ridden vehicle by movement input: a boat with left, right and forward, a mount by where its rider looks. */
internal class Steering {

    private var lastYaw by clientState { Float.NaN }

    /** Ticks the jump key is held for the next jump, counting on after its release until the jump is retried. */
    private var chargeTicks by clientState { 0 }

    fun reset() {
        lastYaw = Float.NaN
        chargeTicks = 0
    }

    /** Turns [boat] towards [target] and paddles once it faces it; with [brake], only while it is slow. */
    fun boat(event: MovementInputEvent, boat: Entity, target: Vec3, brake: Boolean) {
        val yaw = boat.yRot
        val turnRate = if (lastYaw.isNaN()) 0f else Mth.wrapDegrees(yaw - lastYaw)
        lastYaw = yaw

        // The boat keeps turning after the key is released, the more the less its floor slows it.
        val friction = if (isOnIce(boat)) ICE_FRICTION else WATER_FRICTION
        val coast = turnRate * friction / (1f - friction)
        val off = Mth.wrapDegrees(target.subtract(boat.position()).yaw - yaw)
        val error = off - coast

        val slowEnough = !brake || boat.deltaMovement.horizontalDistance() < BRAKE_SPEED
        event.directionalInput = DirectionalInput(
            forwards = abs(off) < BOAT_FORWARD_ANGLE && slowEnough,
            backwards = !slowEnough,
            left = error < -BOAT_DEAD_ANGLE,
            right = error > BOAT_DEAD_ANGLE,
        )
        event.jump = false
        event.sneak = false
    }

    /** Rides towards [target]; within [reach] of [jumpTo], where a jump leads, the jump is charged and released. */
    fun mount(event: MovementInputEvent, mount: Entity, target: Vec3, jumpTo: Vec3?, reach: Double) {
        val desired = target.subtract(mount.position()).yaw
        face(desired)
        val aligned = abs(Mth.wrapDegrees(desired - player.yRot)) < MOUNT_FORWARD_ANGLE

        val atJump = jumpTo != null && mount.position().horizontalDistanceToSqr(jumpTo) < Mth.square(reach)
        chargeTicks = if (atJump) chargeTicks + 1 else 0
        event.directionalInput = if (aligned) DirectionalInput.FORWARDS else DirectionalInput.NONE
        event.jump = chargeTicks in 1..Body.JUMP_CHARGE_TICKS
        event.sneak = false
        if (chargeTicks > Body.JUMP_CHARGE_TICKS + JUMP_RETRY_TICKS) {
            chargeTicks = 0
        }
    }

    fun stop(event: MovementInputEvent, sneak: Boolean = false) {
        reset()
        event.directionalInput = DirectionalInput.NONE
        event.jump = false
        event.sneak = sneak
    }

    fun face(yaw: Float) {
        player.yRot += Mth.wrapDegrees(yaw - player.yRot).coerceIn(-MAX_TURN, MAX_TURN)
    }

    private fun isOnIce(boat: Entity): Boolean {
        val below = BlockPos.containing(boat.x, boat.y - FLOOR_PROBE, boat.z)
        return world.getBlockState(below).block.friction >= ICE_THRESHOLD
    }

    private companion object {
        const val WATER_FRICTION = 0.9f
        const val ICE_FRICTION = 0.98f
        const val ICE_THRESHOLD = 0.9f
        const val BOAT_DEAD_ANGLE = 4f
        const val BOAT_FORWARD_ANGLE = 35f
        const val BRAKE_SPEED = 0.12
        const val MOUNT_FORWARD_ANGLE = 50f
        const val MAX_TURN = 35f
        const val JUMP_RETRY_TICKS = 20
        const val FLOOR_PROBE = 0.1
    }

}
