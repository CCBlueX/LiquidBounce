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

package net.ccbluex.liquidbounce.render.gui

import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphicsExtractor

/**
 * Rounded panel drawn behind GUI content.
 *
 * @param radius Corner radius in GUI units, the shader clamps it to half of the smaller side.
 * @param gradient How far the bottom edge of the fill is darkened towards black.
 * @param glow Alpha of the outer glow layer, `0` disables the glow.
 */
data class GuiPanel(
    val fillColor: Color4b = Color4b.DEFAULT_BG_COLOR,
    val outlineColor: Color4b = Color4b.TRANSPARENT,
    val margin: Float = DEFAULT_PANEL_MARGIN,
    val radius: Float = DEFAULT_PANEL_RADIUS,
    val gradient: Float = DEFAULT_PANEL_GRADIENT,
    val glow: Float = DEFAULT_PANEL_GLOW,
) : ItemStackListBackground {

    /**
     * Space the panel takes around the content, including the glow.
     */
    override val padding: Float
        get() = margin + if (glow > 0.0F) BACKGROUND_GLOW_SPREAD else 0.0F

    override fun withOutline(color: Color4b) = copy(outlineColor = color)

    companion object {
        @JvmField
        val DEFAULT = GuiPanel()
    }

}

/**
 * Draws [panel] around a `0..width` by `0..height` box that starts at the current pose origin, returning whether
 * anything was drawn at all.
 */
internal fun GuiGraphicsExtractor.drawPanel(width: Float, height: Float, panel: GuiPanel): Boolean {
    if (panel.fillColor.isTransparent && panel.outlineColor.isTransparent) {
        return false
    }

    val x1 = -panel.margin
    val y1 = -panel.margin
    val x2 = width + panel.margin
    val y2 = height + panel.margin

    if (panel.glow > 0.0F) {
        val glowColor = if (panel.outlineColor.isTransparent) panel.fillColor else panel.outlineColor
        for (layer in BACKGROUND_GLOW_LAYERS downTo 1) {
            val spread = BACKGROUND_GLOW_SPREAD * layer / BACKGROUND_GLOW_LAYERS
            drawRoundedRect(
                x1 = x1 - spread,
                y1 = y1 - spread,
                x2 = x2 + spread,
                y2 = y2 + spread,
                radius = panel.radius + spread,
                fillColor = glowColor.fade(panel.glow / layer),
            )
        }
    }

    val darken = panel.gradient.toDouble()
    drawRoundedRect(
        x1 = x1,
        y1 = y1,
        x2 = x2,
        y2 = y2,
        radius = panel.radius,
        fillColor = panel.fillColor,
        fillBottomColor = panel.fillColor.interpolateTo(
            Color4b.BLACK,
            tR = darken,
            tG = darken,
            tB = darken,
            tA = 0.0,
        ),
        outlineColor = panel.outlineColor,
        outlineWidth = BACKGROUND_OUTLINE_WIDTH,
    )
    return true
}

internal const val DEFAULT_PANEL_MARGIN = 2.0F
internal const val DEFAULT_PANEL_RADIUS = 4.0F
internal const val DEFAULT_PANEL_GRADIENT = 0.3F
internal const val DEFAULT_PANEL_GLOW = 0.3F
internal const val BACKGROUND_GLOW_SPREAD = 3.0F

private const val BACKGROUND_OUTLINE_WIDTH = 1.0F
private const val BACKGROUND_GLOW_LAYERS = 3
