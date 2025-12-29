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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getColorByAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getNextAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getRotationAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getStarRadius
import net.ccbluex.liquidbounce.features.module.modules.render.hats.getToroidalMeshCords
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import org.joml.Vector2f

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
        val radius by float("Radius", 0.3f, 0.1f..2f)
        val tubeRadius by float("Thickness", 0.05f, 0.01f..1f)
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

    override fun WorldRenderEnvironment.drawHat() {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val rotAngle = if (HatStarSettings.StarSpin.enabled) {
                getRotationAngle(HatStarSettings.StarSpin.spinSpeed)
            } else {
                0.0
            }
            val outerSegments = HatStarSettings.pointsCount * 120
            val innerSegments = HatStarSettings.pointsCount * 2
            val points = HatStarSettings.pointsCount

            for (mainI in 0 until outerSegments) {

                val mainCurrentAngleStar = getAngle(mainI, outerSegments)
                val mainNextAngleStar = getNextAngle(mainI, outerSegments)

                val currentRadius = getStarRadius(
                    mainCurrentAngleStar,
                    HatStarSettings.radius,
                    points,
                    HatStarSettings.sharpness,
                    1.75,
                )
                val nextRadius = getStarRadius(
                    mainNextAngleStar,
                    HatStarSettings.radius,
                    points,
                    HatStarSettings.sharpness,
                    1.75,
                )

                for (tubeI in 0 until innerSegments) {

                    val tubeCurrentAngleStar = getAngle(tubeI, innerSegments)
                    val tubeNextAngleStar = getNextAngle(tubeI, innerSegments)

                    val color = getColorByAngle(
                        mainCurrentAngleStar,
                        Colors.firstColor,
                        if (!Colors.syncColors) Colors.secondColor else Colors.firstColor,
                        if (Colors.ColorSpin.enabled) {Colors.ColorSpin.spinSpeed} else {
                            0f
                        },
                    )

                    val radii = Vector2f(currentRadius, nextRadius)
                    val quad = getToroidalMeshCords(
                        mainCurrentAngleStar,
                        mainNextAngleStar,
                        tubeCurrentAngleStar,
                        tubeNextAngleStar,
                        rotAngle,
                        radii,
                        HatStarSettings.tubeRadius
                    )

                    addVertex(matrix, quad.p1).color(color)
                    addVertex(matrix, quad.p2).color(color)
                    addVertex(matrix, quad.p3).color(color)
                    addVertex(matrix, quad.p2).color(color)
                    addVertex(matrix, quad.p4).color(color)
                    addVertex(matrix, quad.p3).color(color)
                }
            }
        }
    }

}
