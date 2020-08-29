/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.renderer.vertex

import java.nio.ByteBuffer

interface IVertexBuffer {
    fun deleteGlBuffers()
    fun bindBuffer()
    fun drawArrays(mode: Int)
    fun unbindBuffer()
    fun bufferData(buffer: ByteBuffer)
}