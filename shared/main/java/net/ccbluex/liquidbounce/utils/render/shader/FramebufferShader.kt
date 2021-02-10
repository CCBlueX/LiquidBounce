/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader

import net.ccbluex.liquidbounce.api.minecraft.client.shader.IFramebuffer
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL20
import java.awt.Color

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
		classProvider.glStateManager.enableAlpha()
		classProvider.glStateManager.pushMatrix()
		classProvider.glStateManager.pushAttrib()

		framebuffer = setupFrameBuffer(framebuffer).apply {
			framebufferClear()
			bindFramebuffer(true)
		}

		entityShadows = mc.gameSettings.entityShadows

		mc.gameSettings.entityShadows = false
		mc.entityRenderer.setupCameraTransform(partialTicks, 0)
	}

	fun stopDraw(color: Color, radius: Float, quality: Float)
	{
		mc.gameSettings.entityShadows = entityShadows

		GL11.glEnable(GL11.GL_BLEND)
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA)

		mc.framebuffer?.bindFramebuffer(true)

		red = color.red / 255.0f
		green = color.green / 255.0f
		blue = color.blue / 255.0f
		alpha = color.alpha / 255.0f

		this.radius = radius
		this.quality = quality

		mc.entityRenderer.disableLightmap()
		functions.disableStandardItemLighting()

		startShader()

		mc.entityRenderer.setupOverlayRendering()

		drawFramebuffer(framebuffer)

		stopShader()

		mc.entityRenderer.disableLightmap()
		classProvider.glStateManager.popMatrix()
		classProvider.glStateManager.popAttrib()
	}

	companion object
	{
		private var framebuffer: IFramebuffer? = null

		/**
		 * @param  frameBuffer
		 * @return             frameBuffer
		 * @author             TheSlowly
		 */
		fun setupFrameBuffer(frameBuffer: IFramebuffer?): IFramebuffer
		{
			frameBuffer?.deleteFramebuffer()
			return classProvider.createFramebuffer(mc.displayWidth, mc.displayHeight, true)
		}

		/**
		 * @author TheSlowly
		 */
		fun drawFramebuffer(framebuffer: IFramebuffer?)
		{
			val scaledResolution = classProvider.createScaledResolution(mc)
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
