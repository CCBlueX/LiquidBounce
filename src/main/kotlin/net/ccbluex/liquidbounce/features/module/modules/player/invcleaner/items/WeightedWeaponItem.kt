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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.SwordItem
import kotlin.math.pow

open class WeightedWeaponItem(itemSlot: ItemSlot) : WeightedItem(itemSlot) {
    companion object {
        val DAMAGE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SHARPNESS, 0.5f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SMITE, 2.0f * 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BANE_OF_ARTHROPODS, 2.0f * 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.75f),
            )
        val SECONDARY_VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.LOOTING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SWEEPING, 0.2f),
            )
        private val COMPARATOR =
            ComparatorChain<WeightedWeaponItem>(
                { o1, o2 -> (estimateDamage(o1)).compareTo(estimateDamage(o2)) },
                { o1, o2 ->
                    SECONDARY_VALUE_ESTIMATOR.estimateValue(o1.itemStack)
                        .compareTo(SECONDARY_VALUE_ESTIMATOR.estimateValue(o2.itemStack))
                },
                { o1, o2 -> compareByCondition(o1, o2) { it.itemStack.item is SwordItem } },
                { o1, o2 -> o1.itemStack.item.enchantability.compareTo(o2.itemStack.item.enchantability) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )

        private fun estimateDamage(o1: WeightedWeaponItem): Float {
            val attackSpeed = o1.itemStack.item.attackSpeed
            val attackDamage = o1.itemStack.item.attackDamage

            val p = 0.85.pow(1 / 20.0)
            val bigT = 20.0 / attackSpeed

            val probabilityAdjustmentFactor = p.pow(bigT)

            val speedAdjustedDamage = attackDamage * attackSpeed * probabilityAdjustmentFactor.toFloat()

            val damageFromFireAspect = o1.itemStack.getEnchantment(Enchantments.FIRE_ASPECT) * 4.0f * 0.625f * 0.9f

            return speedAdjustedDamage * (1.0f + DAMAGE_ESTIMATOR.estimateValue(o1.itemStack)) + damageFromFireAspect
        }
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.WEAPON, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedWeaponItem)
    }
}
