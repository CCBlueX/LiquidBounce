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

package net.ccbluex.liquidbounce.features.module.modules.render.crosshair

import net.ccbluex.liquidbounce.config.types.nesting.Configurable
import net.ccbluex.liquidbounce.event.events.OverlayRenderEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.render.drawTriangle
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.inventory.isInContainerScreen
import net.ccbluex.liquidbounce.utils.inventory.isInInventoryScreen
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

object ModuleCrosshair : ClientModule("Crosshair", ModuleCategories.RENDER) {

    private val radius by intRange("Range", 5..7, 1..25)
    private object Color : Configurable("Color") {
        val syncColors by boolean("Sync", true)
        val firstColor by color("FirstColor", Color4b(0, 0, 255, 255))
        val secondColor by color("SecondColor", Color4b(0, 0, 255, 255))
        object Spin : Configurable("Spin") {
            val invertSpin by boolean("InvertSpin", false)
            val spinSpeed by float("SpinSpeed", 4f, 0f..10f)
        }
    }

    init {
        tree(Color)
        tree(Color.Spin)
    }

    @Suppress("unused")
    private val cursorHandler = handler<OverlayRenderEvent> {
        if(isInInventoryScreen || isInContainerScreen) return@handler

        val centerWidth = (it.context.guiWidth() / 2.002f)
        val centerHeight = (it.context.guiHeight() / 2.0025f)
        val segments = 600

        for(i in 0 until segments) {
            val cAngle = (Mth.TWO_PI * i / segments)
            val nAngle = (Mth.TWO_PI * (i + 1) / segments)

            val innerCurrX = centerWidth + sin(cAngle) * radius.min()
            val innerCurrY = centerHeight + cos(cAngle) * radius.min()
            val innerNextX = centerWidth + sin(nAngle) * radius.min()
            val innerNextY = centerHeight + cos(nAngle) * radius.min()

            val outerCurrX = centerWidth + sin(cAngle) * radius.max()
            val outerCurrY = centerHeight + cos(cAngle) * radius.max()
            val outerNextX = centerWidth + sin(nAngle) * radius.max()
            val outerNextY = centerHeight + cos(nAngle) * radius.max()

            val color = getCurrentStepColor(Color.firstColor, Color.secondColor,
                Color.syncColors, Color.Spin.spinSpeed, Color.Spin.invertSpin, cAngle)
            
            it.context.drawTriangle(innerCurrX, innerCurrY, outerCurrX, outerCurrY, outerNextX, outerNextY, color)
            it.context.drawTriangle(innerCurrX, innerCurrY, outerNextX, outerNextY, innerNextX, innerNextY, color)
        }
    }
}

private fun getCurrentStepColor
        (firstColor: Color4b,
         secondColor: Color4b,
         syncColors: Boolean,
         spinSpeed: Float,
         invertSpin: Boolean,
         angle: Float): Color4b {
    val first = firstColor
    val second: Color4b = if (!syncColors) secondColor else firstColor
    val speed = if(!invertSpin) spinSpeed else -abs(spinSpeed)

    return getColorByAngle(angle, first, second, speed)
}

private fun getColorByAngle(angle: Float, color1: Color4b, color2: Color4b, speed: Float): Color4b {
    val timeOffset = if (speed != 0f) {
        ((System.currentTimeMillis().toDouble() / 10000.0) * speed.toDouble() % 1.0) * Mth.TWO_PI
    } else {
        0.0
    }

    val progress = (Mth.sin(angle + timeOffset) * 0.5 + 0.5)

    return color1.interpolateTo(color2, progress)
}
