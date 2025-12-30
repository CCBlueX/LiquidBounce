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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Radiuses
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getColorByAngle
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getNextAngle
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.addVertex
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * @author minecrrrr
 */
internal object HatsHalo : HatsMode("Halo") {

    private object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val firstColor by color("FirstColor", Color4b(0, 0, 255, 125))
        val secondColor by color("SecondColor", Color4b(0, 0, 255, 125))

        object ColorSpin : ToggleableConfigurable(this@HatsHalo, "ColorSpin", true) {
            val spinSpeed by float("SpinSpeed", 1f, 0.1f..10f)
        }
    }

    private object HatHaloSettings : Configurable("HatSettings") {
        val outerRadius by float("Radius", 0.3f, 0.1f..2f)
        val innerRadius by float("Thickness", 0.05f, 0.01f..1f)
    }

    init {
        tree(HatHaloSettings)
        tree(Colors)
        tree(Colors.ColorSpin)
    }

    override fun WorldRenderEnvironment.drawHat() {
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

                val color = getColorByAngle(
                    outerCurAngleTorus,
                    Colors.firstColor,
                    if (!Colors.syncColors) Colors.secondColor else Colors.firstColor,
                    if (Colors.ColorSpin.enabled) Colors.ColorSpin.spinSpeed else 0.0f
                )

                val radiuses = Radiuses(
                    HatHaloSettings.outerRadius,
                    HatHaloSettings.outerRadius,
                    HatHaloSettings.innerRadius,
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
}
