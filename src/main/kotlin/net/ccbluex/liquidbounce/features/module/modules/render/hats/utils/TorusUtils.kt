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

import org.joml.Vector2f
import org.joml.Vector3f
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author minecrrrr
 */
// --- Generic Torus ---
private fun getTorusPoints(
    mainAngle: Double,
    tubeAngle: Double,
    radius: Float,
    tubeRadius: Float
): Vector3f {

    val x = ((radius + tubeRadius * cos(tubeAngle)) * sin(mainAngle)).toFloat()
    val y = (tubeRadius * sin(tubeAngle)).toFloat()
    val z = ((radius + tubeRadius * cos(tubeAngle)) * cos(mainAngle)).toFloat()

    return Vector3f(x, y, z)

}


fun getToroidalMeshCords(
    angles: TorusAngles, radii: Vector2f,
    innerRadius: Float
): TorusQuad {

    val currentRadius = radii.x
    val nextRadius = radii.y
    return TorusQuad(
        getTorusPoints(
            angles.outerCurrentAngle + angles.rotationAngle,
            angles.innerCurrentAngle, currentRadius, innerRadius
        ),
        getTorusPoints(
            angles.outerCurrentAngle + angles.rotationAngle,
            angles.innerNextAngle, currentRadius, innerRadius
        ),
        getTorusPoints(
            angles.outerNextAngle + angles.rotationAngle,
            angles.innerCurrentAngle, nextRadius, innerRadius
        ),
        getTorusPoints(
            angles.outerNextAngle + angles.rotationAngle,
            angles.innerNextAngle, nextRadius, innerRadius
        ),
    )
}

// --- Data Classes ---
data class TorusQuad(
    val p1: Vector3f,
    val p2: Vector3f,
    val p3: Vector3f,
    val p4: Vector3f,
)

data class TorusAngles(
    val outerCurrentAngle: Double,
    val outerNextAngle: Double,
    val innerCurrentAngle: Double,
    val innerNextAngle: Double,
    val rotationAngle: Double,
)
