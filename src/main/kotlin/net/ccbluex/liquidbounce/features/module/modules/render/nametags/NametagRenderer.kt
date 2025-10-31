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

import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.player
import net.minecraft.entity.LivingEntity
import org.joml.Vector2f

private const val NAMETAG_PADDING: Int = 15

internal fun GUIRenderEnvironment.drawNametag(nametag: Nametag, pos: Vec3) {
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

        context.drawItemStackList(nametag.items)
            .center(pos.copy(y = pos.y - NAMETAG_PADDING * ModuleNametags.scale))
            .scale(ModuleNametags.scale)
            .itemStackRenderer(currentItemStackRenderer)
            .rectBackground(color = 0)
            .draw(immediately = true)
    }

    val fontSize = FontManager.DEFAULT_FONT_SIZE

    val scale = 1f / (fontSize * 0.15f) * ModuleNametags.scale

    if (ModuleNametags.batchRenderMode == ModuleNametags.BatchRenderMode.EACH) startBatch()

    matrixStack.push()
    matrixStack.translate(pos.x, pos.y, 0f)
    matrixStack.scale(scale, scale, 1f)

    val fontRenderer = ModuleNametags.fontRenderer
    val processedText = fontRenderer.process(nametag.text)
    val textWidth = fontRenderer.getStringWidth(processedText, shadow = true)

    // Make the model view matrix center the text when rendering
    matrixStack.translate(-textWidth * 0.5f, -fontRenderer.height * 0.5f, 0f)

    val q1 = Vector2f(-0.1f * fontSize, fontRenderer.height * -0.1f)
    val q2 = Vector2f(textWidth + 0.2f * fontSize, fontRenderer.height * 1.1f)

    // Background
    drawQuad(
        q1, q2, z = 0f,
        fillColor = Color4b(Int.MIN_VALUE, hasAlpha = true),
        outlineColor = Color4b.BLACK.takeIf { NametagShowOptions.BORDER.isShowing() },
    )

    // Text
    fontRenderer.draw(
        processedText,
        x0 = 0f, y0 = 0f,
        shadow = true,
        z = 0.001f,
    )

    // Draw enchantments directly for the entity (regardless of whether items are shown)
    if (NametagShowOptions.ENCHANTMENTS.isShowing() && nametag.entity is LivingEntity) {
        val entityPos = nametag.entity.pos
        val worldX = entityPos.x.toFloat()
        val worldY = (entityPos.y + nametag.entity.height + 0.5f).toFloat()

        NametagEnchantmentRenderer.drawEntityEnchantments(
            nametag.entity,
            worldX,
            worldY,
        )
    }

    matrixStack.pop()

    if (ModuleNametags.batchRenderMode == ModuleNametags.BatchRenderMode.EACH) commitBatch()
}
