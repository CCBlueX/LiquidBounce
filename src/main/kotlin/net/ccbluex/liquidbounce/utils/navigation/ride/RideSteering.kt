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
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.math.yaw
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Movement input that drives a ridden vehicle towards a point: a boat turns with left and right and paddles
 * forward, a mount goes where the rider looks.
 */
internal class RideSteering {

    private var lastYaw = Float.NaN

    /**
     * Ticks the jump key has been held for the next jump, or counting on after the release until it is retried.
     */
    private var chargeTicks = 0

    fun reset() {
        lastYaw = Float.NaN
        chargeTicks = 0
    }

    /**
     * Turns [boat] towards [target] and paddles once it faces it; with [brake], only while it is slow.
     */
    fun boat(event: MovementInputEvent, boat: Entity, target: Vec3, brake: Boolean) {
        val yaw = boat.yRot
        val turnRate = if (lastYaw.isNaN()) 0f else Mth.wrapDegrees(yaw - lastYaw)
        lastYaw = yaw

        // The boat keeps turning after the key is released, the more the less its floor slows it.
        val friction = if (isOnIce(boat)) ICE_FRICTION else WATER_FRICTION
        val coast = turnRate * friction / (1f - friction)
        val delta = target.subtract(boat.position())
        val off = Mth.wrapDegrees(delta.yaw - yaw)
        val error = off - coast

        val speed = boat.deltaMovement.horizontalDistance()
        val slowEnough = !brake || speed < BRAKE_SPEED
        event.directionalInput = DirectionalInput(
            forwards = abs(off) < BOAT_FORWARD_ANGLE && slowEnough,
            backwards = !slowEnough,
            left = error < -BOAT_DEAD_ANGLE,
            right = error > BOAT_DEAD_ANGLE,
        )
        event.jump = false
        event.sneak = false
    }

    /**
     * Faces the rider towards [target] and rides on once facing it. With [jumpTo], the point ahead a jump leads to,
     * the jump is charged and released once the mount is close to it.
     */
    fun mount(event: MovementInputEvent, mount: Entity, target: Vec3, jumpTo: Vec3?, reach: Double) {
        val desired = target.subtract(mount.position()).yaw
        face(desired)
        val aligned = abs(Mth.wrapDegrees(desired - player.yRot)) < MOUNT_FORWARD_ANGLE

        var jump = false
        if (jumpTo != null && horizontalDistance(mount.position(), jumpTo) < reach) {
            chargeTicks++
            jump = chargeTicks <= RideBody.JUMP_CHARGE_TICKS
            if (chargeTicks > RideBody.JUMP_CHARGE_TICKS + JUMP_RETRY_TICKS) {
                chargeTicks = 0
            }
        } else {
            chargeTicks = 0
        }

        event.directionalInput = if (aligned) DirectionalInput.FORWARDS else DirectionalInput.NONE
        event.jump = jump
        event.sneak = false
    }

    /**
     * Stops pressing keys, holding [sneak] to get off.
     */
    fun stop(event: MovementInputEvent, sneak: Boolean = false) {
        lastYaw = Float.NaN
        chargeTicks = 0
        event.directionalInput = DirectionalInput.NONE
        event.jump = false
        event.sneak = sneak
    }

    /**
     * Turns the rider towards [yaw], which a mount follows; a boat only lets its rider look around.
     */
    fun face(yaw: Float) {
        player.yRot += Mth.wrapDegrees(yaw - player.yRot).coerceIn(-MAX_TURN, MAX_TURN)
    }

    private fun isOnIce(boat: Entity): Boolean {
        val below = BlockPos.containing(boat.x, boat.y - FLOOR_PROBE, boat.z)
        return world.getBlockState(below).block.friction >= ICE_THRESHOLD
    }

    companion object {
        private const val WATER_FRICTION = 0.9f
        private const val ICE_FRICTION = 0.98f
        private const val ICE_THRESHOLD = 0.9f
        private const val BOAT_DEAD_ANGLE = 4f
        private const val BOAT_FORWARD_ANGLE = 35f
        private const val BRAKE_SPEED = 0.12
        private const val MOUNT_FORWARD_ANGLE = 50f
        private const val MAX_TURN = 35f
        private const val JUMP_RETRY_TICKS = 20
        private const val FLOOR_PROBE = 0.1

        fun horizontalDistance(a: Vec3, b: Vec3): Double {
            val dx = a.x - b.x
            val dz = a.z - b.z
            return sqrt(dx * dx + dz * dz)
        }
    }

}
