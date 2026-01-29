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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.features.module.modules.render.nametags.NametagEnchantmentRenderer.drawEntityEnchantments
import net.ccbluex.liquidbounce.render.FontManager
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.minecraft.client.gui.GuiGraphics
import net.minecraft.world.entity.LivingEntity

private const val NAMETAG_PADDING: Int = 15

private const val FONT_SIZE = FontManager.DEFAULT_FONT_SIZE
private const val BASE_SCALE_FACTOR = 1f / (FONT_SIZE * 0.15f)
private const val BACKGROUND_X_OFFSET = 0.1f * FONT_SIZE
private const val BACKGROUND_Y_OFFSET_TOP = -0.1f
private const val BACKGROUND_Y_OFFSET_BOTTOM = 1.1f
private const val BACKGROUND_X_PADDING = 0.2f * FONT_SIZE

internal fun GuiGraphics.drawNametag(nametag: NametagRenderState, posX: Float, posY: Float) {
    val entity = nametag.entity ?: return
    if (nametag.equipments.itemStacks.any { !it.isEmpty }) {
        drawItemStackList(nametag.equipments.itemStacks)
            .centerX(posX)
            .centerY(posY - NAMETAG_PADDING * nametag.scale)
            .scale(nametag.scale)
            .itemStackRenderer(nametag.equipmentStackRenderer())
            .rectBackground(Color4b.TRANSPARENT)
            .draw()
    }

    val scale = BASE_SCALE_FACTOR * nametag.scale

    pose().pushMatrix()
    pose().translate(posX, posY)
    pose().scale(scale, scale)

    val fontRenderer = ModuleNametags.fontRenderer
    val processedText = fontRenderer.process(nametag.text)
    val textWidth = fontRenderer.getStringWidth(processedText, shadow = true)

    // Make the model view matrix center the text when rendering
    pose().translate(-textWidth * 0.5f, -fontRenderer.height * 0.5f)

    val x1 = -BACKGROUND_X_OFFSET
    val y1 = fontRenderer.height * BACKGROUND_Y_OFFSET_TOP
    val x2 = textWidth + BACKGROUND_X_PADDING
    val y2 = fontRenderer.height * BACKGROUND_Y_OFFSET_BOTTOM

    // Background
    drawQuad(
        x1, y1, x2, y2,
        fillColor = Color4b.DEFAULT_BG_COLOR,
        outlineColor = Color4b.BLACK.takeIf { ModuleNametags.border },
    )

    // Text
    fontRenderer.draw(processedText) {
        shadow = true
    }

    // Draw enchantments directly for the entity (regardless of whether items are shown)
    if (NametagEnchantmentRenderer.running && entity is LivingEntity) {
        val entityPos = entity.position()
        val worldX = entityPos.x.toFloat()
        val worldY = (entityPos.y + entity.bbHeight + 0.5f).toFloat()

        drawEntityEnchantments(
            entity,
            worldX,
            worldY,
        )
    }

    pose().popMatrix()
}
