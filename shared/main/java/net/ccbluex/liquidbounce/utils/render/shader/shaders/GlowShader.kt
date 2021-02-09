/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render.shader.shaders

import net.ccbluex.liquidbounce.utils.render.shader.FramebufferShader
import org.lwjgl.opengl.GL20

class GlowShader : FramebufferShader("glow.frag")
{
	override fun setupUniforms()
	{
		setupUniform("texture")
		setupUniform("texelSize")
		setupUniform("color")
		setupUniform("divider")
		setupUniform("radius")
		setupUniform("maxSample")
	}

	override fun updateUniforms()
	{
		GL20.glUniform1i(getUniform("texture"), 0)
		GL20.glUniform2f(getUniform("texelSize"), 1.0f / mc.displayWidth * (radius * quality), 1.0f / mc.displayHeight * (radius * quality))
		GL20.glUniform3f(getUniform("color"), red, green, blue)
		GL20.glUniform1f(getUniform("divider"), 140.0f)
		GL20.glUniform1f(getUniform("radius"), radius)
		GL20.glUniform1f(getUniform("maxSample"), 10.0f)
	}

	companion object
	{
		val INSTANCE = GlowShader()
	}
}
