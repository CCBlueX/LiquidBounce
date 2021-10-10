/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer.texture

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.IAbstractTexture
import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.ITextureManager
import net.ccbluex.liquidbounce.api.minecraft.util.IResourceLocation
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.unwrap
import net.minecraft.client.renderer.texture.TextureManager

class TextureManagerImpl(val wrapped: TextureManager) : ITextureManager
{
	override fun loadTexture(textureLocation: IResourceLocation, textureObj: IAbstractTexture): Boolean = wrapped.loadTexture(textureLocation.unwrap(), textureObj.unwrap())

	override fun bindTexture(image: IResourceLocation) = wrapped.bindTexture(image.unwrap())

	override fun equals(other: Any?): Boolean = other is TextureManagerImpl && other.wrapped == wrapped
}

fun ITextureManager.unwrap(): TextureManager = (this as TextureManagerImpl).wrapped
fun TextureManager.wrap(): ITextureManager = TextureManagerImpl(this)
