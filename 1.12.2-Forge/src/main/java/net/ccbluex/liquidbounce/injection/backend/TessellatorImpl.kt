/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.ITessellator
import net.ccbluex.liquidbounce.api.minecraft.client.render.IWorldRenderer
import net.minecraft.client.renderer.Tessellator

class TessellatorImpl(val wrapped: Tessellator) : ITessellator {
    override val worldRenderer: IWorldRenderer
        get() = WorldRendererImpl(wrapped.buffer)

    override fun draw() = wrapped.draw()

    override fun equals(other: Any?): Boolean {
        return other is TessellatorImpl && other.wrapped == this.wrapped
    }

}