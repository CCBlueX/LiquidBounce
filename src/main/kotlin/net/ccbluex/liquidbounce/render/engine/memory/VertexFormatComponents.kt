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

import org.lwjgl.opengl.GL11

class VertexFormatComponent(
    val type: VertexFormatComponentDataType,
    val offset: Int,
    val count: Int,
    val normalized: Boolean,
    val attribInfo: AttributeInfo
) {

    val length: Int
        get() = this.type.length * this.count
}

class AttributeInfo(val attributeType: AttributeType)

enum class AttributeType(val openGlClientState: Int) {
    Position(GL11.GL_VERTEX_ARRAY),
    Color(GL11.GL_COLOR_ARRAY),
    Texture(GL11.GL_TEXTURE_COORD_ARRAY)
}

enum class VertexFormatComponentDataType(val openGlEnum: Int, val legacyOpenGlEnum: Int, val length: Int) {
    GlByte(GL11.GL_BYTE, GL11.GL_BYTE, 1),
    GlUnsignedByte(GL11.GL_UNSIGNED_BYTE, GL11.GL_BYTE, 1),
    GlShort(GL11.GL_SHORT, GL11.GL_SHORT, 2),
    GlUnsignedShort(GL11.GL_UNSIGNED_SHORT, GL11.GL_SHORT, 2),
    GlInt(GL11.GL_INT, GL11.GL_INT, 4),
    GlUnsignedInt(GL11.GL_UNSIGNED_INT, GL11.GL_INT, 4),
    GlFloat(GL11.GL_FLOAT, GL11.GL_FLOAT, 4),
    GlDouble(GL11.GL_DOUBLE, GL11.GL_DOUBLE, 8)
}
