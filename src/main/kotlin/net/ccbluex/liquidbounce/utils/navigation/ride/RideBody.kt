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

import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import kotlin.math.max

enum class RideKind {
    /**
     * Floats on water and slides over ice; turned with left and right.
     */
    BOAT,

    /**
     * Walks and jumps where the rider looks.
     */
    MOUNT,
}

/**
 * The size and abilities of a ridden vehicle with its rider, captured on the render thread for one search.
 */
@Suppress("LongParameterList")
class RideBody(
    val kind: RideKind,
    val width: Double,

    /**
     * Room above where the vehicle rests for the vehicle and the eyes of its rider.
     */
    val height: Double,

    /**
     * Highest ledge it moves onto without jumping.
     */
    val step: Double,

    /**
     * Highest ledge a fully charged jump reaches; 0 when it cannot jump.
     */
    val jump: Double,

    /**
     * Highest drop without fall damage, in blocks.
     */
    val safeFall: Int,

    /**
     * Top speed on plain ground or water, in blocks per tick.
     */
    val speed: Double,

    /**
     * Top speed on ice, in blocks per tick.
     */
    val iceSpeed: Double = speed,
) {

    companion object {
        const val BOAT_WATER_SPEED = 0.35
        const val BOAT_ICE_SPEED = 1.0

        /**
         * Ticks a ridden jump is charged for its full height.
         */
        const val JUMP_CHARGE_TICKS = 11

        private const val EYE_MARGIN = 0.05
        private const val JUMP_MARGIN = 0.2
        private const val BOAT_SAFE_FALL = 1
        private const val GRAVITY = 0.08
        private const val DRAG = 0.98

        /**
         * Top ground speed per point of the movement speed attribute, after LivingEntity.travel on plain ground.
         */
        private const val GROUND_SPEED_FACTOR = 2.158

        /**
         * The body of [vehicle] ridden by [rider], or null when it is not one that can be steered to a goal.
         */
        fun of(rider: Entity, vehicle: Entity): RideBody? {
            val seat = vehicle.getPassengerRidingPosition(rider).y - vehicle.y
            val height = max(vehicle.bbHeight.toDouble(), seat + rider.eyeHeight + EYE_MARGIN)
            return when (vehicle) {
                is AbstractBoat -> RideBody(
                    kind = RideKind.BOAT,
                    width = vehicle.bbWidth.toDouble(),
                    height = height,
                    step = 0.0,
                    jump = 0.0,
                    safeFall = BOAT_SAFE_FALL,
                    speed = BOAT_WATER_SPEED,
                    iceSpeed = BOAT_ICE_SPEED,
                )

                is AbstractHorse -> {
                    val jump = if (vehicle is Camel || !vehicle.canJump()) {
                        0.0
                    } else {
                        jumpHeight(vehicle.getAttributeValue(Attributes.JUMP_STRENGTH) + vehicle.jumpBoostPower)
                    }
                    RideBody(
                        kind = RideKind.MOUNT,
                        width = vehicle.bbWidth.toDouble(),
                        height = height,
                        step = vehicle.maxUpStep().toDouble(),
                        jump = jump,
                        safeFall = vehicle.getAttributeValue(Attributes.SAFE_FALL_DISTANCE).toInt() - 1,
                        speed = vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED) * GROUND_SPEED_FACTOR,
                    )
                }

                else -> null
            }
        }

        /**
         * Highest ledge reached by a jump starting with [impulse], with some room to land on it.
         */
        fun jumpHeight(impulse: Double): Double {
            var velocity = impulse
            var height = 0.0
            while (velocity > 0.0) {
                height += velocity
                velocity = (velocity - GRAVITY) * DRAG
            }
            return (height - JUMP_MARGIN).coerceAtLeast(0.0)
        }
    }

}
