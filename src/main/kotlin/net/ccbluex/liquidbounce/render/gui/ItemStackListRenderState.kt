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
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.network.chat.Component
import net.minecraft.world.item.ItemStack

/**
 * Describes an item stack list that [ItemStackListRenderer] draws. The setters mutate this state and
 * return it, so a list is configured by chaining them and finishing with [draw].
 */
class ItemStackListRenderState(
    @JvmField val stacks: List<ItemStack>,
) : GuiRearrangeable {

    @JvmField var title: Component? = null

    @JvmField var titleColor: Color4b = Color4b.WHITE

    @JvmField var centerX: Float = 0F

    @JvmField var centerY: Float = 0F

    @JvmField var scale: Float = 1.0F

    @JvmField var rowLength: Int = 9

    @JvmField var backgroundColor: Color4b = Color4b.DEFAULT_BG_COLOR

    @JvmField var backgroundOutlineColor: Color4b = Color4b.TRANSPARENT

    @JvmField var backgroundMargin: Float = 2.0F

    @JvmField var useTexture: Boolean = false

    @JvmField var itemStackRenderer: ItemStackListRenderer.SingleItemStackRenderer =
        ItemStackListRenderer.SingleItemStackRenderer.All

    /**
     * Only [GuiOverlapRearranger] needs the box. States that are not queued for rearrangement are
     * positioned by [centerX] and [centerY] alone, thus this stays empty for them.
     */
    override var bounds: BoundingBox2f = BoundingBox2f.EMPTY

    @JvmOverloads
    fun title(title: Component?, color: Color4b = this.titleColor): ItemStackListRenderState = apply {
        this.title = title
        this.titleColor = color
    }

    fun centerX(centerX: Float): ItemStackListRenderState = apply {
        this.centerX = centerX
    }

    fun centerY(centerY: Float): ItemStackListRenderState = apply {
        this.centerY = centerY
    }

    /**
     * @param rowLength The maximum count of stack which can be placed in one row.
     */
    fun rowLength(rowLength: Int): ItemStackListRenderState = apply {
        require(rowLength > 0) { "Row length must be greater than zero." }
        this.rowLength = rowLength
    }

    fun scale(scale: Float): ItemStackListRenderState = apply {
        require(scale > 0F) { "Scale must be greater than zero." }
        this.scale = scale
    }

    @JvmOverloads
    fun rectBackground(
        color: Color4b,
        outlineColor: Color4b = Color4b.TRANSPARENT,
        margin: Float = this.backgroundMargin,
    ): ItemStackListRenderState = apply {
        require(margin >= 0F) { "Background margin must not be negative." }
        backgroundColor = color
        backgroundOutlineColor = outlineColor
        backgroundMargin = margin
        useTexture = false
    }

    fun textureBackground(): ItemStackListRenderState = apply {
        useTexture = true
        backgroundColor = Color4b.TRANSPARENT
        backgroundOutlineColor = Color4b.TRANSPARENT
        backgroundMargin = 0F
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

    fun itemStackRenderer(
        itemStackRenderer: ItemStackListRenderer.SingleItemStackRenderer,
    ): ItemStackListRenderState = apply {
        this.itemStackRenderer = itemStackRenderer
    }

    internal fun updateBounds() {
        bounds = ItemStackListLayout.computeBounds(this)
    }

    @JvmOverloads
    fun draw(graphics: GuiGraphicsExtractor, rearrange: Boolean = false) {
        ItemStackListRenderer.draw(graphics, this, rearrange)
    }
}
