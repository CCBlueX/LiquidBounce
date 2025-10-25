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

import com.mojang.blaze3d.platform.GlStateManager
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.ItemStackListRenderer.Companion.drawItemStackList
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.minecraft.entity.LivingEntity

private const val NAMETAG_PADDING: Int = 15

private val currentItemStackRenderer
    get() = if (NametagShowOptions.ITEM_INFO.isShowing()) {
        ItemStackListRenderer.SingleItemStackRenderer
    } else {
        ItemStackListRenderer.SingleItemStackRenderer.OnlyItem
    }

internal fun GUIRenderEnvironment.drawNametag(nametag: Nametag, pos: Vec3) {
    if (NametagShowOptions.ITEMS.isShowing()) {
        context.drawItemStackList(nametag.items)
            .center(pos.copy(y = pos.y - NAMETAG_PADDING * ModuleNametags.scale))
            .scale(ModuleNametags.scale)
            .itemStackRenderer(currentItemStackRenderer)
            .rectBackground(color = 0)
            .draw(immediately = true)
    }

    val fontSize = FontManager.DEFAULT_FONT_SIZE

    val scale = 1f / (fontSize * 0.15f) * ModuleNametags.scale

    matrixStack.push()
    matrixStack.translate(pos.x, pos.y, pos.z)
    matrixStack.scale(scale, scale, 1f)

    startBatch()

    GlStateManager._enableBlend()

    val x =
        ModuleNametags.fontRenderer.draw(
            ModuleNametags.fontRenderer.process(nametag.text),
            0f,
            0f,
            shadow = true,
            z = 0.001f,
        )

    // Make the model view matrix center the text when rendering
    matrixStack.translate(-x * 0.5f, -ModuleNametags.fontRenderer.height * 0.5f, 0f)

    val q1 = Vec3(-0.1f * fontSize, ModuleNametags.fontRenderer.height * -0.1f, 0f)
    val q2 = Vec3(x + 0.2f * fontSize, ModuleNametags.fontRenderer.height * 1.1f, 0f)

    drawColoredQuad(q1, q2, Int.MIN_VALUE)

    if (NametagShowOptions.BORDER.isShowing()) {
        drawColoredQuadOutlines(q1, q2, Color4b.BLACK.toARGB())
    }

    // Draw enchantments directly for the entity (regardless of whether items are shown)
    if (NametagShowOptions.ENCHANTMENTS.isShowing() && nametag.entity is LivingEntity) {
        val entityPos = nametag.entity.pos
        val worldX = entityPos.x.toFloat()
        val worldY = (entityPos.y + nametag.entity.height + 0.5f).toFloat()

        NametagEnchantmentRenderer.drawEntityEnchantments(
            this@drawNametag,
            nametag.entity,
            worldX,
            worldY,
        )
    }

    commitBatch()

    ModuleNametags.fontRenderer.commit(this@drawNametag)

    GlStateManager._disableBlend()

    matrixStack.pop()
}
