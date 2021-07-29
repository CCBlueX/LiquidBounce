/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

package net.ccbluex.liquidbounce.render.engine.memory

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.UV2s
import net.ccbluex.liquidbounce.render.engine.Vec3
import kotlin.reflect.KProperty

abstract class VertexFormat() {

    /**
     * How many vertices are in this buffer?
     */
    var elementCount: Int = 0
        private set

    /**
     * How long is each vertex in this buffer?
     */
    var length = 0
        private set

    lateinit var bufferBuilder: BufferBuilder

    val components = mutableListOf<VertexFormatComponent>()

    fun vec3(attributeType: AttributeType): DelegatedVertexFormatComponent<Vec3> {
        val component = VertexFormatComponent(VertexFormatComponentDataType.GlFloat, this.length, 3, false, AttributeInfo(attributeType))

        return DelegatedVertexFormatComponent(this, registerComponent(component))
    }

    fun uv(attributeType: AttributeType): DelegatedVertexFormatComponent<UV2s> {
        val component = VertexFormatComponent(VertexFormatComponentDataType.GlUnsignedShort, length, 2, true, AttributeInfo(attributeType))

        return DelegatedVertexFormatComponent(this, registerComponent(component))
    }

    fun color4b(attributeType: AttributeType): DelegatedVertexFormatComponent<Color4b> {
        val component = VertexFormatComponent(VertexFormatComponentDataType.GlUnsignedByte, this.length, 4, true, AttributeInfo(attributeType))

        return DelegatedVertexFormatComponent(this, registerComponent(component))
    }

    private fun registerComponent(vertexFormatComponent: VertexFormatComponent): VertexFormatComponent {
        this.components += vertexFormatComponent
        this.length += vertexFormatComponent.length

        return vertexFormatComponent
    }

    fun nextVertex() {
        this.bufferBuilder.pos += this.length
        this.elementCount++
    }

    fun initBuffer(capacity: Int) {
        this.bufferBuilder = BufferBuilder(capacity * this.length)
    }
}

inline fun <T : VertexFormat> T.putVertex(function: T.() -> Unit): Int {
    function()

    nextVertex()

    return this.elementCount - 1
}

class DelegatedVertexFormatComponent<T>(val vertexFormat: VertexFormat, val vertexFormatComponent: VertexFormatComponent) {

    operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
        throw IllegalStateException("This property shall not be read from!")
    }

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        val bufferBuilder = vertexFormat.bufferBuilder
        val offset = this.vertexFormatComponent.offset + bufferBuilder.pos

        when (this.vertexFormatComponent.type) {
            VertexFormatComponentDataType.GlByte -> bufferBuilder.buffer.put(offset, value as Byte)
            VertexFormatComponentDataType.GlUnsignedByte -> {
                when (vertexFormatComponent.count) {
                    4 -> (value as Color4b).writeToBuffer(offset, bufferBuilder.buffer)
                    1 -> bufferBuilder.buffer.put(offset, value as Byte)
                    else -> throw IllegalStateException()
                }
            }
            VertexFormatComponentDataType.GlShort -> bufferBuilder.buffer.putShort(offset, value as Short)
            VertexFormatComponentDataType.GlUnsignedShort -> {
                when (vertexFormatComponent.count) {
                    2 -> (value as UV2s).writeToBuffer(offset, bufferBuilder.buffer)
                    1 -> bufferBuilder.buffer.putShort(offset, value as Short)
                    else -> throw IllegalStateException()
                }
            }
            VertexFormatComponentDataType.GlInt -> bufferBuilder.buffer.putInt(offset, value as Int)
            VertexFormatComponentDataType.GlUnsignedInt -> bufferBuilder.buffer.putInt(offset, value as Int)
            VertexFormatComponentDataType.GlFloat -> {
                when (vertexFormatComponent.count) {
                    3 -> (value as Vec3).writeToBuffer(offset, bufferBuilder.buffer)
                    1 -> bufferBuilder.buffer.putFloat(offset, value as Float)
                    else -> throw IllegalStateException()
                }

            }
            VertexFormatComponentDataType.GlDouble -> bufferBuilder.buffer.putDouble(offset, value as Double)
            else -> throw IllegalStateException("Not implemented")
        }
    }
}
