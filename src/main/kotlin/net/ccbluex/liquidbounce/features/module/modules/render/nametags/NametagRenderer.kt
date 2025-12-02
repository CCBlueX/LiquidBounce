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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.features.module.modules.render.nametags.NametagEnchantmentRenderer.drawEntityEnchantments
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.client.gui.DrawContext
import net.minecraft.entity.LivingEntity

private const val NAMETAG_PADDING: Int = 15

internal fun DrawContext.drawNametag(nametag: Nametag, posX: Float, posY: Float) {
    if (NametagShowOptions.ITEMS.isShowing()) {
        val currentItemStackRenderer = if (NametagShowOptions.ITEM_INFO.isShowing()) {
            if (nametag.entity === player) {
                ItemStackListRenderer.SingleItemStackRenderer.All
            } else {
                ItemStackListRenderer.SingleItemStackRenderer.ForOtherPlayer
            }
        } else {
            ItemStackListRenderer.SingleItemStackRenderer.OnlyItem
        }

        drawItemStackList(nametag.items)
            .centerX(posX)
            .centerY(posY - NAMETAG_PADDING * ModuleNametags.scale)
            .scale(ModuleNametags.scale)
            .itemStackRenderer(currentItemStackRenderer)
            .rectBackground(Color4b.TRANSPARENT)
            .draw()
    }

    val fontSize = FontManager.DEFAULT_FONT_SIZE

    val scale = 1f / (fontSize * 0.15f) * ModuleNametags.scale

    matrices.pushMatrix()
    matrices.translate(posX, posY)
    matrices.scale(scale, scale)

    val fontRenderer = ModuleNametags.fontRenderer
    val processedText = fontRenderer.process(nametag.text)
    val textWidth = fontRenderer.getStringWidth(processedText, shadow = true)

    // Make the model view matrix center the text when rendering
    matrices.translate(-textWidth * 0.5f, -fontRenderer.height * 0.5f)

    val x1 = -0.1f * fontSize
    val y1 = fontRenderer.height * -0.1f
    val x2 = textWidth + 0.2f * fontSize
    val y2 = fontRenderer.height * 1.1f

    // Background
    drawQuad(
        x1, y1, x2, y2,
        fillColor = Color4b(Int.MIN_VALUE, hasAlpha = true),
        outlineColor = Color4b.BLACK.takeIf { NametagShowOptions.BORDER.isShowing() },
    )

    // Text
    fontRenderer.draw(
        processedText,
        x0 = 0f, y0 = 0f,
        shadow = true,
    )

    // Draw enchantments directly for the entity (regardless of whether items are shown)
    if (NametagShowOptions.ENCHANTMENTS.isShowing() && nametag.entity is LivingEntity) {
        val entityPos = nametag.entity.pos
        val worldX = entityPos.x.toFloat()
        val worldY = (entityPos.y + nametag.entity.height + 0.5f).toFloat()

        drawEntityEnchantments(
            nametag.entity,
            worldX,
            worldY,
        )
    }

    matrices.popMatrix()
}
