/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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

package net.ccbluex.liquidbounce.render.engine.utils

import net.ccbluex.liquidbounce.render.engine.Color4b
import org.lwjgl.opengl.GL11
import java.nio.ByteBuffer

fun Color4b.imSetColor() {
    GL11.glColor4f(
        this.r / 255.0f,
        this.g / 255.0f,
        this.b / 255.0f,
        this.a / 255.0f,
    )
}

fun imSetColorFromBuffer(vertexBuffer: ByteBuffer, idx: Int) {
    GL11.glColor4f(
        (vertexBuffer.get(idx * 4).toInt() and 255) / 255.0f,
        (vertexBuffer.get(idx * 4 + 1).toInt() and 255) / 255.0f,
        (vertexBuffer.get(idx * 4 + 2).toInt() and 255) / 255.0f,
        (vertexBuffer.get(idx * 4 + 3).toInt() and 255) / 255.0f
    )
}

fun imVertexPositionFromBuffer(vertexBuffer: ByteBuffer, idx: Int) {
    GL11.glVertex3f(
        vertexBuffer.getFloat(idx * 4),
        vertexBuffer.getFloat((idx + 1) * 4),
        vertexBuffer.getFloat((idx + 2) * 4)
    )
}
