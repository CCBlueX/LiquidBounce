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

package net.ccbluex.liquidbounce.render.utils

import net.ccbluex.liquidbounce.render.engine.Color4b
import net.ccbluex.liquidbounce.render.engine.Vec3
import net.ccbluex.liquidbounce.render.engine.memory.*
import net.minecraft.util.math.Box
import net.minecraft.util.math.Direction

fun drawBoxOutlineNew(box: Box, color: Color4b): Pair<VertexFormat, IndexBuffer> {
    val vertexFormat = PositionColorVertexFormat()

    vertexFormat.initBuffer(8)

    val indexBuffer = IndexBuffer(6 * 6, VertexFormatComponentDataType.GlUnsignedByte)

    val p0 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.maxZ); this.color = color }
    val p1 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.maxZ); this.color = color }
    val p2 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.maxZ); this.color = color }
    val p3 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.maxZ); this.color = color }

    val p4 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.minZ); this.color = color }
    val p5 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.minZ); this.color = color }
    val p6 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.minZ); this.color = color }
    val p7 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.minZ); this.color = color }

    indexBuffer.indexLine(p0, p1)
    indexBuffer.indexLine(p2, p3)
    indexBuffer.indexLine(p4, p5)
    indexBuffer.indexLine(p6, p7)

    indexBuffer.indexLine(p0, p4)
    indexBuffer.indexLine(p1, p5)
    indexBuffer.indexLine(p2, p6)
    indexBuffer.indexLine(p3, p7)

    indexBuffer.indexLine(p0, p2)
    indexBuffer.indexLine(p1, p3)
    indexBuffer.indexLine(p4, p6)
    indexBuffer.indexLine(p5, p7)

    return Pair(vertexFormat, indexBuffer)
}

fun drawBoxNew(box: Box, color: Color4b): Pair<VertexFormat, IndexBuffer> {
    val vertexFormat = PositionColorVertexFormat()

    vertexFormat.initBuffer(8)

    val indexBuffer = IndexBuffer(6 * 6, VertexFormatComponentDataType.GlUnsignedByte)

    val p0 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.maxZ); this.color = color }
    val p1 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.maxZ); this.color = color }
    val p2 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.maxZ); this.color = color }
    val p3 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.maxZ); this.color = color }

    val p4 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.minZ); this.color = color }
    val p5 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.minZ); this.color = color }
    val p6 = vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.minZ); this.color = color }
    val p7 = vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.minZ); this.color = color }

    indexBuffer.indexQuad(p2, p0, p1, p3)
    indexBuffer.indexQuad(p6, p4, p0, p2)
    indexBuffer.indexQuad(p7, p5, p4, p6)
    indexBuffer.indexQuad(p3, p1, p5, p7)
    indexBuffer.indexQuad(p6, p2, p3, p7)
    indexBuffer.indexQuad(p0, p4, p5, p1)

    return Pair(vertexFormat, indexBuffer)
}

fun drawBoxSide(box: Box, side: Direction, color: Color4b): Pair<VertexFormat, IndexBuffer> {
    val vertexFormat = PositionColorVertexFormat()

    vertexFormat.initBuffer(4)

    val indexBuffer = IndexBuffer(6, VertexFormatComponentDataType.GlUnsignedByte)

    if (side == Direction.SOUTH) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.maxZ); this.color = color },
    )
    if (side == Direction.WEST) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.maxZ); this.color = color },
    )
    if (side == Direction.NORTH) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.minZ); this.color = color },
    )
    if (side == Direction.EAST) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.minZ); this.color = color },
    )
    if (side == Direction.UP) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.maxY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.maxY, box.minZ); this.color = color },
    )
    if (side == Direction.DOWN) indexBuffer.indexQuad(
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.maxZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.minX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.minZ); this.color = color },
        vertexFormat.putVertex { this.position = Vec3(box.maxX, box.minY, box.maxZ); this.color = color },
    )

    return Pair(vertexFormat, indexBuffer)
}
