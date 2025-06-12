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

    // Colors
    private const val GROUP_BORDER_COLOR = 0xFFFF0000.toInt()
    private val BG_COLOR_NORMAL = Color4b(0, 0, 0, 180)
    private val BG_COLOR_CURSE = Color4b(100, 0, 0, 180)

    /**
     * Class for storing enchantment cell information
     */
    private data class EnchantCell(
        val processedText: ProcessedText,
        val textWidth: Float,
        val isCurse: Boolean
    )

    private val ENCHANTMENT_DATA = listOf(
        // Armor enchantments
        Enchantments.PROTECTION to "Pro",
        Enchantments.FIRE_PROTECTION to "FPr",
        Enchantments.FEATHER_FALLING to "FFa",
        Enchantments.BLAST_PROTECTION to "BPr",
        Enchantments.PROJECTILE_PROTECTION to "PPr",
        Enchantments.THORNS to "Tho",
        Enchantments.RESPIRATION to "Res",
        Enchantments.DEPTH_STRIDER to "Dep",
        Enchantments.AQUA_AFFINITY to "Aqu",
        Enchantments.FROST_WALKER to "Fro",
        Enchantments.SOUL_SPEED to "Sou",
        Enchantments.SWIFT_SNEAK to "SwS",

        // Weapon enchantments
        Enchantments.SHARPNESS to "Sha",
        Enchantments.SMITE to "Smi",
        Enchantments.BANE_OF_ARTHROPODS to "BoA",
        Enchantments.KNOCKBACK to "Kno",
        Enchantments.FIRE_ASPECT to "Fir",
        Enchantments.LOOTING to "Loo",
        Enchantments.SWEEPING_EDGE to "Swe",

        // Tool enchantments
        Enchantments.EFFICIENCY to "Eff",
        Enchantments.SILK_TOUCH to "Sil",
        Enchantments.UNBREAKING to "Unb",
        Enchantments.FORTUNE to "For",
        Enchantments.MENDING to "Men",

        // Bow enchantments
        Enchantments.POWER to "Pow",
        Enchantments.PUNCH to "Pun",
        Enchantments.FLAME to "Fla",
        Enchantments.INFINITY to "Inf",

        // Fishing rod enchantments
        Enchantments.LUCK_OF_THE_SEA to "Luc",
        Enchantments.LURE to "Lur",

        // Trident enchantments
        Enchantments.LOYALTY to "Loy",
        Enchantments.IMPALING to "Imp",
        Enchantments.RIPTIDE to "Rip",
        Enchantments.CHANNELING to "Cha",

        // Crossbow enchantments
        Enchantments.MULTISHOT to "Mul",
        Enchantments.QUICK_CHARGE to "QCh",
        Enchantments.PIERCING to "Pie",

        // Curse enchantments
        Enchantments.BINDING_CURSE to "Cur",
        Enchantments.VANISHING_CURSE to "Van"
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
        itemStack.takeIf {
            !it.isEmpty &&
            NametagShowOptions.ENCHANTMENTS.isShowing() &&
            it.getEnchantmentCount() > 0
        }?.let {
            processItemEnchantments(it)
                .takeIf { cells -> cells.isNotEmpty() }
                ?.also { cells ->
                    // Enable blending
                    RenderSystem.enableBlend()
                    RenderSystem.defaultBlendFunc()
                    renderEnchantmentColumn(env, cells, x, y, fontRenderer)
                }
        }
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
            cells to columnWidth
        }

        if (columnData.isNotEmpty()) {
            drawEnchantmentColumns(env, x, y, fontRenderer, columnData)
        }
    }

    private fun processItemEnchantments(itemStack: ItemStack): List<EnchantCell> {
        val enchantments = ENCHANTMENT_DATA
            .mapNotNull { (enchantment, name) ->
                itemStack.getEnchantment(enchantment).takeIf { it > 0 }?.let { level ->
                    name to level
                }
            }

        if (enchantments.isEmpty()) return emptyList()

        val sortedEnchantments = enchantments.sortedByDescending { it.second }
        val hasMoreEnchantments = sortedEnchantments.size > MAX_ENCHANTMENTS_PER_ITEM

        val cells = sortedEnchantments
            .take(MAX_ENCHANTMENTS_PER_ITEM)
            .map { (name, level) -> createCell(name, level) }

        if (!hasMoreEnchantments || cells.isEmpty()) {
            return cells
        }

        return cells.toMutableList().apply {
            removeAt(lastIndex)
            add(createCell(null, 0, true))
        }
    }

    private fun getEntityItemsWithEnchantments(entity: LivingEntity) = mutableListOf(
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
    private fun createCell(name: String? = null, level: Int = 0, isEllipsis: Boolean = false): EnchantCell {
        val text = if (isEllipsis) {
            "${Formatting.GRAY}..."
        } else {
            val textColor = when {
                name == "Cur" || name == "Van" -> Formatting.RED
                level >= 4 -> Formatting.GOLD
                level == 3 -> Formatting.YELLOW
                level == 2 -> Formatting.GREEN
                else -> Formatting.WHITE
            }
            "${textColor}$name $level"
        }

        val processedText = ModuleNametags.fontRenderer.process(text)
        val textWidth = ModuleNametags.fontRenderer.getStringWidth(processedText, false)
        return EnchantCell(
            processedText,
            textWidth,
            !isEllipsis && (name == "Cur" || name == "Van")
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
        columnData: List<Pair<List<EnchantCell>, Float>>
    ) {
        val columnsWidth = columnData.sumOf { it.second.toDouble() }.toFloat()
        val spacingWidth = (columnData.size - 1) * COLUMN_SPACING
        val totalWidth = columnsWidth + spacingWidth
        val halfTotalWidth = totalWidth / 2

        val maxColumnHeight = columnData.maxOfOrNull { (cells, _) ->
            cells.size * (CELL_HEIGHT + VERTICAL_SPACING) - VERTICAL_SPACING
        } ?: 0f

        val groupRect = Rect(
            x - halfTotalWidth - PADDING,
            y + Y_OFFSET - PADDING,
            x + halfTotalWidth + PADDING,
            y + Y_OFFSET + maxColumnHeight + PADDING
        )

        drawGroupBorder(env, groupRect)

        var columnX = x - halfTotalWidth
        columnData.forEach { (cells, columnWidth) ->
            val columnCenterX = columnX + columnWidth / 2
            renderEnchantmentColumn(env, cells, columnCenterX, y, fontRenderer)
            columnX += columnWidth + COLUMN_SPACING
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
