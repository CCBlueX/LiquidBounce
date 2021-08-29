/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import javax.imageio.ImageIO

object IconUtils
{
	@JvmStatic
	val favicon: Array<ByteBuffer?>?
		get() = try
		{
			arrayOf(readImageToBuffer(IconUtils::class.java.getResourceAsStream("/assets/minecraft/" + LiquidBounce.CLIENT_NAME.toLowerCase() + "/icon_16x16.png")), readImageToBuffer(IconUtils::class.java.getResourceAsStream("/assets/minecraft/" + LiquidBounce.CLIENT_NAME.toLowerCase() + "/icon_32x32.png")))
		}
		catch (e: IOException)
		{
			logger.error("Can't load the favicon from assets", e)
			null
		}

	@Throws(IOException::class)
	private fun readImageToBuffer(imageStream: InputStream?): ByteBuffer?
	{
		val bufferedImage = ImageIO.read(imageStream ?: return null)
		val rgb = bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, null, 0, bufferedImage.width)
		val byteBuffer = ByteBuffer.allocate(4 * rgb.size)

		for (i in rgb) byteBuffer.putInt(i shl 8 or (i shr 24 and 255))
		byteBuffer.flip()
		return byteBuffer
	}
}
