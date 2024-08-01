/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.DrawContext
import net.minecraft.client.render.VertexFormat
import net.minecraft.item.ItemStack
import org.lwjgl.opengl.GL11

private const val NAMETAG_PADDING: Int = 5
private const val ITEM_SIZE: Int = 20
private const val ITEM_SCALE: Float = 1.0F

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

    fun drawNametag(
        env: RenderEnvironment,
        info: NametagInfo,
        pos: Vec3,
    ) {
        val c = Fonts.DEFAULT_FONT_SIZE.toFloat()

        val scale = 1.0F / (c * 0.15F) * ModuleNametags.scale

        env.matrixStack.push()
        env.matrixStack.translate(pos.x, pos.y, pos.z)
        env.matrixStack.scale(scale, scale, 1.0F)

        val x =
            ModuleNametags.fontRenderer.draw(
                info.text,
                0.0F,
                0.0F,
                shadow = true,
                z = 0.001F,
            )

        // Make the model view matrix center the text when rendering
        env.matrixStack.translate(-x * 0.5F, -ModuleNametags.fontRenderer.height * 0.5F, 0.00F)

        ModuleNametags.fontRenderer.commit(env, fontBuffers)

        val q1 = Vec3(-0.1F * c, ModuleNametags.fontRenderer.height * -0.1F, 0.0F)
        val q2 = Vec3(x + 0.2F * c, ModuleNametags.fontRenderer.height * 1.1F, 0.0F)

        quadBuffers.drawQuad(env, q1, q2)

        if (ModuleNametags.border) {
            lineBuffers.drawQuadOutlines(env, q1, q2)
        }

        if (ModuleNametags.items) {
            drawItemList(pos, info.items)
        }

        env.matrixStack.pop()
    }

    private fun drawItemList(
        pos: Vec3,
        itemsToRender: List<ItemStack?>,
    ) {
        val dc = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)

        dc.matrices.translate(pos.x, pos.y - NAMETAG_PADDING, pos.z)
        dc.matrices.scale(ITEM_SCALE * ModuleNametags.scale, ITEM_SCALE * ModuleNametags.scale, 1.0F)
        dc.matrices.translate(-itemsToRender.size * ITEM_SIZE / 2.0F, -ITEM_SIZE.toFloat(), 0.0F)

        itemsToRender.forEachIndexed { index, itemStack ->
            dc.drawItem(itemStack, index * ITEM_SIZE, 0)
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
            fontBuffers.draw(ModuleNametags.fontRenderer)
        }
    }
}
