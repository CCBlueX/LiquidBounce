/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair.modes

import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairColorSettings
import net.ccbluex.liquidbounce.features.module.modules.render.crosshair.CrosshairMode
import net.ccbluex.liquidbounce.render.drawTriangle
import net.minecraft.util.Mth
import kotlin.math.cos
import kotlin.math.sin

object CrosshairCircle : CrosshairMode("Circle") {
    private val color = CrosshairColorSettings()
    private val radiusConfig = Radius()
    private val dynRadius = Radius.DynRadius(this)

    init {
        tree(radiusConfig)
        tree(dynRadius)
        tree(color)
    }

    override fun OverlayRenderEvent.drawCrosshair(
        centerWidth: Float,
        centerHeight: Float,
    ) {
        val segments = 300

        for (i in 0 until segments) {
            val multiplier = dynamicCrosshair(dynRadius.multiplier, dynRadius.enabled)

            val innerRadius = radiusConfig.radius.min() + multiplier
            val outerRadius = radiusConfig.radius.max() + multiplier

            val cAngle = (Mth.TWO_PI * i / segments)
            val nAngle = (Mth.TWO_PI * (i + 1) / segments)

            val innerCurrX = centerWidth + sin(cAngle) * innerRadius
            val innerCurrY = centerHeight + cos(cAngle) * innerRadius
            val innerNextX = centerWidth + sin(nAngle) * innerRadius
            val innerNextY = centerHeight + cos(nAngle) * innerRadius

            val outerCurrX = centerWidth + sin(cAngle) * outerRadius
            val outerCurrY = centerHeight + cos(cAngle) * outerRadius
            val outerNextX = centerWidth + sin(nAngle) * outerRadius
            val outerNextY = centerHeight + cos(nAngle) * outerRadius

            val currentColor =
                color.getCurrentStepColor(
                    color.firstColor,
                    color.secondColor,
                    color.syncColors,
                    color.spinSpeed,
                    cAngle,
                )

            context.drawTriangle(innerCurrX, innerCurrY, outerCurrX, outerCurrY, outerNextX, outerNextY, currentColor)
            context.drawTriangle(innerCurrX, innerCurrY, outerNextX, outerNextY, innerNextX, innerNextY, currentColor)
        }
    }
}
