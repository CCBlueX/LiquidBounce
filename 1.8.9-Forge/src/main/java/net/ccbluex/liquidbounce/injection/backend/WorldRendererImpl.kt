/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.IWorldRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import net.minecraft.client.renderer.WorldRenderer

class WorldRendererImpl(val wrapped: WorldRenderer) : IWorldRenderer {
    override fun begin(mode: Int, vertexFormat: IVertexFormat) = wrapped.begin(mode, (vertexFormat as VertexFormatImpl).wrapped)

    override fun pos(x: Double, y: Double, z: Double): IWorldRenderer {
        wrapped.pos(x, y, z)

        return this
    }

    override fun endVertex() = wrapped.endVertex()

    override fun tex(u: Double, v: Double): IWorldRenderer {
        wrapped.tex(u, v)

        return this
    }
}