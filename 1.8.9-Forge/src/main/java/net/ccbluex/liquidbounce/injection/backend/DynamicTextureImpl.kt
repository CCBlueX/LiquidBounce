/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.IDynamicTexture
import net.minecraft.client.renderer.texture.DynamicTexture

class DynamicTextureImpl<T : DynamicTexture>(wrapped: T) : AbstractTextureImpl<T>(wrapped), IDynamicTexture

inline fun IDynamicTexture.unwrap(): DynamicTexture = (this as DynamicTextureImpl<*>).wrapped
inline fun DynamicTexture.wrap(): IDynamicTexture = DynamicTextureImpl(this)