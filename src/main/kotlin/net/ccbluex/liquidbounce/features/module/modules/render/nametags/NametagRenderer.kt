package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import net.ccbluex.liquidbounce.render.RenderBufferBuilder
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.VertexInputType
import net.ccbluex.liquidbounce.render.drawQuad
import net.ccbluex.liquidbounce.render.drawQuadOutlines
import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.withColor
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.render.Camera
import net.minecraft.client.render.VertexFormat
import net.minecraft.util.math.RotationAxis
import org.lwjgl.opengl.GL11

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
        text: String,
        pos: Vec3,
    ) {
        val camera = mc.gameRenderer.camera
        val scale = calculateScale(camera, pos)

        val c = 43.0F

        env.matrixStack.push()
        env.matrixStack.translate(pos.x, pos.y, pos.z)
        env.matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-(camera.yaw)))
        env.matrixStack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.pitch))
        env.matrixStack.scale(-scale, -scale, -scale)

        val x =
            ModuleNametags.fontRenderer.draw(
                text,
                0.0F,
                0.0F,
                shadow = true,
                z = 0.1F,
            )
        env.matrixStack.translate(-x * 0.5F, -ModuleNametags.fontRenderer.height * 0.5F, 0.00F)

        ModuleNametags.fontRenderer.commit(env, fontBuffers)

        val q1 = Vec3(-0.0F * c, ModuleNametags.fontRenderer.height * -0.01F, 0.0F)
        val q2 = Vec3(x + 0.1F * c, ModuleNametags.fontRenderer.height * 1.01F, 0.0F)

        quadBuffers.drawQuad(env, q1, q2)

        if (ModuleNametags.border) {
            lineBuffers.drawQuadOutlines(env, q1, q2)
        }

        env.matrixStack.pop()
    }

    private fun calculateScale(
        camera: Camera,
        pos: Vec3,
    ): Float {
        val cameraDistance = camera.pos.distanceTo(pos.toVec3d())

        val baseScale = (cameraDistance / 4F).coerceAtLeast(1.0).toFloat() / 450.0F

        return baseScale * ModuleNametags.scale
    }

    fun commit(env: RenderEnvironment) {
        GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT)
        GL11.glEnable(GL11.GL_DEPTH_TEST)

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
