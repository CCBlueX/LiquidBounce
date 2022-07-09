/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.renderer

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.IRenderGlobal
import net.minecraft.client.renderer.RenderGlobal

class RenderGlobalImpl(val wrapped: RenderGlobal) : IRenderGlobal
{
    override fun loadRenderers() = wrapped.loadRenderers()

    override fun equals(other: Any?): Boolean = other is RenderGlobalImpl && other.wrapped == wrapped
}

fun IRenderGlobal.unwrap(): RenderGlobal = (this as RenderGlobalImpl).wrapped
fun RenderGlobal.wrap(): IRenderGlobal = RenderGlobalImpl(this)
