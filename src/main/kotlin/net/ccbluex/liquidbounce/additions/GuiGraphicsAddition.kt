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

@file:Suppress("FunctionName", "NOTHING_TO_INLINE")

package net.ccbluex.liquidbounce.additions

import net.minecraft.client.gui.Font
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.world.item.ItemStack

/**
 * Addition to [net.minecraft.client.gui.GuiGraphicsExtractor].
 */
interface GuiGraphicsExtractorAddition {

    fun `liquidbounce$drawItemBar`(stack: ItemStack, x: Int, y: Int)

    fun `liquidbounce$drawStackCount`(
        textRenderer: Font,
        stack: ItemStack,
        x: Int,
        y: Int,
        stackCountText: String?,
    )

    fun `liquidbounce$drawCooldownProgress`(stack: ItemStack, x: Int, y: Int)

}

internal inline fun GuiGraphicsExtractor.drawItemBar(stack: ItemStack, x: Int, y: Int) =
    (this as GuiGraphicsExtractorAddition).`liquidbounce$drawItemBar`(stack, x, y)

internal inline fun GuiGraphicsExtractor.drawStackCount(
    textRenderer: Font,
    stack: ItemStack,
    x: Int,
    y: Int,
    stackCountText: String?,
) =
    (this as GuiGraphicsExtractorAddition).`liquidbounce$drawStackCount`(textRenderer, stack, x, y, stackCountText)

internal inline fun GuiGraphicsExtractor.drawCooldownProgress(stack: ItemStack, x: Int, y: Int) =
    (this as GuiGraphicsExtractorAddition).`liquidbounce$drawCooldownProgress`(stack, x, y)

// Removed in 1.21.9, copied from 1.21.8
fun GuiGraphicsExtractor.drawBorder(x: Int, y: Int, width: Int, height: Int, color: Int) {
    fill(x, y, x + width, y + 1, color)
    fill(x, y + height - 1, x + width, y + height, color)
    fill(x, y + 1, x + 1, y + height - 1, color)
    fill(x + width - 1, y + 1, x + width, y + height - 1, color)
}
