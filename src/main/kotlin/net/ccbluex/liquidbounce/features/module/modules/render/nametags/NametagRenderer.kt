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

        val scale = 1.0F / (c * 0.1F)

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

        drawItemList(pos, info.items)

        env.matrixStack.pop()
    }

    private fun drawItemList(
        pos: Vec3,
        itemsToRender: List<ItemStack?>,
    ) {
        val dc = DrawContext(mc, mc.bufferBuilders.entityVertexConsumers)

        dc.matrices.translate(pos.x, pos.y - NAMETAG_PADDING, pos.z)
        dc.matrices.scale(ITEM_SCALE, ITEM_SCALE, 1.0F)
        dc.matrices.translate(-itemsToRender.size * ITEM_SIZE / 2.0F, -ITEM_SIZE.toFloat(), 0.0F)

        itemsToRender.forEachIndexed { index, itemStack ->
            dc.drawItem(itemStack, index * ITEM_SIZE, 0)
        }
    }

    fun commit(env: RenderEnvironment) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        GL11.glEnable(GL11.GL_DEPTH_TEST)

        RenderSystem.enableBlend()
        env.withColor(Color4b(0, 0, 0, 127)) {
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
