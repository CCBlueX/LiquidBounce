/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.texture.IAbstractTexture
import net.minecraft.client.renderer.texture.AbstractTexture

open class AbstractTextureImpl<T : AbstractTexture>(val wrapped: T) : IAbstractTexture {
    override fun equals(other: Any?): Boolean {
        return other is AbstractTextureImpl<*> && other.wrapped == this.wrapped
    }
}

inline fun IAbstractTexture.unwrap(): AbstractTexture = (this as AbstractTextureImpl<*>).wrapped
inline fun AbstractTexture.wrap(): IAbstractTexture = AbstractTextureImpl(this)