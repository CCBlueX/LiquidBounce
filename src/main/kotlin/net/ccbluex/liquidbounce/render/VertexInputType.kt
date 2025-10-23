/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.vertex.VertexFormat
import net.minecraft.client.render.VertexFormats
import net.minecraft.util.Identifier

enum class VertexInputType(
    val vertexFormat: VertexFormat,
    val vertexShader: Identifier,
    val fragmentShader: Identifier,
) {
    Pos(
        VertexFormats.POSITION,
        Identifier.ofVanilla("core/position"),
        Identifier.ofVanilla("core/position"),
    ),
    PosColor(
        VertexFormats.POSITION_COLOR,
        Identifier.ofVanilla("core/position_color"),
        Identifier.ofVanilla("core/position_color"),
    ),
    PosTexColor(
        VertexFormats.POSITION_TEXTURE_COLOR,
        Identifier.ofVanilla("core/position_tex_color"),
        Identifier.ofVanilla("core/position_tex_color"),
    ),
}
