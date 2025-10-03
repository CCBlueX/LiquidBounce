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

@file:Suppress("FunctionName", "NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.additions

import net.minecraft.client.font.TextRenderer
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.RenderLayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Identifier
import java.util.function.Function

/**
 * Addition to [net.minecraft.client.gui.DrawContext].
 */
interface DrawContextAddition {


    /**
     * drawTexture with floats
     */
    @Suppress("LongParameterList")
    fun `liquid_bounce$drawTexture`(
        renderLayers: Function<Identifier, RenderLayer>,
        texture: Identifier,
        x: Float,
        y: Float,
        width: Int,
        height: Int,
    )

    /**
     * drawTexturedQuad with floats
     */
    @Suppress("LongParameterList")
    fun `liquid_bounce$drawTexturedQuad`(
        renderLayers: Function<Identifier, RenderLayer>,
        texture: Identifier,
        x1: Float,
        x2: Float,
        y1: Float,
        y2: Float,
        u1: Float,
        u2: Float,
        v1: Float,
        v2: Float,
        color: Int,
    )


    fun `liquidbounce$drawItemBar`(stack: ItemStack, x: Int, y: Int)

    fun `liquidbounce$drawStackCount`(
        textRenderer: TextRenderer,
        stack: ItemStack,
        x: Int,
        y: Int,
        stackCountText: String?,
    )

    fun `liquidbounce$drawCooldownProgress`(stack: ItemStack, x: Int, y: Int)

}

internal inline fun DrawContext.drawItemBar(stack: ItemStack, x: Int, y: Int) =
    (this as DrawContextAddition).`liquidbounce$drawItemBar`(stack, x, y)

internal inline fun DrawContext.drawStackCount(
    textRenderer: TextRenderer,
    stack: ItemStack,
    x: Int,
    y: Int,
    stackCountText: String?,
) =
    (this as DrawContextAddition).`liquidbounce$drawStackCount`(textRenderer, stack, x, y, stackCountText)

internal inline fun DrawContext.drawCooldownProgress(stack: ItemStack, x: Int, y: Int) =
    (this as DrawContextAddition).`liquidbounce$drawCooldownProgress`(stack, x, y)


