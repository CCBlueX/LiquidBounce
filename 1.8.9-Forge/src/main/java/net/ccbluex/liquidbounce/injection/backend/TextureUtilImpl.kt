package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.ITextureUtil
import net.minecraft.client.renderer.texture.TextureUtil
import java.awt.image.BufferedImage

object TextureUtilImpl : ITextureUtil
{
	override fun uploadTextureImageAllocate(textureId: Int, p_110989_1_: BufferedImage, p_110989_2_: Boolean, p_110989_3_: Boolean): Int = TextureUtil.uploadTextureImageAllocate(textureId, p_110989_1_, p_110989_2_, p_110989_3_)
	override fun glGenTextures(): Int = TextureUtil.glGenTextures()
}
