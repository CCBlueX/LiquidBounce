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

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.client.gui.screens.achievement.StatsScreen
import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f

/**
 * @see StatsScreen.ItemStatisticsList.SLOT_BG_SIZE
 */
internal const val ITEM_STACK_SLOT_SIZE = 18
internal const val ITEM_STACK_ITEM_SIZE = GuiRenderer.DEFAULT_ITEM_SIZE

internal object ItemStackListLayout {

    @JvmRecord
    internal data class ContentDimensions(
        val width: Int,
        val height: Int,
    )

    fun measureContent(state: ItemStackListRenderState): ContentDimensions {
        val textRenderer = mc.font
        val size = if (state.useTexture) ITEM_STACK_SLOT_SIZE else ITEM_STACK_ITEM_SIZE
        var width = size * minOf(state.stacks.size, state.rowLength)
        var height = size * (state.stacks.size / state.rowLength +
            if (state.stacks.size % state.rowLength != 0) 1 else 0)

        state.title?.let { title ->
            width = maxOf(width, textRenderer.width(title))
            height += textRenderer.lineHeight + if (state.stacks.isEmpty()) 0 else 2
        }

        return ContentDimensions(width, height)
    }

    fun computeBounds(state: ItemStackListRenderState): BoundingBox2f {
        val dimensions = measureContent(state)
        val w = (dimensions.width + state.backgroundMargin * 2f) * state.scale
        val h = (dimensions.height + state.backgroundMargin * 2f) * state.scale
        val halfW = w * 0.5f
        val halfH = h * 0.5f
        return BoundingBox2f(
            state.centerX - halfW,
            state.centerY - halfH,
            state.centerX + halfW,
            state.centerY + halfH,
        )
    }

}
