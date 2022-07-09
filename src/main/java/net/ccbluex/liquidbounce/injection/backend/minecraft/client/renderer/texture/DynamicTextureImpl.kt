/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer.texture

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.IDynamicTexture
import net.minecraft.client.renderer.texture.DynamicTexture

class DynamicTextureImpl<out T : DynamicTexture>(wrapped: T) : AbstractTextureImpl<T>(wrapped), IDynamicTexture
{
	override val textureData: IntArray
		get() = wrapped.textureData

	override fun updateDynamicTexture() = wrapped.updateDynamicTexture()
}

fun IDynamicTexture.unwrap(): DynamicTexture = (this as DynamicTextureImpl<*>).wrapped
fun DynamicTexture.wrap(): IDynamicTexture = DynamicTextureImpl(this)
