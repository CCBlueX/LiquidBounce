/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.render.IWorldRenderer
import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import net.minecraft.client.renderer.BufferBuilder
import java.nio.ByteBuffer

class WorldRendererImpl(val wrapped: BufferBuilder) : IWorldRenderer {
    override val byteBuffer: ByteBuffer
        get() = wrapped.byteBuffer
    override val vertexFormat: IVertexFormat
        get() = wrapped.vertexFormat.wrap()

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

    override fun color(red: Float, green: Float, blue: Float, alpha: Float): IWorldRenderer {
        wrapped.color(red, green, blue, alpha)

        return this
    }

    override fun finishDrawing() = wrapped.finishDrawing()

    override fun reset() = wrapped.reset()

    override fun equals(other: Any?): Boolean {
        return other is WorldRendererImpl && other.wrapped == this.wrapped
    }
}