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

import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.Mth
import org.joml.Vector2f
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

fun getAngle(i: Int, segments: Int) = i * Math.PI * 2 / segments
fun getNextAngle(i: Int, segments: Int) = (i + 1) * Math.PI * 2 / segments
fun getPointX(angle: Double, radius: Float) = (sin(angle) * radius).toFloat()
fun getPointZ(angle: Double, radius: Float) = (cos(angle) * radius).toFloat()

fun getTorusPoints(
    mainAngel: Double,
    tubeAngel: Double,
    radius: Float,
    tubeRadius: Float
): Triple<Float, Float, Float> {

    val x = ((radius + tubeRadius * cos(tubeAngel)) * sin(mainAngel)).toFloat()
    val y = (tubeRadius * sin(tubeAngel)).toFloat()
    val z = ((radius + tubeRadius * cos(tubeAngel)) * cos(mainAngel)).toFloat()

    return Triple(x, y, z)

}

fun getFlowerRadius(angle: Double, baseRadius: Float, points: Int, sharpness: Float): Float {

    val innerRadius = baseRadius * sharpness

    val f = (Math.PI / points)
    val r = (abs(angle % (f * 2) - f) / f).toFloat()

    return innerRadius + (baseRadius - innerRadius) * (1f - r)
}

fun getRotationAngle(speed: Float): Double {
    return (System.currentTimeMillis() % 360000) * 0.001 * speed
}

fun getToroidalMeshCords(outerCurrentAngle: Double, outerNextAngle: Double, innerCurrentAngle: Double,
                         innerNextAngle: Double, rotationAngle: Double, radii: Vector2f,
                         innerRadius: Float ): TorusQuad  {

    val currentRadius = radii.x
    val nextRadius = radii.y
    return TorusQuad (
        getTorusPoints(
            outerCurrentAngle + rotationAngle,
            innerCurrentAngle, currentRadius, innerRadius
        ),
        getTorusPoints(
            outerCurrentAngle + rotationAngle,
            innerNextAngle, currentRadius, innerRadius
        ),
        getTorusPoints(
            outerNextAngle + rotationAngle,
            innerCurrentAngle, nextRadius, innerRadius
        ),
        getTorusPoints(
            outerNextAngle + rotationAngle,
            innerNextAngle, nextRadius, innerRadius
        ),
    )
}

data class TorusQuad(
    val p1: Triple<Float, Float, Float>,
    val p2: Triple<Float, Float, Float>,
    val p3: Triple<Float, Float, Float>,
    val p4: Triple<Float, Float, Float>
)

fun getColorByAngle(angle: Double, color1: Color4b, color2: Color4b, speed: Float): Color4b {

    val timeOffset = if (speed > 0) (System.currentTimeMillis() % 10000L) / 10000.0 * Math.PI * 2 else 0.0

    val progress = (Mth.sin(angle + timeOffset) * 0.5 + 0.5).toFloat()

    return lerpColor(color1, color2, progress)
}

fun lerpColor(c1: Color4b, c2: Color4b, progress: Float): Color4b {

    val p = progress.coerceIn(0f, 1f)

    val r = (c1.r and 0xFF) + (((c2.r and 0xFF) - (c1.r and 0xFF)) * p).toInt()
    val g = (c1.g and 0xFF) + (((c2.g and 0xFF) - (c1.g and 0xFF)) * p).toInt()
    val b = (c1.b and 0xFF) + (((c2.b and 0xFF) - (c1.b and 0xFF)) * p).toInt()
    val a = (c1.a and 0xFF) + (((c2.a and 0xFF) - (c1.a and 0xFF)) * p).toInt()

    return Color4b(r, g, b, a)
}

fun getStarRadius(angle: Double, baseRadius: Float, points: Int, sharpness: Float, smooth: Boolean, exponent: Double): Float{

    val section = (Math.PI * 2) / points
    val m = (angle % section) / section
    val dist = abs(m * 2.0 - 1.0)

    val linearProgress = 1.0 - dist

    val radius = if (smooth) {
        baseRadius * (1.0 - sharpness + sharpness * dist)
    } else {
        baseRadius * (1.0 - sharpness + sharpness * linearProgress.pow(exponent))
    }

    return radius.toFloat()
}
