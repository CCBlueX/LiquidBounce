/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_NAME
import net.minecraftforge.fml.relauncher.Side
import net.minecraftforge.fml.relauncher.SideOnly
import java.io.IOException
import java.io.InputStream
import java.nio.ByteBuffer
import java.util.*
import javax.imageio.ImageIO

@SideOnly(Side.CLIENT)
object IconUtils {

    fun getFavicon() =
        IconUtils::class.java.runCatching {
            arrayOf(
                readImageToBuffer(getResourceAsStream("/assets/minecraft/${CLIENT_NAME.lowercase()}/icon_16x16.png")),
                readImageToBuffer(getResourceAsStream("/assets/minecraft/${CLIENT_NAME.lowercase()}/icon_32x32.png")),
                readImageToBuffer(getResourceAsStream("/assets/minecraft/${CLIENT_NAME.lowercase()}/icon_64x64.png"))
            )
        }.onFailure {
            it.printStackTrace()
        }.getOrNull()

    @Throws(IOException::class)
    private fun readImageToBuffer(imageStream: InputStream?): ByteBuffer? {
        val bufferedImage = ImageIO.read(imageStream ?: return null)
        val rgb = bufferedImage.getRGB(0, 0, bufferedImage.width, bufferedImage.height, null, 0, bufferedImage.width)
        val byteBuffer = ByteBuffer.allocate(4 * rgb.size)

        for (i in rgb)
            byteBuffer.putInt(i shl 8 or (i shr 24 and 255))

        byteBuffer.flip()
        return byteBuffer
    }
}
