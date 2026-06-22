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
package net.ccbluex.liquidbounce.features.module.modules.render.nametags

import it.unimi.dsi.fastutil.objects.ObjectArrayList
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.fastutil.mapToCharArray
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.render.drawRoundedRect
import net.ccbluex.liquidbounce.render.engine.font.processor.MinecraftTextProcessor
import net.ccbluex.liquidbounce.render.engine.type.Color4b
import net.ccbluex.liquidbounce.utils.item.getEnchantmentCount
import net.ccbluex.liquidbounce.utils.collection.LruCache
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.minecraft.ChatFormatting
import net.minecraft.client.gui.GuiGraphicsExtractor
import net.minecraft.client.gui.render.GuiRenderer
import net.minecraft.core.Holder
import net.minecraft.tags.EnchantmentTags
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper

private object EnchantmentDisplayHelper {
    private val enchantmentAbbreviationCache = LruCache<Holder<Enchantment>, String>(128)

    private const val MAX_NAME_LENGTH = 3

    fun getEnchantmentInfo(enchantment: Holder<Enchantment>, level: Int): EnchantmentInfo {
        return EnchantmentInfo(
            displayName = getAbbreviation(enchantment),
            isCurse = isCurse(enchantment),
            level,
        )
    }

    /**
     * @see net.minecraft.world.item.enchantment.Enchantment.getFullname
     */
    private fun getEnchantmentName(enchantment: Holder<Enchantment>): String =
        enchantment.value().description.string

    private fun getInitialsAbbreviation(words: List<String>): String =
        words.mapToCharArray { it.first() }.concatToString()

    private fun getCompoundAbbreviation(words: List<String>): String {
        val firstWord = words.first()

        if (firstWord.length >= MAX_NAME_LENGTH) {
            return firstWord.take(MAX_NAME_LENGTH)
        }

        val remainingChars = MAX_NAME_LENGTH - firstWord.length
        return firstWord + words.getOrNull(1)?.take(remainingChars).orEmpty()
    }

    private fun processMultiWordName(words: List<String>): String {
        return if (words.size >= MAX_NAME_LENGTH) {
            getInitialsAbbreviation(words)
        } else {
            getCompoundAbbreviation(words)
        }
    }

    private fun processName(name: String): String {
        if (name.length <= MAX_NAME_LENGTH) {
            return name
        }

        val words = name.split(' ').filter { it.isNotEmpty() }

        return if (words.size >= 2) {
            processMultiWordName(words)
        } else {
            words.getOrNull(0).orEmpty().take(MAX_NAME_LENGTH)
        }
    }

    private fun getAbbreviation(enchantment: Holder<Enchantment>): String {
        return enchantmentAbbreviationCache.computeIfAbsent(enchantment) {
            val name = getEnchantmentName(it)
            processName(name)
        }
    }

    /**
     * @see net.minecraft.world.item.enchantment.Enchantment.getFullname
     */
    private fun isCurse(enchantment: Holder<Enchantment>): Boolean = enchantment.`is`(EnchantmentTags.CURSE)
}

@JvmRecord
private data class EnchantmentInfo(
    val displayName: String,
    val isCurse: Boolean,
    val level: Int,
)

internal object NametagEnchantmentRenderer : ToggleableValueGroup(ModuleNametags, "Enchantment", true) {

    private val scale by float("Scale", 0.8f, 0.25f..4f)
    private val maxCountPerItem by int("MaxCountPerItem", 4, 1..16)
    private val backgroundRadius by float("BackgroundRadius", 1.0f, 0f..8f)

    private const val ITEM_SIZE = GuiRenderer.DEFAULT_ITEM_SIZE.toFloat()
    private const val ITEM_CENTER_X = ITEM_SIZE * 0.5f
    private const val LABEL_PADDING_X = 1.25f
    private const val LABEL_PADDING_Y = 0.5f
    private const val LABEL_VERTICAL_GAP = 1.5f
    private const val LABEL_ROW_SPACING = 0.5f
    private val BG_COLOR_NORMAL = Color4b.DEFAULT_BG_COLOR
    private val BG_COLOR_CURSE = Color4b.RED.darker().alpha(150)

    private val labelTextScale get() = scale * ModuleNametags.fontRenderer.scaleToVanillaFont
    private val labelBackgroundRadius get() = backgroundRadius * scale

