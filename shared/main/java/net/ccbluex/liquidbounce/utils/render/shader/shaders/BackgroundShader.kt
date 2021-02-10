/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.render.shader.Shader
import org.lwjgl.opengl.Display
import org.lwjgl.opengl.GL20

class BackgroundShader : Shader("background.frag")
{
	private var time = 0f

	override fun setupUniforms()
	{
		setupUniform("iResolution")
		setupUniform("iTime")
	}

	override fun updateUniforms()
	{
		val resolutionID = getUniform("iResolution")

		if (resolutionID > -1)
		{
			val displayWidth = Display.getWidth().toFloat()
			val displayHeight = Display.getHeight().toFloat()

			GL20.glUniform2f(resolutionID, displayWidth, displayHeight)
		}

		val timeID = getUniform("iTime")

		if (timeID > -1) GL20.glUniform1f(timeID, time)

		time += 0.003f * RenderUtils.deltaTime
	}

	companion object
	{
		@JvmField
		val INSTANCE = BackgroundShader()
	}
}
