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

import net.minecraft.client.gl.ShaderProgramKey
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.*

enum class VertexInputType(
    val vertexFormat: VertexFormat,
    val shaderProgram: ShaderProgramKey,
) {
    PosColor(VertexFormats.POSITION_COLOR, ShaderProgramKeys.POSITION_COLOR),
    PosTexColor(VertexFormats.POSITION_TEXTURE_COLOR, ShaderProgramKeys.POSITION_TEX_COLOR),
}