    private val enchantmentInfoComparator = Comparator
        .comparingInt<EnchantmentInfo> { -it.level }
        .thenComparing { it.displayName }

    @JvmRecord
    private data class EnchantCell(
        val processedText: MinecraftTextProcessor.RecyclingProcessedText,
        val textWidth: Float,
        val isCurse: Boolean
    )

    context(guiGraphics: GuiGraphicsExtractor)
    fun drawItemEnchantments(
        stack: ItemStack,
        x: Float,
        y: Float,
    ) {
        if (!running || stack.isEmpty || stack.getEnchantmentCount() == 0) {
            return
        }

        val cells = processItemEnchantments(stack)
        if (cells.isEmpty()) {
            return
        }

        val rowHeight = ModuleNametags.fontRenderer.height * labelTextScale + LABEL_PADDING_Y * 2f
        val totalHeight = cells.size * rowHeight + (cells.size - 1) * LABEL_ROW_SPACING
        val centerX = x + ITEM_CENTER_X
        var rowY = y - LABEL_VERTICAL_GAP - totalHeight

        cells.forEach { cell ->
            drawCell(cell, centerX, rowY, rowHeight)
            rowY += rowHeight + LABEL_ROW_SPACING
        }
    }

    private fun processItemEnchantments(itemStack: ItemStack): List<EnchantCell> {
        val enchantmentList = ObjectArrayList<EnchantmentInfo>()

        // Use EnchantmentHelper for both normal items and enchantment books
        for (itemEnchantment in EnchantmentHelper.getEnchantmentsForCrafting(itemStack).entrySet()) {
            val enchantment = itemEnchantment.key
            val level = itemEnchantment.intValue
            if (level <= 0) continue
            enchantmentList += EnchantmentDisplayHelper.getEnchantmentInfo(enchantment, level)
        }

        if (enchantmentList.isEmpty) return emptyList()
        enchantmentList.sortWith(enchantmentInfoComparator)

        val hasMoreEnchantments = enchantmentList.size > maxCountPerItem

        val cells = (if (hasMoreEnchantments) enchantmentList.subList(0, maxCountPerItem) else enchantmentList)
            .mapToArray { info -> createCell(info, isEllipsis = false) }

        if (hasMoreEnchantments && cells.isNotEmpty()) {
            MinecraftTextProcessor.TEXT_POOL.recycle(cells.last().processedText)
            cells[cells.lastIndex] = createCell(null, true)
        }

        return cells.asList()
    }

    private fun createCell(
        info: EnchantmentInfo?,
        isEllipsis: Boolean,
    ): EnchantCell {
        val text = if (isEllipsis) {
            "...".asPlainText(ChatFormatting.GRAY)
        } else {
            requireNotNull(info)
            val textColor = when {
                info.isCurse -> ChatFormatting.RED
                info.level >= 4 -> ChatFormatting.GOLD
                info.level == 3 -> ChatFormatting.YELLOW
                info.level == 2 -> ChatFormatting.GREEN
                else -> ChatFormatting.WHITE
            }
            "${info.displayName}${info.level}".asPlainText(textColor)
        }

        val processedText = ModuleNametags.fontRenderer.process(text)
        val textWidth = ModuleNametags.fontRenderer.getStringWidth(processedText, false)
        return EnchantCell(
            processedText,
            textWidth,
            !isEllipsis && info?.isCurse == true
        )
    }

    context(guiGraphics: GuiGraphicsExtractor)
    private fun drawCell(
        cell: EnchantCell,
        centerX: Float,
        y: Float,
        rowHeight: Float,
    ) {
        val textWidth = cell.textWidth * labelTextScale
        val width = textWidth + LABEL_PADDING_X * 2f
        val x1 = centerX - width * 0.5f
        val x2 = centerX + width * 0.5f

        guiGraphics.drawRoundedRect(
            x1 = x1,
            y1 = y,
            x2 = x2,
            y2 = y + rowHeight,
            radius = labelBackgroundRadius,
            fillColor = if (cell.isCurse) BG_COLOR_CURSE else BG_COLOR_NORMAL,
        )

        ModuleNametags.fontRenderer.draw(cell.processedText) {
            this.x = centerX - textWidth * 0.5f
            this.y = y + LABEL_PADDING_Y
            shadow = true
            scale = labelTextScale
        }
    }
}
