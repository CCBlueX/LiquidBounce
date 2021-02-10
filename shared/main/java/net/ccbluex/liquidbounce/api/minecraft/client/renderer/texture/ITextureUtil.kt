package net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture

import java.awt.image.BufferedImage

interface ITextureUtil
{
	fun uploadTextureImageAllocate(textureId: Int, p_110989_1_: BufferedImage, p_110989_2_: Boolean, p_110989_3_: Boolean): Int
	fun glGenTextures(): Int
}
