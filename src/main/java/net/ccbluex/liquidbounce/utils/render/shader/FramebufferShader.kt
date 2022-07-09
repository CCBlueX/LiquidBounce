/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.minecraft.client.gui.ScaledResolution
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.client.renderer.RenderHelper
import net.minecraft.client.shader.Framebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20

/**
 * @author TheSlowly
 */
abstract class FramebufferShader(fragmentShader: String) : Shader(fragmentShader)
{
    protected var red = 0f
    protected var green = 0f
    protected var blue = 0f
    protected var alpha = 1.0f
    protected var radius = 2.0f
    protected var quality = 1.0f

    private var entityShadows = false

    fun startDraw(partialTicks: Float)
    {
        GlStateManager.enableAlpha()
        GlStateManager.pushMatrix()
        GlStateManager.pushAttrib()

        framebuffer = setupFrameBuffer(framebuffer).apply {
            framebufferClear()
            bindFramebuffer(true)
        }

        entityShadows = mc.gameSettings.entityShadows

        mc.gameSettings.entityShadows = false
        mc.entityRenderer.setupCameraTransform(partialTicks, 0)
    }

    fun stopDraw(color: Int, radius: Float, quality: Float)
    {
        mc.gameSettings.entityShadows = entityShadows

        GL11.glEnable(GL11.GL_BLEND)
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

        mc.framebuffer?.bindFramebuffer(true)

        red = (color shr 16 and 0xFF) / 255.0F
        green = (color shr 8 and 0xFF) / 255.0F
        blue = (color and 0xFF) / 255.0F
        alpha = (color shr 24 and 0xFF) / 255.0F

        this.radius = radius
        this.quality = quality

        val entityRenderer = mc.entityRenderer

        entityRenderer.disableLightmap()
        RenderHelper.disableStandardItemLighting()

        startShader()

        entityRenderer.setupOverlayRendering()

        drawFramebuffer(framebuffer)

        stopShader()

        entityRenderer.disableLightmap()

        GlStateManager.popMatrix()
        GlStateManager.popAttrib()
    }

    companion object
    {
        private var framebuffer: Framebuffer? = null

        /**
         * @param  frameBuffer
         * @return             frameBuffer
         * @author             TheSlowly
         */
        fun setupFrameBuffer(frameBuffer: Framebuffer?): Framebuffer
        {
            frameBuffer?.deleteFramebuffer()
            return Framebuffer(mc.displayWidth, mc.displayHeight, true)
        }

        /**
         * @author TheSlowly
         */
        fun drawFramebuffer(framebuffer: Framebuffer?)
        {
            val scaledResolution = ScaledResolution(mc)
            GL11.glBindTexture(GL11.GL_TEXTURE_2D, (framebuffer ?: return).framebufferTexture)

            GL11.glBegin(GL11.GL_QUADS)
            GL11.glTexCoord2d(0.0, 1.0)
            GL11.glVertex2d(0.0, 0.0)
            GL11.glTexCoord2d(0.0, 0.0)
            GL11.glVertex2d(0.0, scaledResolution.scaledHeight.toDouble())
            GL11.glTexCoord2d(1.0, 0.0)
            GL11.glVertex2d(scaledResolution.scaledWidth.toDouble(), scaledResolution.scaledHeight.toDouble())
            GL11.glTexCoord2d(1.0, 1.0)
            GL11.glVertex2d(scaledResolution.scaledWidth.toDouble(), 0.0)
            GL11.glEnd()

            GL20.glUseProgram(0)
        }
    }
}
