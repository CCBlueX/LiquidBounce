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

import net.ccbluex.liquidbounce.features.module.modules.render.nametags.ModuleNametags
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.getEnchantmentCount
import net.minecraft.entity.EquipmentSlot
import net.minecraft.entity.LivingEntity
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKeys
import net.minecraft.util.Formatting

// Auxiliary Processing Class

object EnchantmentProcessor {
    private const val MAX_ENCHANTMENTS_PER_ITEM = 10

    private val supportedEnchantments by lazy {
        mc.world?.registryManager?.getOrThrow(RegistryKeys.ENCHANTMENT)?.keys?.toList() ?: emptyList()
    }

    fun processItemEnchantments(itemStack: ItemStack): List<EnchantCell> {
        val enchantmentList = mutableListOf<Pair<EnchantmentInfo, Int>>()
        
        for (enchantmentKey in supportedEnchantments) {
            val level = itemStack.getEnchantment(enchantmentKey)
            if (level > 0) {
                enchantmentList.add(EnchantmentDisplayHelper.getEnchantmentInfo(enchantmentKey) to level)
            }
        }

        if (enchantmentList.isEmpty()) return emptyList()

        val sortedEnchantments = enchantmentList.sortedByDescending { it.second }
        val hasMoreEnchantments = sortedEnchantments.size > MAX_ENCHANTMENTS_PER_ITEM

        val cells = sortedEnchantments
            .take(MAX_ENCHANTMENTS_PER_ITEM)
            .map { (info, level) -> createCell(info, level) }
            .toMutableList()

        if (hasMoreEnchantments && cells.isNotEmpty()) {
            cells[cells.lastIndex] = createCell(null, 0, true)
        }

        return cells
    }

    fun getEntityItemsWithEnchantments(entity: LivingEntity): List<ItemStack> = listOf(
        entity.mainHandStack,
        entity.offHandStack,
        entity.getEquippedStack(EquipmentSlot.HEAD),
        entity.getEquippedStack(EquipmentSlot.CHEST),
        entity.getEquippedStack(EquipmentSlot.LEGS),
        entity.getEquippedStack(EquipmentSlot.FEET)
    ).filter { !it.isEmpty && it.getEnchantmentCount() > 0 }

    fun createCell(
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
}
