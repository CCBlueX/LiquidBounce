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
 * Background of an [ItemStackListRenderState], see [GuiPanel] for the rounded panel most lists use.
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

}
