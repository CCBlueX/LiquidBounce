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

package net.ccbluex.liquidbounce.features.module.modules.render.hats

import kotlin.math.cos
import kotlin.math.sin

fun getAngle(i: Int, segments: Int) = i * Math.PI * 2 / segments
fun getNextAngle(i: Int, segments: Int) = (i + 1) * Math.PI * 2 / segments
fun getPointX(angle: Double, radius: Float) = (sin(angle) * radius).toFloat()
fun getPointZ(angle: Double, radius: Float) = (cos(angle) * radius).toFloat()

fun getTorusPoints(mainAngel: Double, tubeAngel: Double, radius: Float, tubeRadius: Float ): Triple<Float, Float, Float> {

    val x = ((radius + tubeRadius * cos(tubeAngel)) * sin(mainAngel)).toFloat()
    val y = (tubeRadius * sin(tubeAngel)).toFloat()
    val z = ((radius + tubeRadius * cos(tubeAngel)) * cos(mainAngel)).toFloat()

    return Triple(x, y, z)

}

fun getStarRadius(angle: Double, baseRadius: Float, points: Int, sharpness: Float): Float {

    val innerRadius = baseRadius * sharpness

    val f = (Math.PI / points)
    val r = (abs(angle % (f * 2) - f) / f).toFloat()

    return innerRadius + (baseRadius - innerRadius) * (1f - r)
}