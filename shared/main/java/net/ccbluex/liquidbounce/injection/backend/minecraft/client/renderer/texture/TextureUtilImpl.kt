package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer.texture

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.ITextureUtil
import net.minecraft.client.renderer.texture.TextureUtil
import java.awt.image.BufferedImage

object TextureUtilImpl : ITextureUtil
{
    override fun uploadTextureImageAllocate(textureId: Int, texture: BufferedImage, textureBlur: Boolean, textureClamp: Boolean): Int = TextureUtil.uploadTextureImageAllocate(textureId, texture, textureBlur, textureClamp)
    override fun glGenTextures(): Int = TextureUtil.glGenTextures()
}
