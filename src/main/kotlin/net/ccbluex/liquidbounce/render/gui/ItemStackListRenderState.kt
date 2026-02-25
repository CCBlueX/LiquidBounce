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

import net.ccbluex.liquidbounce.render.engine.type.BoundingBox2f
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack
import org.joml.Vector2fc

data class ItemStackListRenderState internal constructor(
    internal val guiGraphics: GuiGraphics,
    internal val stacks: List<ItemStack>,
    internal val title: Component? = null,
    internal val titleColor: Int = 0xffffffff.toInt(),
    internal val centerX: Float = 0F,
    internal val centerY: Float = 0F,
    internal val scale: Float = 1.0F,
    internal val rowLength: Int = 9,
    internal val backgroundColor: Color4b = Color4b.DEFAULT_BG_COLOR,
    internal val backgroundOutlineColor: Color4b = Color4b.TRANSPARENT,
    internal val backgroundMargin: Float = 2.0F,
    internal val useTexture: Boolean = false,
    internal val itemStackRenderer: ItemStackListRenderer.SingleItemStackRenderer =
        ItemStackListRenderer.SingleItemStackRenderer.All,
) : GuiRearrangeable {

    override var bounds: BoundingBox2f = ItemStackListLayout.computeBounds(this)

    init {
        require(rowLength > 0) { "Row length must be greater than zero." }
        require(scale > 0F) { "Scale must be greater than zero." }
        require(backgroundMargin >= 0F) { "Background margin must not be negative." }
    }

    @JvmOverloads
    fun title(title: Component?, color: Int = this.titleColor): ItemStackListRenderState {
        return copy(title = title, titleColor = color)
    }

    fun centerX(centerX: Float): ItemStackListRenderState {
        return copy(centerX = centerX)
    }

    fun centerY(centerY: Float): ItemStackListRenderState {
        return copy(centerY = centerY)
    }

    fun center(center: Vector2fc): ItemStackListRenderState {
        return copy(centerX = center.x(), centerY = center.y())
    }

    /**
     * @param rowLength The maximum count of stack which can be placed in one row.
     */
    fun rowLength(rowLength: Int): ItemStackListRenderState {
        require(rowLength > 0) { "Row length must be greater than zero." }
        return copy(rowLength = rowLength)
    }

    fun scale(scale: Float): ItemStackListRenderState {
        require(scale > 0F) { "Scale must be greater than zero." }
        return copy(scale = scale)
    }

    @JvmOverloads
    fun rectBackground(
        color: Color4b,
        outlineColor: Color4b = Color4b.TRANSPARENT,
        margin: Float = this.backgroundMargin,
    ): ItemStackListRenderState {
        require(margin >= 0F) { "Background margin must not be negative." }
        return copy(
            backgroundColor = color,
            backgroundOutlineColor = outlineColor,
            backgroundMargin = margin,
            useTexture = false,
        )
    }

    fun textureBackground(): ItemStackListRenderState {
        return copy(
            useTexture = true,
            backgroundColor = Color4b.TRANSPARENT,
            backgroundOutlineColor = Color4b.TRANSPARENT,
            backgroundMargin = 0F,
        )
    }

    fun background(choice: ItemStackListRenderer.BackgroundMode): ItemStackListRenderState =
        when (choice) {
            is ItemStackListRenderer.BackgroundMode.Rect -> rectBackground(
                choice.fillColor,
                choice.outlineColor,
                choice.margin,
            )
            is ItemStackListRenderer.BackgroundMode.Texture -> textureBackground()
        }

    fun itemStackRenderer(itemStackRenderer: ItemStackListRenderer.SingleItemStackRenderer): ItemStackListRenderState {
        return copy(itemStackRenderer = itemStackRenderer)
    }

    @JvmOverloads
    fun draw(rearrange: Boolean = false) {
        ItemStackListRenderer.draw(this, rearrange)
    }
}
