/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.LiquidBounce
import org.lwjgl.opengl.GL11
import java.awt.image.BufferedImage

class CustomTexture(private val image: BufferedImage)
{
	private var unloaded = false

	/**
	 * @return                       ID of this texture loaded into memory
	 * @throws IllegalStateException
	 * If the texture was unloaded via [.unload]
	 */
	var textureId = -1
		get()
		{
			check(!unloaded) { "Texture unloaded" }
			val textureUtil = LiquidBounce.wrapper.classProvider.textureUtil
			if (field == -1) field = textureUtil.uploadTextureImageAllocate(textureUtil.glGenTextures(), image, textureBlur = true, textureClamp = true)
			return field
		}
		private set

	private fun unload()
	{
		if (!unloaded)
		{
			GL11.glDeleteTextures(textureId)
			unloaded = true
		}
	}

	@Throws(Throwable::class)
	protected fun finalize() // TODO: Check it really working right as Java's
	{
		unload()
	}
}
