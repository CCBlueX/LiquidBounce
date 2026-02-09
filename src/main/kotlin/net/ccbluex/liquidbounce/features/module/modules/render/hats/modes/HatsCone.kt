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
import kotlin.math.cos
import kotlin.math.sin

/**
 * @author minecrrrr
 */
internal object HatsCone : HatsMode("Cone") {

    private val colors = HatsColorSettings()

    private object HatConeSettings : ValueGroup("HatSettings") {
        object RadiusSettings : ValueGroup("RadiusSettings") {
            val outerRadius by float("OuterRadius", 0.6f, 0.1f..2f)
        }

        val peak by float("Peak", 0.3f, 0.01f..2f)
    }

    init {
        tree(HatConeSettings)
        tree(HatConeSettings.RadiusSettings)
        tree(colors)
    }

    override fun WorldRenderEnvironment.drawHat(isHurt: Boolean) {
        drawCustomMesh(ClientRenderPipelines.triangleStrip(noDepthTest = true)) { matrix ->
            val segments = 600
            for (i in 0..segments) {
                val angle = (i.toFloat() / segments) * Mth.TWO_PI
                val cosine = cos(angle)
                val sine = sin(angle)

                val color = if (!isHurt) colors.getCurrentStepColor(angle) else Color4b(255, 0, 0, colors.firstColor.a)

                addVertex(
                    matrix,
                    cosine * HatConeSettings.RadiusSettings.outerRadius,
                    0f,
                    sine * HatConeSettings.RadiusSettings.outerRadius
                ).setColor(color)
                addVertex(matrix, 0f, HatConeSettings.peak, 0f).setColor(color)
            }
        }
    }
}
