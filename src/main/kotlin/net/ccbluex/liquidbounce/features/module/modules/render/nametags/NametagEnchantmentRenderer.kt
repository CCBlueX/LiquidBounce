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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.render.RenderEnvironment
import net.ccbluex.liquidbounce.render.drawCustomMesh
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.render.engine.font.FontRendererBuffers
import net.ccbluex.liquidbounce.render.engine.font.processor.TextProcessor.ProcessedText
import net.ccbluex.liquidbounce.render.engine.type.Rect
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.getEnchantmentCount
import net.minecraft.client.render.VertexFormats
import net.minecraft.client.render.VertexFormat.DrawMode
import net.minecraft.client.gl.ShaderProgramKeys
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.util.Formatting

/**
 * Renders item enchantments in nametags
 */
object NametagEnchantmentRenderer {

    // Display settings
    private const val MAX_ENCHANTMENTS_PER_ITEM = 10
    private const val FIXED_SCALE = 0.6f
    private const val Y_OFFSET = -40f

    // Layout dimensions
    private const val LINE_HEIGHT = 14f
    private const val COLUMN_SPACING = 20f
    private const val PADDING = 3f
    private const val CELL_HEIGHT = LINE_HEIGHT + PADDING * 2
    private const val VERTICAL_SPACING = 4f
    private const val FRAME_MARGIN = 6f

    // Colors
    private const val GROUP_BORDER_COLOR = 0xFFFF0000.toInt()
    private val BG_COLOR_NORMAL = Color4b(0, 0, 0, 180)
    private val BG_COLOR_CURSE = Color4b(100, 0, 0, 180)

    private data class EnchantmentInfo(
        val displayName: String,
        val isCurse: Boolean = false
    )

    /**
     * Class for storing enchantment cell information
     */
    private data class EnchantCell(
        val processedText: ProcessedText,
        val textWidth: Float,
        val isCurse: Boolean
    )

    /**
     * Class for storing column data
     */
    private data class EnchantColumn(
        val cells: List<EnchantCell>,
        val width: Float
    )

    private val ENCHANTMENT_DATA = arrayOf(
        // Armor enchantments
        Enchantments.PROTECTION to EnchantmentInfo("Pro"),
        Enchantments.FIRE_PROTECTION to EnchantmentInfo("FPr"),
        Enchantments.FEATHER_FALLING to EnchantmentInfo("FFa"),
        Enchantments.BLAST_PROTECTION to EnchantmentInfo("BPr"),
        Enchantments.PROJECTILE_PROTECTION to EnchantmentInfo("PPr"),
        Enchantments.THORNS to EnchantmentInfo("Tho"),
        Enchantments.RESPIRATION to EnchantmentInfo("Res"),
        Enchantments.DEPTH_STRIDER to EnchantmentInfo("Dep"),
        Enchantments.AQUA_AFFINITY to EnchantmentInfo("Aqu"),
        Enchantments.FROST_WALKER to EnchantmentInfo("Fro"),
        Enchantments.SOUL_SPEED to EnchantmentInfo("Sou"),
        Enchantments.SWIFT_SNEAK to EnchantmentInfo("SwS"),

        // Weapon enchantments
        Enchantments.SHARPNESS to EnchantmentInfo("Sha"),
        Enchantments.SMITE to EnchantmentInfo("Smi"),
        Enchantments.BANE_OF_ARTHROPODS to EnchantmentInfo("BoA"),
        Enchantments.KNOCKBACK to EnchantmentInfo("Kno"),
        Enchantments.FIRE_ASPECT to EnchantmentInfo("Fir"),
        Enchantments.LOOTING to EnchantmentInfo("Loo"),
        Enchantments.SWEEPING_EDGE to EnchantmentInfo("Swe"),

        // Tool enchantments
        Enchantments.EFFICIENCY to EnchantmentInfo("Eff"),
        Enchantments.SILK_TOUCH to EnchantmentInfo("Sil"),
        Enchantments.UNBREAKING to EnchantmentInfo("Unb"),
        Enchantments.FORTUNE to EnchantmentInfo("For"),
        Enchantments.MENDING to EnchantmentInfo("Men"),

        // Bow enchantments
        Enchantments.POWER to EnchantmentInfo("Pow"),
        Enchantments.PUNCH to EnchantmentInfo("Pun"),
        Enchantments.FLAME to EnchantmentInfo("Fla"),
        Enchantments.INFINITY to EnchantmentInfo("Inf"),

        // Fishing rod enchantments
        Enchantments.LUCK_OF_THE_SEA to EnchantmentInfo("Luc"),
        Enchantments.LURE to EnchantmentInfo("Lur"),

        // Trident enchantments
        Enchantments.LOYALTY to EnchantmentInfo("Loy"),
        Enchantments.IMPALING to EnchantmentInfo("Imp"),
        Enchantments.RIPTIDE to EnchantmentInfo("Rip"),
        Enchantments.CHANNELING to EnchantmentInfo("Cha"),

        // Crossbow enchantments
        Enchantments.MULTISHOT to EnchantmentInfo("Mul"),
        Enchantments.QUICK_CHARGE to EnchantmentInfo("QCh"),
        Enchantments.PIERCING to EnchantmentInfo("Pie"),

        // Curse enchantments
        Enchantments.BINDING_CURSE to EnchantmentInfo("Cur", isCurse = true),
        Enchantments.VANISHING_CURSE to EnchantmentInfo("Van", isCurse = true)
    )

