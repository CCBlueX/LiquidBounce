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

package net.ccbluex.liquidbounce.features.module.modules.render.hats.modes

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.setColor
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.Mth
import kotlin.math.abs

/**
 * @author minecrrrr
 */
internal object HatsFlower : HatsMode("Flower") {

    private val colors = HatsColorSettings()

    private object HatFlowerSettings : ValueGroup("HatSettings") {
        val outerRadius by float("Radius", 0.3f, 0.1f..2f)
        val innerRadius by float("Thickness", 0.05f, 0.01f..1f)
        val sharpness by float("Sharpness", 0.6f, 0.1f..0.9f)
        val petalCount by int("PetalCount", 5, 5..15)
        val spinSpeed by float("SpinSpeed", 1f, -10f..10f)
    }

    init {
        tree(HatFlowerSettings)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val rotAngle = getRotationAngle(HatFlowerSettings.spinSpeed)
            val petals = HatFlowerSettings.petalCount
            val outerSegments = petals * 120
            val innerSegments = petals * 2


            for (outerI in 0 until outerSegments) {
                // Outer
                val outerCurAngleFlower = getAngle(outerI, outerSegments)
                val outerNextAngleFlower = getNextAngle(outerI, outerSegments)

                val curRadius = getFlowerRadius(
                    outerCurAngleFlower,
                    HatFlowerSettings.outerRadius,
                    petals,
                    HatFlowerSettings.sharpness
                )
                val nextRadius = getFlowerRadius(
                    outerNextAngleFlower,
                    HatFlowerSettings.outerRadius,
                    petals,
                    HatFlowerSettings.sharpness
                )

                val color = if (!isHurt) {
                    colors
                        .getCurrentStepColor(outerCurAngleFlower)
                } else {
                    Color4b(255, 0, 0, colors.firstColor.a)
                }

                val angles = Angles(
                    outerCurAngleFlower,
                    outerNextAngleFlower,
                    rotAngle,
                )
                val radiuses = Radiuses(
                    curRadius,
                    nextRadius,
                    HatFlowerSettings.innerRadius
                )

                // Inner
                for (innerI in 0 until innerSegments) {
                    val pos = innerI(innerSegments, angles, radiuses, innerI)
                    addVertex(matrix, pos.p1).setColor(color)
                    addVertex(matrix, pos.p2).setColor(color)
                    addVertex(matrix, pos.p3).setColor(color)
                    addVertex(matrix, pos.p2).setColor(color)
                    addVertex(matrix, pos.p4).setColor(color)
                    addVertex(matrix, pos.p3).setColor(color)
                }
            }
        }
    }

    private fun getFlowerRadius(angle: Float, baseRadius: Float, points: Int, sharpness: Float): Float {
        val innerRadius = baseRadius * sharpness
        val f = Mth.PI / points
        val r = abs(angle % (f * 2) - f) / f

        return innerRadius + (baseRadius - innerRadius) * (1f - r)
    }

}

