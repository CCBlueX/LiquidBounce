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

package net.ccbluex.liquidbounce.render

import com.mojang.blaze3d.vertex.VertexFormat
import com.mojang.blaze3d.vertex.VertexFormatElement

object ClientVertexFormats {

    /**
     * Vertex format for GUI rounded rectangle shader.
     *
     * - UV0: Quad-local UV (0..1). Shader maps this into rect-local coordinates for SDF evaluation.
     * - Color: Fill or outline color.
     * - Size: Rect width/height encoded in UV1.x/UV1.y.
     * - Parameters: Corner radius encoded in UV2.x. UV2.y is reserved for future flags.
     * - StrokeWidth: Outline width in rect-local GUI units. 0 means fill.
     */
    @JvmField
    val GUI_ROUNDED_RECT: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("Color", VertexFormatElement.COLOR)
        .add("Size", VertexFormatElement.UV1)
        .add("Parameters", VertexFormatElement.UV2)
        .add("StrokeWidth", VertexFormatElement.LINE_WIDTH)
        .build()

    /**
     * Vertex format for GUI circle LUT shader.
     *
     * - UV0: Quad-local UV (0..1). Shader remaps this to [-1,1] to evaluate circle SDF.
     * - UV2.x: LUT row index in [net.ccbluex.liquidbounce.render.gui.GuiCircleLutAtlas].
     * - UV2.y: Encoded inner radius ratio (0..32767 => 0..1).
     */
    @JvmField
    val GUI_CIRCLE_LUT: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("UV2", VertexFormatElement.UV2)
        .build()

    /**
     * Vertex format for gradient circle shader.
     *
     * - UV0: Quad-local UV (0..1). Shader remaps this to [-1,1] to evaluate circle SDF.
     * - OuterColor: Packed outer ring RG/BA channels in UV1.x/UV1.y.
     * - InnerColor: Packed inner ring RG/BA channels in UV2.x/UV2.y.
     * - InnerRatio: Inner radius ratio in [0,1] (innerRadius / outerRadius).
     */
    @JvmField
    val GRADIENT_CIRCLE: VertexFormat = VertexFormat.builder()
        .add("Position", VertexFormatElement.POSITION)
        .add("UV0", VertexFormatElement.UV0)
        .add("OuterColor", VertexFormatElement.UV1)
        .add("InnerColor", VertexFormatElement.UV2)
        .add("InnerRatio", VertexFormatElement.LINE_WIDTH)
        .build()
}
