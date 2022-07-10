/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.render

import net.minecraft.client.renderer.texture.TextureUtil
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

            if (field != -1) return field

            return TextureUtil.uploadTextureImageAllocate(TextureUtil.glGenTextures(), image, true, true).also { field = it }
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
    protected fun finalize()
    {
        unload()
    }
}
