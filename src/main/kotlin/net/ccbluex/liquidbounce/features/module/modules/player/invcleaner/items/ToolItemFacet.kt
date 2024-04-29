/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.type
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ToolItem

class ToolItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
            )
        private val COMPARATOR =
            ComparatorChain<ToolItemFacet>(
                compareBy { (it.itemStack.item as ToolItem).material.miningSpeedMultiplier },
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.TOOL, (this.itemStack.item as ToolItem).type)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as ToolItemFacet)
    }
}
