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
import kotlin.math.pow

/**
 * @author minecrrrr
 */
internal object HatsStar : HatsMode("Star") {

    private val colors = HatsColorSettings()

    private object HatStarSettings : ValueGroup("HatSettings") {
        val outerRadius by float("Radius", 0.3f, 0.1f..2f)
        val innerRadius by float("Thickness", 0.05f, 0.01f..1f)
        val sharpness by float("Sharpness", 0.6f, 0.1f..0.7f)
        val pointsCount by int("PointsCount", 5, 5..15)
        val spinSpeed by float("SpinSpeed", 1f, -10f..10f)
    }

    init {
        tree(HatStarSettings)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val rotAngle = getRotationAngle(HatStarSettings.spinSpeed)
            val points = HatStarSettings.pointsCount
            val outerSegments = points * 120
            val innerSegments = points * 2

            for (mainI in 0 until outerSegments) {

                val outerCurAngleStar = getAngle(mainI, outerSegments)
                val outerNextAngleStar = getNextAngle(mainI, outerSegments)

                val curRadius = getStarRadius(
                    outerCurAngleStar,
                    HatStarSettings.outerRadius,
                    points,
                    HatStarSettings.sharpness,
                    1.75F,
                )
                val nextRadius = getStarRadius(
                    outerNextAngleStar,
                    HatStarSettings.outerRadius,
                    points,
                    HatStarSettings.sharpness,
                    1.75F,
                )

                val color = if (!isHurt) {
                    colors
                        .getCurrentStepColor(outerCurAngleStar)
                } else {
                    Color4b(255, 0, 0, colors.firstColor.a)
                }
                val angles = Angles(
                    outerCurAngleStar,
                    outerNextAngleStar,
                    rotAngle,
                )
                val radiuses = Radiuses(
                    curRadius,
                    nextRadius,
                    HatStarSettings.innerRadius
                )

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

    private fun getStarRadius(angle: Float, baseRadius: Float, points: Int, sharpness: Float, exponent: Float): Float {
        val section = Mth.TWO_PI / points
        val m = (angle % section) / section
        val dist = abs(m * 2.0F - 1.0F)
        val linearProgress = 1.0F - dist

        return (baseRadius * (1.0F - sharpness + sharpness * linearProgress.pow(exponent)))
    }

}

