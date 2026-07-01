/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

package net.ccbluex.liquidbounce.render.utils

import it.unimi.dsi.fastutil.floats.FloatArrayList
import net.ccbluex.liquidbounce.render.engine.type.Vec3f
import net.minecraft.client.Camera
import net.minecraft.world.phys.Vec3
import org.joml.Vector3f

interface VertexList {

    val size: Int

    fun x(index: Int): Float

    fun y(index: Int): Float

    fun z(index: Int): Float

    fun vec(index: Int, dest: Vector3f = Vector3f()): Vector3f =
        dest.set(x(index), y(index), z(index))
}

inline fun VertexList.forEachVertex(action: (x: Float, y: Float, z: Float) -> Unit) {
    for (index in 0 until size) {
        action(x(index), y(index), z(index))
    }
}

fun VertexList.lineStripAsLines(): VertexList = LineStripAsLinesVertexView(this)

@Suppress("TooManyFunctions")
class MutableVertexList(initialVertexCapacity: Int = 0) : VertexList {

    private val values = FloatArrayList(initialVertexCapacity * ELEMENTS_PER_VERTEX)

    override val size: Int
        get() = values.size / ELEMENTS_PER_VERTEX

    override fun x(index: Int): Float = values.getFloat(index.toValueIndex())

    override fun y(index: Int): Float = values.getFloat(index.toValueIndex() + 1)

    override fun z(index: Int): Float = values.getFloat(index.toValueIndex() + 2)

    fun add(x: Float, y: Float, z: Float): MutableVertexList {
        values.add(x)
        values.add(y)
        values.add(z)
        return this
    }

    fun add(vec: Vec3f): MutableVertexList = add(vec.x, vec.y, vec.z)

    fun add(vec: Vec3): MutableVertexList = add(vec.x.toFloat(), vec.y.toFloat(), vec.z.toFloat())

    fun addAll(vertices: Iterable<Vec3>): MutableVertexList {
        vertices.forEach(this::add)
        return this
    }

    fun addRelative(vec: Vec3, origin: Vec3): MutableVertexList = add(
        (vec.x - origin.x).toFloat(),
        (vec.y - origin.y).toFloat(),
        (vec.z - origin.z).toFloat(),
    )

    fun addAllRelative(vertices: Iterable<Vec3>, origin: Vec3): MutableVertexList {
        vertices.forEach { addRelative(it, origin) }
        return this
    }

    fun addRelativeToCamera(vec: Vec3, camera: Camera): MutableVertexList {
        val cameraPos = camera.position()
        return addRelative(vec, cameraPos)
    }

    fun addAllRelativeToCamera(vertices: Iterable<Vec3>, camera: Camera): MutableVertexList {
        vertices.forEach { addRelativeToCamera(it, camera) }
        return this
    }

    inline fun <T> addAll(vertices: Iterable<T>, vertexMapper: (T) -> Vec3): MutableVertexList {
        vertices.forEach { add(vertexMapper(it)) }
        return this
    }

    inline fun <T> addAllRelative(
        vertices: Iterable<T>,
        origin: Vec3,
        vertexMapper: (T) -> Vec3,
    ): MutableVertexList {
        vertices.forEach { addRelative(vertexMapper(it), origin) }
        return this
    }

    inline fun <T> addAllRelativeToCamera(
        vertices: Iterable<T>,
        camera: Camera,
        vertexMapper: (T) -> Vec3,
    ): MutableVertexList {
        vertices.forEach { addRelativeToCamera(vertexMapper(it), camera) }
        return this
    }

    private fun Int.toValueIndex(): Int {
        if (this !in 0 until size) {
            throw IndexOutOfBoundsException("Index $this out of bounds [0, $size)")
        }

        return this * ELEMENTS_PER_VERTEX
    }

    private companion object {
        const val ELEMENTS_PER_VERTEX = 3
    }
}

private class LineStripAsLinesVertexView(private val source: VertexList) : VertexList {

    override val size: Int
        get() = if (source.size < 2) 0 else (source.size - 1) shl 1

    override fun x(index: Int): Float = source.x(index.toSourceIndex())

    override fun y(index: Int): Float = source.y(index.toSourceIndex())

    override fun z(index: Int): Float = source.z(index.toSourceIndex())

    private fun Int.toSourceIndex(): Int {
        if (this !in 0 until size) {
            throw IndexOutOfBoundsException("Index $this out of bounds [0, $size)")
        }

        return (this shr 1) + (this and 1)
    }
}
