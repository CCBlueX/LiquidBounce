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
import net.minecraft.world.entity.LivingEntity.BASE_VERTICAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.entity.ai.attributes.Attributes
import net.minecraft.world.entity.animal.camel.Camel
import net.minecraft.world.entity.animal.equine.AbstractHorse
import net.minecraft.world.entity.vehicle.boat.AbstractBoat
import kotlin.math.max

enum class RideKind { BOAT, MOUNT }

/** The size and abilities of a ridden vehicle with its rider: heights in blocks, speeds in blocks per tick. */
@Suppress("LongParameterList")
class RideBody(
    val kind: RideKind,
    val width: Double,
    /** Room above where the vehicle rests for the vehicle and the eyes of its rider. */
    val height: Double,
    val step: Double,
    /** Highest ledge a fully charged jump reaches; 0 when it cannot jump. */
    val jump: Double,
    val safeFall: Int,
    val speed: Double,
    val iceSpeed: Double = speed,
) {

    companion object {
        const val BOAT_WATER_SPEED = 0.35
        const val BOAT_ICE_SPEED = 1.0

        /** Ticks the jump key is held for a ridden jump of full height. */
        const val JUMP_CHARGE_TICKS = 11

        private const val EYE_MARGIN = 0.05
        private const val JUMP_MARGIN = 0.2
        private const val BOAT_SAFE_FALL = 1

        // Top ground speed per point of the movement speed attribute, as LivingEntity.travel moves on plain ground.
        private const val GROUND_SPEED_FACTOR = 2.158

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

                is AbstractHorse -> RideBody(
                    kind = RideKind.MOUNT,
                    width = vehicle.bbWidth.toDouble(),
                    height = height,
                    step = vehicle.maxUpStep().toDouble(),
                    jump = jumpHeight(vehicle),
                    safeFall = vehicle.getAttributeValue(Attributes.SAFE_FALL_DISTANCE).toInt() - 1,
                    speed = vehicle.getAttributeValue(Attributes.MOVEMENT_SPEED) * GROUND_SPEED_FACTOR,
                )

                else -> null
            }
        }

        private fun jumpHeight(mount: AbstractHorse): Double {
            if (mount is Camel || !mount.canJump()) {
                return 0.0
            }

            var velocity = mount.getAttributeValue(Attributes.JUMP_STRENGTH) + mount.jumpBoostPower
            var height = 0.0
            while (velocity > 0.0) {
                height += velocity
                velocity = (velocity - DEFAULT_BASE_GRAVITY) * BASE_VERTICAL_AIR_DRAG
            }
            return (height - JUMP_MARGIN).coerceAtLeast(0.0)
        }
    }

}
