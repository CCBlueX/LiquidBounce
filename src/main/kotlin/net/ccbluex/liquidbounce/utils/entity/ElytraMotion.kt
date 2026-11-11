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

package net.ccbluex.liquidbounce.utils.entity

import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity.DEFAULT_BASE_GRAVITY
import net.minecraft.world.entity.LivingEntity.ELYTRA_HORIZONTAL_AIR_DRAG
import net.minecraft.world.entity.LivingEntity.ELYTRA_VERTICAL_AIR_DRAG
import net.minecraft.world.phys.Vec3
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

/** Elytra flight as the game moves it, one tick at a time. */
internal object ElytraMotion {

    private const val LIFT = 0.75
    private const val DIVE = 0.1
    private const val CLIMB = 0.04
    private const val CLIMB_LIFT = 3.2
    private const val TURN = 0.1
    private const val ROCKET_PUSH = 0.1
    private const val ROCKET_SPEED = 1.5
    private const val ROCKET_PULL = 0.5

    fun step(velocity: Vec3, yaw: Float, pitch: Float, boosted: Boolean): Vec3 {
        val look = Vec3.directionFromRotation(pitch, yaw)
        val lean = Math.toRadians(pitch.toDouble())
        val lookHorizontal = look.horizontalDistance()
        val moveHorizontal = velocity.horizontalDistance()
        val lift = cos(lean) * cos(lean)

        var x = velocity.x
        var y = velocity.y + DEFAULT_BASE_GRAVITY * (-1.0 + lift * LIFT)
        var z = velocity.z
        if (lookHorizontal > 0.0) {
            if (y < 0.0) {
                val convert = y * -DIVE * lift
                x += look.x * convert / lookHorizontal
                y += convert
                z += look.z * convert / lookHorizontal
            }
            if (lean < 0.0) {
                val convert = moveHorizontal * -sin(lean) * CLIMB
                x -= look.x * convert / lookHorizontal
                y += convert * CLIMB_LIFT
                z -= look.z * convert / lookHorizontal
            }
            x += (look.x / lookHorizontal * moveHorizontal - x) * TURN
            z += (look.z / lookHorizontal * moveHorizontal - z) * TURN
        }

        x *= ELYTRA_HORIZONTAL_AIR_DRAG
        y *= ELYTRA_VERTICAL_AIR_DRAG
        z *= ELYTRA_HORIZONTAL_AIR_DRAG
        if (boosted) {
            x += look.x * ROCKET_PUSH + (look.x * ROCKET_SPEED - x) * ROCKET_PULL
            y += look.y * ROCKET_PUSH + (look.y * ROCKET_SPEED - y) * ROCKET_PULL
            z += look.z * ROCKET_PUSH + (look.z * ROCKET_SPEED - z) * ROCKET_PULL
        }
        return Vec3(x, y, z)
    }

    fun yawTowards(dx: Double, dz: Double) = Mth.wrapDegrees(Math.toDegrees(atan2(-dx, dz))).toFloat()

    fun pitchTowards(up: Double, ahead: Double) = (-Math.toDegrees(atan2(up, ahead))).toFloat()

}
