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

/**
 * @author minecrrrr
 */
internal object HatsHalo : HatsMode("Halo") {

    private val colors = HatsColorSettings()

    private object HatHaloSettings : ValueGroup("HatSettings") {
        val outerRadius by float("Radius", 0.3f, 0.1f..2f)
        val innerRadius by float("Thickness", 0.05f, 0.01f..1f)
    }

    init {
        tree(HatHaloSettings)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.Triangles) { matrix ->
            val outerSegments = 600
            val innerSegments = 60

            // Main loop for creating the torus (donut) using segments.
            for (outerI in 0 until outerSegments) {

                val outerCurAngleTorus = getAngle(outerI, outerSegments)
                val outerNextAngleTorus = getNextAngle(outerI, outerSegments)

                // Nested loop for rendering the torus "thickness".
                val angles = Angles(
                    outerCurAngleTorus,
                    outerNextAngleTorus,
                    0.0F,
                )

                val color = if (!isHurt) {
                    colors
                        .getCurrentStepColor(outerCurAngleTorus)
                } else {
                    Color4b(255, 0, 0, colors.firstColor.a)
                }

                val radiuses = Radiuses(
                    HatHaloSettings.outerRadius,
                    HatHaloSettings.outerRadius,
                    HatHaloSettings.innerRadius,
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
}
