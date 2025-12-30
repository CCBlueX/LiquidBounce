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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.utils

import kotlin.math.cos
import kotlin.math.sin

// --- Common ---
fun getAngle(i: Int, segments: Int) = i * Math.PI * 2 / segments
fun getNextAngle(i: Int, segments: Int) = (i + 1) * Math.PI * 2 / segments
fun getPointX(angle: Double, radius: Float) = (sin(angle) * radius).toFloat()
fun getPointZ(angle: Double, radius: Float) = (cos(angle) * radius).toFloat()

fun getRotationAngle(speed: Float): Double {
    return (System.currentTimeMillis() % 360000) * 0.001 * speed
}

// --- Data Classes ---

data class Angles(
    val outerCurAngle: Double,
    val outerNextAngle: Double,
    val rotationAngle: Double,
)

data class Radiuses(
    val outerCurRadius: Float,
    val outerNextRadius: Float,
    val innerRadius: Float,
)
