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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.enchantment.Enchantments

class CrossbowItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.QUICK_CHARGE, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MULTISHOT, 1.5f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.PIERCING, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.25f),
            )
        private val COMPARATOR =
            ComparatorChain<CrossbowItemFacet>(
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.CROSSBOW, 0)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as CrossbowItemFacet)
    }
}
