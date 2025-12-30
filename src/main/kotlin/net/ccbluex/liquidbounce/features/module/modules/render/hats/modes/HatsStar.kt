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

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.module.modules.render.hats.HatsMode
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Angles
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Colors
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Radiuses
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getCurrentStepColor
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getRotationAngle
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.pow

/**
 * @author minecrrrr
 */
internal object HatsStar : HatsMode("Star") {

    private object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val firstColor by color("FirstColor", Color4b(0, 0, 255, 125))
        val secondColor by color("SecondColor", Color4b(0, 0, 255, 125))

        object ColorSpin : ToggleableConfigurable(this@HatsStar, "ColorSpin", true) {
            val spinSpeed by float("SpinSpeed", 1f, 0.1f..10f)
        }
    }

    private object HatStarSettings : Configurable("HatSettings") {
        val outerRadius by float("Radius", 0.3f, 0.1f..2f)
        val innerRadius by float("Thickness", 0.05f, 0.01f..1f)
        val sharpness by float("Sharpness", 0.6f, 0.1f..0.7f)
        val pointsCount by int("PointsCount", 5, 5..15)

        object StarSpin : ToggleableConfigurable(this@HatsStar, "Spin", true) {
            val spinSpeed by float("Speed", 1f, 0.1f..10f)
        }
    }

    init {
        tree(HatStarSettings)
        tree(HatStarSettings.StarSpin)
        tree(Colors)
        tree(Colors.ColorSpin)
    }

    private val colors
        get() = Colors(
            Colors.syncColors,
            Colors.firstColor,
            Colors.secondColor,
            Colors.ColorSpin.enabled,
            Colors.ColorSpin.spinSpeed,
        )

    override fun WorldRenderEnvironment.drawHat() {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val rotAngle = if (HatStarSettings.StarSpin.enabled) {
                getRotationAngle(HatStarSettings.StarSpin.spinSpeed)
            } else {
                0.0F
            }
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

                val color = getCurrentStepColor(outerCurAngleStar, colors)
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
                    addVertex(matrix, pos.p1).color(color)
                    addVertex(matrix, pos.p2).color(color)
                    addVertex(matrix, pos.p3).color(color)
                    addVertex(matrix, pos.p2).color(color)
                    addVertex(matrix, pos.p4).color(color)
                    addVertex(matrix, pos.p3).color(color)
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