    /**
     * Renders item enchantments in nametag
     */
    fun drawEnchantments(
        env: RenderEnvironment,
        itemStack: ItemStack,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers
    ) {
        if (itemStack.isEmpty || !NametagShowOptions.ENCHANTMENTS.isShowing() || itemStack.getEnchantmentCount() <= 0) {
            return
        }

        val cells = processItemEnchantments(itemStack)
        if (cells.isEmpty()) {
            return
        }

        // Enable blending
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        renderEnchantmentColumn(env, cells, x, y, fontRenderer)
    }

    /**
     * Renders enchantments for all entity equipment items
     */
    fun drawEntityEnchantments(
        env: RenderEnvironment,
        entity: LivingEntity,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers
    ) {
        if (!NametagShowOptions.ENCHANTMENTS.isShowing()) return

        val itemsWithEnchantments = getEntityItemsWithEnchantments(entity)
        if (itemsWithEnchantments.isEmpty()) return

        // Enable blending
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()

        val columnData = itemsWithEnchantments.mapNotNull { item ->
            val cells = processItemEnchantments(item)
            if (cells.isEmpty()) return@mapNotNull null

            val maxWidth = cells.maxOfOrNull { it.textWidth } ?: 0f
            val columnWidth = maxWidth * FIXED_SCALE + PADDING * 2
            EnchantColumn(cells, columnWidth)
        }

        if (columnData.isNotEmpty()) {
            drawEnchantmentColumns(env, x, y, fontRenderer, columnData)
        }
    }

    private fun processItemEnchantments(itemStack: ItemStack): List<EnchantCell> {
        val enchantments = ENCHANTMENT_DATA
            .mapNotNull { (enchantment, info) ->
                itemStack.getEnchantment(enchantment).takeIf { it > 0 }?.let { level ->
                    info to level
                }
            }

        if (enchantments.isEmpty()) return emptyList()

        val sortedEnchantments = enchantments.sortedByDescending { it.second }
        val hasMoreEnchantments = sortedEnchantments.size > MAX_ENCHANTMENTS_PER_ITEM

        val cells = sortedEnchantments
            .take(MAX_ENCHANTMENTS_PER_ITEM)
            .map { (info, level) -> createCell(info, level) }

        if (!hasMoreEnchantments || cells.isEmpty()) {
            return cells
        }

        val result = cells.toMutableList()
        result[result.lastIndex] = createCell(null, 0, true)
        return result
    }

    private fun getEntityItemsWithEnchantments(entity: LivingEntity) = arrayOf(
        entity.mainHandStack,
        entity.offHandStack,
        entity.getEquippedStack(EquipmentSlot.HEAD),
        entity.getEquippedStack(EquipmentSlot.CHEST),
        entity.getEquippedStack(EquipmentSlot.LEGS),
        entity.getEquippedStack(EquipmentSlot.FEET)
    ).filter { !it.isEmpty && it.getEnchantmentCount() > 0 }

    /**
     * Creates a cell for display (enchantment or ellipsis)
     */
    private fun createCell(
        info: EnchantmentInfo? = null,
        level: Int = 0,
        isEllipsis: Boolean = false
    ): EnchantCell {
        val text = if (isEllipsis) {
            "${Formatting.GRAY}..."
        } else {
            val textColor = when {
                info?.isCurse == true -> Formatting.RED
                level >= 4 -> Formatting.GOLD
                level == 3 -> Formatting.YELLOW
                level == 2 -> Formatting.GREEN
                else -> Formatting.WHITE
            }
            "${textColor}${info?.displayName} $level"
        }

        val processedText = ModuleNametags.fontRenderer.process(text)
        val textWidth = ModuleNametags.fontRenderer.getStringWidth(processedText, false)
        return EnchantCell(
            processedText,
            textWidth,
            !isEllipsis && info?.isCurse == true
        )
    }

    private fun renderEnchantmentColumn(
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
            val cellY = y + Y_OFFSET + index * (CELL_HEIGHT + VERTICAL_SPACING)

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

    /**
     * Renders enchantment columns
     */
    private fun drawEnchantmentColumns(
        env: RenderEnvironment,
        x: Float,
        y: Float,
        fontRenderer: FontRendererBuffers,
        columnData: List<EnchantColumn>
    ) {
        val columnsWidth = columnData.sumOf { it.width.toDouble() }.toFloat()
        val spacingWidth = (columnData.size - 1) * COLUMN_SPACING
        val totalWidth = columnsWidth + spacingWidth
        val halfTotalWidth = totalWidth / 2

        val maxColumnHeight = columnData.maxOfOrNull { column ->
            column.cells.size * (CELL_HEIGHT + VERTICAL_SPACING) - VERTICAL_SPACING
        } ?: 0f

        val groupRect = Rect(
            x - halfTotalWidth - FRAME_MARGIN,
            y + Y_OFFSET - FRAME_MARGIN,
            x + halfTotalWidth + FRAME_MARGIN,
            y + Y_OFFSET + maxColumnHeight + FRAME_MARGIN
        )

        drawGroupBorder(env, groupRect)

        var columnX = x - halfTotalWidth
        columnData.forEach { column ->
            val columnCenterX = columnX + column.width / 2
            renderEnchantmentColumn(env, column.cells, columnCenterX, y, fontRenderer)
            columnX += column.width + COLUMN_SPACING
        }
    }

    private fun drawGroupBorder(env: RenderEnvironment, rect: Rect) {
        env.drawCustomMesh(
            DrawMode.DEBUG_LINES,
            VertexFormats.POSITION_COLOR,
            ShaderProgramKeys.POSITION_COLOR
        ) { matrix ->
            val color = Color4b(GROUP_BORDER_COLOR, true).toARGB()

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
