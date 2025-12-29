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
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.Colors
import net.ccbluex.liquidbounce.features.module.modules.render.hats.utils.getCurrentStepColor
import net.ccbluex.liquidbounce.render.ClientRenderPipelines
import net.ccbluex.liquidbounce.render.WorldRenderEnvironment
import net.ccbluex.liquidbounce.render.color
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author minecrrrr
 */
internal object HatsCone : HatsMode("Cone") {

    private object Colors : Configurable("Colors") {
        val syncColors by boolean("SyncColors", true)
        val firstColor by color("InnerColor", Color4b(0, 0, 255, 125))
        val secondColor by color("OuterColor", Color4b(0, 0, 255, 125))

        object ColorSpin : ToggleableConfigurable(this@HatsCone, "ColorSpin", true) {
            val spinSpeed by float("SpinSpeed", 1f, 0.1f..10f)
        }
    }

    private object HatConeSettings : Configurable("HatSettings") {
        object RadiusSettings : Configurable("RadiusSettings") {
            val outerRadius by float("OuterRadius", 0.6f, 0.1f..2f)
        }

        val peak by float("Peak", 0.3f, 0.01f..2f)
    }

    init {
        tree(HatConeSettings)
        tree(HatConeSettings.RadiusSettings)
        tree(Colors)
        tree(Colors.ColorSpin)
    }

    private val colors get() = Colors(
        Colors.syncColors,
        Colors.firstColor,
        Colors.secondColor,
        Colors.ColorSpin.enabled,
        Colors.ColorSpin.spinSpeed,
    )

    override fun WorldRenderEnvironment.drawHat() {

        drawCustomMesh(ClientRenderPipelines.TriangleStrip) { matrix ->
            val segments = 600
            for (i in 0..segments) {
                val angle = (i.toDouble() / segments) * Math.PI * 2
                val cosine = cos(angle).toFloat()
                val sine = sin(angle).toFloat()

                val color = getCurrentStepColor(angle, colors)

                addVertex(
                    matrix,
                    cosine * HatConeSettings.RadiusSettings.outerRadius,
                    0f,
                    sine * HatConeSettings.RadiusSettings.outerRadius
                ).color(color)
                addVertex(matrix, 0f, HatConeSettings.peak, 0f).color(color)
            }
        }
    }
}
