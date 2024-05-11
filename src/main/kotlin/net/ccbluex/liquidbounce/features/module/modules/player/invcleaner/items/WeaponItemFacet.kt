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
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.SwordItem
import kotlin.math.ceil
import kotlin.math.pow

open class WeaponItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        /**
         * Estimates damage for different enchantments. Note that sharpness is already considered by
         * `ItemStack.attackDamage`
         */
        val DAMAGE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SMITE, 2.0f * 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BANE_OF_ARTHROPODS, 2.0f * 0.1f),
                // Knockback deals no damage, but it allows us to deal more damage because we don't get hit as often.
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.75f),
            )
        val SECONDARY_VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.LOOTING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SWEEPING_EDGE, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.25f),
            )
        private val COMPARATOR =
            ComparatorChain<WeaponItemFacet>(
                compareBy { estimateDamage(it) },
                compareBy { SECONDARY_VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                compareByCondition { it.itemStack.item is SwordItem },
                compareBy { it.itemStack.item.enchantability },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )

        private fun estimateDamage(o1: WeaponItemFacet): Float {
            // Already contains damage enchantments like sharpness
            val attackDamage = o1.itemStack.attackDamage
            val attackSpeed = o1.itemStack.attackSpeed

            val p = 0.85.pow(1 / 20.0)
            val bigT = 20.0 / attackSpeed

            val probabilityAdjustmentFactor = p.pow(ceil(bigT * 0.9))

            val speedAdjustedDamage = attackDamage * attackSpeed * probabilityAdjustmentFactor.toFloat()

            val damageFromFireAspect = (o1.itemStack.getEnchantment(Enchantments.FIRE_ASPECT) * 4.0f - 1)
                    .coerceAtLeast(0.0F) * 0.33F

            val additionalFactor = DAMAGE_ESTIMATOR.estimateValue(o1.itemStack)

            return speedAdjustedDamage * (1.0F + additionalFactor) + damageFromFireAspect
        }
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.WEAPON, 0)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as WeaponItemFacet)
    }
}
