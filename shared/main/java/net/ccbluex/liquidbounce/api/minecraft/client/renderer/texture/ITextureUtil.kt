package net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture

import java.awt.image.BufferedImage

interface ITextureUtil
{
    fun uploadTextureImageAllocate(textureId: Int, texture: BufferedImage, textureBlur: Boolean, textureClamp: Boolean): Int
    fun glGenTextures(): Int
}
