/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer.texture

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.texture.IAbstractTexture
import net.minecraft.client.renderer.texture.AbstractTexture

open class AbstractTextureImpl<out T : AbstractTexture>(val wrapped: T) : IAbstractTexture
{
    override val glTextureId: Int
        get() = wrapped.glTextureId

    override fun deleteGlTexture() = wrapped.deleteGlTexture()

    override fun equals(other: Any?): Boolean = other is AbstractTextureImpl<*> && other.wrapped == wrapped
}

fun IAbstractTexture.unwrap(): AbstractTexture = (this as AbstractTextureImpl<*>).wrapped
fun AbstractTexture.wrap(): IAbstractTexture = AbstractTextureImpl(this)
