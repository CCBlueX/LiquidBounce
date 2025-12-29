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
fun getTorusPoints(
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

// --- Data Classes ---
data class TorusQuad(
    val p1: Vector3f,
    val p2: Vector3f,
    val p3: Vector3f,
    val p4: Vector3f,
)

