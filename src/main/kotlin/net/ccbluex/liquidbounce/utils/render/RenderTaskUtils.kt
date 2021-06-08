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

import net.ccbluex.liquidbounce.render.engine.CullingMode
import net.ccbluex.liquidbounce.render.engine.GlRenderState
import net.ccbluex.liquidbounce.render.engine.PrimitiveType
import net.ccbluex.liquidbounce.render.engine.VertexFormatRenderTask
import net.ccbluex.liquidbounce.render.engine.memory.IndexBuffer
import net.ccbluex.liquidbounce.render.engine.memory.PositionColorVertexFormat
import net.ccbluex.liquidbounce.render.engine.memory.VertexFormat
import net.ccbluex.liquidbounce.render.shaders.ColoredPrimitiveShader
import net.ccbluex.liquidbounce.render.shaders.InstancedColoredPrimitiveShader

fun espBoxInstancedRenderTask(
    instanceBuffer: PositionColorVertexFormat,
    vertexFormat: VertexFormat,
    indexBuffer: IndexBuffer
) = VertexFormatRenderTask(
    vertexFormat,
    PrimitiveType.Triangles,
    InstancedColoredPrimitiveShader,
    indexBuffer = indexBuffer,
    perInstance = instanceBuffer,
    state = GlRenderState(culling = CullingMode.BACKFACE_CULLING)
)

fun espBoxRenderTask(
    pair: Pair<VertexFormat, IndexBuffer>
) = VertexFormatRenderTask(
    pair.first,
    PrimitiveType.Triangles,
    ColoredPrimitiveShader,
    indexBuffer = pair.second,
    state = GlRenderState(culling = CullingMode.BACKFACE_CULLING)
)

fun espBoxInstancedOutlineRenderTask(
    instanceBuffer: PositionColorVertexFormat,
    vertexFormat: VertexFormat,
    indexBuffer: IndexBuffer
) = VertexFormatRenderTask(
    vertexFormat,
    PrimitiveType.Lines,
    InstancedColoredPrimitiveShader,
    indexBuffer = indexBuffer,
    perInstance = instanceBuffer,
    state = GlRenderState(lineWidth = 2.0f, lineSmooth = true)
)
