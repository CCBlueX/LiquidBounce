/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.client.render

import net.ccbluex.liquidbounce.api.minecraft.client.render.vertex.IVertexFormat
import java.nio.ByteBuffer

interface IWorldRenderer {
    val byteBuffer: ByteBuffer
    val vertexFormat: IVertexFormat

    fun begin(mode: Int, vertexFormat: IVertexFormat)
    fun pos(x: Double, y: Double, z: Double): IWorldRenderer
    fun endVertex()
    fun tex(u: Double, v: Double): IWorldRenderer
    fun color(red: Float, green: Float, blue: Float, alpha: Float): IWorldRenderer
    fun finishDrawing()
    fun reset()
}