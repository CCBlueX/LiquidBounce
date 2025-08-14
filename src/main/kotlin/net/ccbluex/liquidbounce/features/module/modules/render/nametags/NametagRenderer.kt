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

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.*
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Vec3
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.render.VertexFormat
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

private const val NAMETAG_PADDING: Int = 5
private const val ITEM_SIZE: Int = 20
private const val ITEM_SCALE: Float = 1.0F

@Suppress("MagicNumber")
class NametagRenderer {

    private val quadBuffers =
        RenderBufferBuilder(
            VertexFormat.DrawMode.QUADS,
            VertexInputType.Pos,
            RenderBufferBuilder.TESSELATOR_A,
        )
    private val lineBuffers =
        RenderBufferBuilder(
            VertexFormat.DrawMode.DEBUG_LINES,
            VertexInputType.Pos,
            RenderBufferBuilder.TESSELATOR_B,
        )

    private val fontBuffers = FontRendererBuffers()

    fun drawNametag(env: RenderEnvironment, nametag: Nametag, pos: Vec3) = with(env) {
        val fontSize = FontManager.DEFAULT_FONT_SIZE

        val scale = 1f / (fontSize * 0.15f) * ModuleNametags.scale

        matrixStack.push()
        matrixStack.translate(pos.x, pos.y, pos.z)
        matrixStack.scale(scale, scale, 1f)

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

        ModuleNametags.fontRenderer.commit(env, fontBuffers)

        val q1 = Vec3(-0.1f * fontSize, ModuleNametags.fontRenderer.height * -0.1f, 0f)
        val q2 = Vec3(x + 0.2f * fontSize, ModuleNametags.fontRenderer.height * 1.1f, 0f)

        quadBuffers.drawQuad(env, q1, q2)

        if (NametagShowOptions.BORDER.isShowing()) {
            lineBuffers.drawQuadOutlines(env, q1, q2)
        }

        if (NametagShowOptions.ITEMS.isShowing()) {
            drawItemList(pos, nametag.items)
        }

        // Draw enchantments directly for the entity (regardless of whether items are shown)
        if (NametagShowOptions.ENCHANTMENTS.isShowing() && nametag.entity is LivingEntity) {
            val entityPos = nametag.entity.pos
            val worldX = entityPos.x.toFloat()
            val worldY = (entityPos.y + nametag.entity.height + 0.5f).toFloat()

            NametagEnchantmentRenderer.drawEntityEnchantments(
                env,
                nametag.entity,
                worldX,
                worldY,
                fontBuffers
            )
        }

        matrixStack.pop()
    }

    private fun drawItemList(pos: Vec3, itemsToRender: List<ItemStack?>) {
        val dc = newDrawContext()

        dc.matrices.translate(pos.x, pos.y - NAMETAG_PADDING, pos.z)
        dc.matrices.scale(ITEM_SCALE * ModuleNametags.scale, ITEM_SCALE * ModuleNametags.scale, 1.0F)
        dc.matrices.translate(-itemsToRender.size * ITEM_SIZE / 2.0F, -ITEM_SIZE.toFloat(), 0.0F)

        dc.fill(
            0,
            0,
            itemsToRender.size * ITEM_SIZE,
            ITEM_SIZE,
            Color4b.BLACK.with(a = 0).toARGB()
        )

        dc.matrices.translate(0.0F, 0.0F, 100.0F)

        val itemInfo = NametagShowOptions.ITEM_INFO.isShowing()

        itemsToRender.forEachIndexed { index, itemStack ->
            itemStack ?: return@forEachIndexed

            val x = index * ITEM_SIZE
            dc.drawItem(itemStack, x, 0)
            if (itemInfo) {
                dc.drawStackOverlay(mc.textRenderer, itemStack, x, 0)
            }
        }
    }

    fun commit(env: RenderEnvironment) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        GL11.glEnable(GL11.GL_DEPTH_TEST)

        RenderSystem.enableBlend()
        RenderSystem.blendFuncSeparate(
            GL11.GL_SRC_ALPHA,
            GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE,
            GL11.GL_ZERO
        )

        env.withColor(Color4b(0, 0, 0, 120)) {
            quadBuffers.draw()
        }
        env.withColor(Color4b(0, 0, 0, 255)) {
            lineBuffers.draw()
        }
        env.withColor(Color4b.WHITE) {
            fontBuffers.draw()
        }
    }
}
