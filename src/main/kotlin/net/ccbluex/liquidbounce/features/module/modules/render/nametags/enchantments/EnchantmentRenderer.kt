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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags.enchantments

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.type.Rect
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.render.VertexFormats

// The auxiliary class renders

object EnchantmentRenderer {
    private const val FIXED_SCALE = 0.6f
    private const val LINE_HEIGHT = 14f
    private const val COLUMN_SPACING = 20f
    private const val PADDING = 3f
    private const val CELL_HEIGHT = LINE_HEIGHT + PADDING * 2
    private const val VERTICAL_SPACING = 4f
    private const val FRAME_MARGIN = 6f
    private val BG_COLOR_NORMAL = Color4b.BLACK.with(a = 200)
    private val BG_COLOR_CURSE = Color4b.RED.darker().with(a = 200)

    fun renderEnchantmentColumn(
        env: RenderEnvironment,
        cells: List<EnchantCell>,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers
    ) {
        val maxWidth = cells.maxOfOrNull { it.textWidth } ?: 0f
        val cellWidth = maxWidth * FIXED_SCALE + PADDING * 2

        cells.forEachIndexed { index, cell ->
            val cellX = x - cellWidth / 2
            val cellY = y + index * (CELL_HEIGHT + VERTICAL_SPACING)

            val rect = Rect(
                cellX,
                cellY,
                cellX + cellWidth,
                cellY + CELL_HEIGHT
            )
            val bgColor = if (cell.isCurse) BG_COLOR_CURSE else BG_COLOR_NORMAL

            drawCellBackground(env, rect, bgColor)

            val textX = cellX + (cellWidth - cell.textWidth * FIXED_SCALE) / 2
            val textY = cellY + PADDING + (LINE_HEIGHT - (ModuleNametags.fontRenderer.height * FIXED_SCALE)) / 2

            ModuleNametags.fontRenderer.draw(
                cell.processedText,
                textX,
                textY,
                shadow = true,
                z = 0.001f,
                scale = FIXED_SCALE
            )
        }

        ModuleNametags.fontRenderer.commit(env, fontRenderer)
    }

    fun drawEnchantmentColumns(
        env: RenderEnvironment,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers,
        columnData: List<EnchantColumn>
    ) {
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val columnsWidth = columnData.sumOf { it.width.toDouble() }.toFloat()
        val spacingWidth = (columnData.size - 1) * COLUMN_SPACING
        val totalWidth = columnsWidth + spacingWidth
        val halfTotalWidth = totalWidth / 2

        val maxColumnHeight = columnData.maxOfOrNull { column ->
            column.cells.size * (CELL_HEIGHT + VERTICAL_SPACING) - VERTICAL_SPACING
        } ?: 0f

        val groupRect = Rect(
            x - halfTotalWidth - FRAME_MARGIN,
            y - FRAME_MARGIN,
            x + halfTotalWidth + FRAME_MARGIN,
            y + maxColumnHeight + FRAME_MARGIN
        )

        drawGroupBorder(env, groupRect)

        var columnX = x - halfTotalWidth
        columnData.forEach { column ->
            val columnCenterX = columnX + column.width / 2
            renderEnchantmentColumn(env, column.cells, columnCenterX, y, fontRenderer)
            columnX += column.width + COLUMN_SPACING
        }
    }

    private fun drawCellBackground(
        env: RenderEnvironment,
        rect: Rect,
        color: Color4b
    ) {
        val argb = color.toARGB()
        env.drawCustomMesh(
            DrawMode.QUADS,
            VertexFormats.POSITION_COLOR,
            ShaderProgramKeys.POSITION_COLOR
        ) { matrix ->
            vertex(matrix, rect.x1, rect.y1, 0.0f).color(argb)
            vertex(matrix, rect.x1, rect.y2, 0.0f).color(argb)
            vertex(matrix, rect.x2, rect.y2, 0.0f).color(argb)
            vertex(matrix, rect.x2, rect.y1, 0.0f).color(argb)
        }
    }

    private fun drawGroupBorder(env: RenderEnvironment, rect: Rect) {
        env.drawCustomMesh(
            DrawMode.QUADS,
            VertexFormats.POSITION_COLOR,
            ShaderProgramKeys.POSITION_COLOR
        ) { matrix ->
            val bgColor = Color4b.BLACK.with(a = 120).toARGB()
            
            vertex(matrix, rect.x1, rect.y1, 0.0f).color(bgColor)
            vertex(matrix, rect.x1, rect.y2, 0.0f).color(bgColor)
            vertex(matrix, rect.x2, rect.y2, 0.0f).color(bgColor)
            vertex(matrix, rect.x2, rect.y1, 0.0f).color(bgColor)
        }
        
        env.drawCustomMesh(
            DrawMode.DEBUG_LINES,
            VertexFormats.POSITION_COLOR,
            ShaderProgramKeys.POSITION_COLOR
        ) { matrix ->
            val color = Color4b.RED.toARGB()

            vertex(matrix, rect.x1, rect.y1, 0.0f).color(color)
            vertex(matrix, rect.x2, rect.y1, 0.0f).color(color)

            vertex(matrix, rect.x2, rect.y1, 0.0f).color(color)
            vertex(matrix, rect.x2, rect.y2, 0.0f).color(color)

            vertex(matrix, rect.x2, rect.y2, 0.0f).color(color)
            vertex(matrix, rect.x1, rect.y2, 0.0f).color(color)

            vertex(matrix, rect.x1, rect.y2, 0.0f).color(color)
            vertex(matrix, rect.x1, rect.y1, 0.0f).color(color)
        }
    }
}
