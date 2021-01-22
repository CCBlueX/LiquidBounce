/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.shader.IFramebuffer
import net.minecraft.client.shader.Framebuffer

class FramebufferImpl(val wrapped: Framebuffer) : IFramebuffer {
    override fun bindFramebuffer(b: Boolean) = wrapped.bindFramebuffer(b)

    override fun equals(other: Any?): Boolean {
        return other is FramebufferImpl && other.wrapped == this.wrapped
    }
}

 fun IFramebuffer.unwrap(): Framebuffer = (this as FramebufferImpl).wrapped
 fun Framebuffer.wrap(): IFramebuffer = FramebufferImpl(this)
