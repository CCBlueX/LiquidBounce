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

import net.ccbluex.liquidbounce.render.engine.type.Color4b

/**
 * Background of an [ItemStackListRenderState].
 */
sealed interface ItemStackListBackground {

    /**
     * Space the background takes around the content of the list. [GuiOverlapRearranger] keeps this space free.
     */
    val padding: Float

    /**
     * Recolors the outline, for lists that outline per widget.
     */
    fun withOutline(color: Color4b): ItemStackListBackground

    /**
     * Places every stack on a vanilla container slot.
     */
    data object Slots : ItemStackListBackground {
        override val padding get() = 0F

        override fun withOutline(color: Color4b) = this
    }

    /**
     * Rounded panel drawn behind the stacks.
     *
     * @param radius Corner radius in GUI units, the shader clamps it to half of the smaller side.
     * @param gradient How far the bottom edge of the fill is darkened towards black.
     * @param glow Alpha of the outer glow layer, `0` disables the glow.
     */
    data class Panel(
        val fillColor: Color4b = Color4b.DEFAULT_BG_COLOR,
        val outlineColor: Color4b = Color4b.TRANSPARENT,
        val margin: Float = DEFAULT_PANEL_MARGIN,
        val radius: Float = DEFAULT_PANEL_RADIUS,
        val gradient: Float = DEFAULT_PANEL_GRADIENT,
        val glow: Float = DEFAULT_PANEL_GLOW,
    ) : ItemStackListBackground {

        override val padding get() = margin + if (glow > 0F) BACKGROUND_GLOW_SPREAD else 0F

        override fun withOutline(color: Color4b) = copy(outlineColor = color)

        companion object {
            @JvmField
            val DEFAULT = Panel()
        }

    }

}

internal const val DEFAULT_PANEL_MARGIN = 2.0F
internal const val DEFAULT_PANEL_RADIUS = 4.0F
internal const val DEFAULT_PANEL_GRADIENT = 0.3F
internal const val DEFAULT_PANEL_GLOW = 0.3F
