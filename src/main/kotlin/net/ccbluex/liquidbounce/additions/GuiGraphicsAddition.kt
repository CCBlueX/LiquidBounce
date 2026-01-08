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
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.item.ItemStack

/**
 * Addition to [net.minecraft.client.gui.GuiGraphics].
 */
interface GuiGraphicsAddition {

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

internal inline fun GuiGraphics.drawItemBar(stack: ItemStack, x: Int, y: Int) =
    (this as GuiGraphicsAddition).`liquidbounce$drawItemBar`(stack, x, y)

internal inline fun GuiGraphics.drawStackCount(
    textRenderer: Font,
    stack: ItemStack,
    x: Int,
    y: Int,
    stackCountText: String?,
) =
    (this as GuiGraphicsAddition).`liquidbounce$drawStackCount`(textRenderer, stack, x, y, stackCountText)

internal inline fun GuiGraphics.drawCooldownProgress(stack: ItemStack, x: Int, y: Int) =
    (this as GuiGraphicsAddition).`liquidbounce$drawCooldownProgress`(stack, x, y)

// Removed in 1.21.9, copied from 1.21.8
fun GuiGraphics.drawBorder(x: Int, y: Int, width: Int, height: Int, color: Int) {
    fill(x, y, x + width, y + 1, color)
    fill(x, y + height - 1, x + width, y + height, color)
    fill(x, y + 1, x + 1, y + height - 1, color)
    fill(x + width - 1, y + 1, x + width, y + height - 1, color)
}
