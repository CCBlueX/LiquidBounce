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

package net.ccbluex.liquidbounce.utils.render

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.memory.IndexBuffer
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.putVertex

typealias VertexInfoRetriever<T> = T.() -> Unit

inline fun <T : VertexFormat> T.quadOutline(indexBuffer: IndexBuffer, p1: VertexInfoRetriever<T>, p2: VertexInfoRetriever<T>, p3: VertexInfoRetriever<T>, p4: VertexInfoRetriever<T>) {
    val v1 = this.putVertex(p1)
    val v2 = this.putVertex(p2)
    val v3 = this.putVertex(p3)
    val v4 = this.putVertex(p4)

    indexBuffer.index(v1)
    indexBuffer.index(v2)
    indexBuffer.index(v2)
    indexBuffer.index(v3)
    indexBuffer.index(v3)
    indexBuffer.index(v4)
    indexBuffer.index(v4)
    indexBuffer.index(v1)
}

inline fun <T : VertexFormat> T.quad(indexBuffer: IndexBuffer, p1: VertexInfoRetriever<T>, p2: VertexInfoRetriever<T>, p3: VertexInfoRetriever<T>, p4: VertexInfoRetriever<T>) {
    val v1 = this.putVertex(p1)
    val v2 = this.putVertex(p2)
    val v3 = this.putVertex(p3)
    val v4 = this.putVertex(p4)

    indexBuffer.index(v1)
    indexBuffer.index(v2)
    indexBuffer.index(v4)
    indexBuffer.index(v4)
    indexBuffer.index(v2)
    indexBuffer.index(v3)
}

fun PositionColorVertexFormat.rect(indexBuffer: IndexBuffer, p1: Vec3, p2: Vec3, color: Color4b, outline: Boolean = false) {
    if (outline) {
        this.quadOutline(
            indexBuffer,
            { this.position = Vec3(p1.x, p1.y, p1.z); this.color = color },
            { this.position = Vec3(p1.x, p2.y, p1.z); this.color = color },
            { this.position = Vec3(p2.x, p2.y, p1.z); this.color = color },
            { this.position = Vec3(p2.x, p1.y, p1.z); this.color = color }
        )
    } else {
        this.quad(
            indexBuffer,
            { this.position = Vec3(p1.x, p1.y, p1.z); this.color = color },
            { this.position = Vec3(p1.x, p2.y, p1.z); this.color = color },
            { this.position = Vec3(p2.x, p2.y, p1.z); this.color = color },
            { this.position = Vec3(p2.x, p1.y, p1.z); this.color = color }
        )
    }
}
