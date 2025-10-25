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
import net.ccbluex.liquidbounce.utils.item.isAxe
import net.ccbluex.liquidbounce.utils.item.isHoe
import net.ccbluex.liquidbounce.utils.item.isPickaxe
import net.ccbluex.liquidbounce.utils.item.isShovel
import net.ccbluex.liquidbounce.utils.item.toolComponent
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ItemStack

class MiningToolItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        const val MASK_AXE = 1 shl 0
        const val MASK_PICKAXE = 1 shl 1
        const val MASK_SHOVEL = 1 shl 2
        const val MASK_HOE = 1 shl 3

        private val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SILK_TOUCH, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FORTUNE, 0.33f),
            )
        private val COMPARATOR =
            /**
             * @see net.minecraft.item.ToolMaterial.applyToolSettings
             * @see net.minecraft.component.type.ToolComponent.Rule.ofAlwaysDropping
             */
            ComparatorChain<MiningToolItemFacet>(
                compareBy {
                    val toolComponent = it.itemStack.toolComponent ?: return@compareBy 0f
                    toolComponent.rules.firstOrNull { rule ->
                        rule.correctForDrops.orElse(false)
                    }?.speed?.orElse(null) ?: toolComponent.defaultMiningSpeed
                },
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_BETTER_DURABILITY,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )

        // TODO: compare multi tool item
        private val ItemStack.miningToolType: Int
            get() {
                var bits = 0
                if (isAxe) bits = bits or MASK_AXE
                if (isPickaxe) bits = bits or MASK_PICKAXE
                if (isShovel) bits = bits or MASK_SHOVEL
                if (isHoe) bits = bits or MASK_HOE
                if (bits == 0) error("Item ${this.item} is not a mining tool")
                return bits
            }
    }

    override val category = ItemCategory(ItemType.TOOL, this.itemStack.miningToolType)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as MiningToolItemFacet)
    }
}
