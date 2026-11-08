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
import net.ccbluex.liquidbounce.render.drawCircle
import net.minecraft.util.Mth
import net.minecraft.util.Util

/**
 * A dot with a ring that keeps growing from the inner to the outer radius while it fades out.
 */
object CrosshairPulse : CrosshairMode("Pulse") {

    private val dotRadius by float("DotRadius", 1f, 0.25f..5f)
    private val ringRadius by floatRange("RingRadius", 3f..7f, 1f..25f)
    private val ringThickness by float("RingThickness", 1f, 0.25f..4f)
    private val speed by float("Speed", 1.5f, 0.1f..5f, "Hz")

    private val color = CrosshairColorSettings()

    init {
        tree(color)
    }

    override fun OverlayRenderEvent.drawCrosshair() {
        val dotColor = stepColor(0f)
        context.drawCircle(0f, 0f, dotRadius) { dotColor.argb }

        val phase = (Util.getMillis() / 1000.0 * speed % 1.0).toFloat()
        val radius = Mth.lerp(phase, ringRadius.start, ringRadius.endInclusive)

        context.drawCircle(0f, 0f, radius, (radius - ringThickness).coerceAtLeast(0f)) { angle ->
            stepColor(angle).fade(1f - phase).argb
        }
    }

    private fun stepColor(angle: Float) =
        color.getCurrentStepColor(color.firstColor, color.secondColor, color.syncColors, color.spinSpeed, angle)
}
