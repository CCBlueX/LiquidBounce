/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.renderer.vertex.IVertexBuffer
import net.minecraft.client.renderer.vertex.VertexBuffer
import java.nio.ByteBuffer

class VertexBufferImpl(val wrapped: VertexBuffer) : IVertexBuffer {
    override fun deleteGlBuffers() = wrapped.deleteGlBuffers()

    override fun bindBuffer() = wrapped.bindBuffer()

    override fun drawArrays(mode: Int) = wrapped.drawArrays(mode)

    override fun unbindBuffer() = wrapped.unbindBuffer()

    override fun bufferData(buffer: ByteBuffer) = wrapped.bufferData(buffer)

    override fun equals(other: Any?): Boolean {
        return other is VertexBufferImpl && other.wrapped == this.wrapped
    }
}

inline fun IVertexBuffer.unwrap(): VertexBuffer = (this as VertexBufferImpl).wrapped
inline fun VertexBuffer.wrap(): IVertexBuffer = VertexBufferImpl(this)