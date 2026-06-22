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

package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.world.item.MaceItem
import net.minecraft.world.item.enchantment.Enchantments
import kotlin.math.ceil
import kotlin.math.pow


/**
 * Specialization of weapon type. Used in order to allow the user to specify that they want a mace and not an axe
 * or something.
 */
class MaceItemFacet(itemSlot: ItemSlot) : WeaponItemFacet(itemSlot) {
    override val category get() = ItemType.MACE.defaultCategory

    companion object {
        /** `0.85.pow(1 / 20.0)` */
        const val P = 0.9919069797821398
        const val ASSUMED_FALL_DISTANCE = 15.0
        /**
         * Estimates damage for different enchantments. Note that sharpness is already considered by
         * `ItemStack.attackDamage`
         */
        private val DAMAGE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.DENSITY, 0.5f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BREACH, 0.15f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SMITE, 2.0f * 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.BANE_OF_ARTHROPODS, 2.0f * 0.1f),
                // Knockback deals no damage, but it allows us to deal more damage because we don't get hit as often.
//                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.WIND_BURST, 0.2f),
            )

        private val COMPARATOR =
            ComparatorChain<MaceItemFacet>(
                Comparator.comparingDouble(this::estimateDamage),
                compareByCondition { it.itemStack.item is MaceItem },
                PREFER_BETTER_DURABILITY,
                PREFER_ENCHANTABLE,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )

        /** Copied (and partially refactored) from [MaceItem.getAttackDamageBonus] **/
        private fun getBonusAttackDamage(fallDistance: Double): Float {
            val dmg = if (fallDistance <= 3.0) {
                4.0 * fallDistance
            } else if (fallDistance <= 8.0) {
                12.0 + 2.0 * (fallDistance - 3.0)
            } else {
                22.0 + fallDistance - 8.0
            }
            // TODO: doesn't account for getSmashDamagePerFallenBlock
            return dmg.toFloat()
        }

        private fun estimateDamage(a: MaceItemFacet): Double {
            val attackDamage = a.itemStack.attackDamage
            val attackSpeed = a.itemStack.attackSpeed

            val bigT = 20.0 / attackSpeed

            val probabilityAdjustmentFactor = P.pow(ceil(bigT * 0.9))

            val speedAdjustedDamage = attackDamage * attackSpeed * probabilityAdjustmentFactor.toFloat()
            val additionalFactor = DAMAGE_ESTIMATOR.estimateValue(a.itemStack)
            val bonusAttackDamage = getBonusAttackDamage(ASSUMED_FALL_DISTANCE)
            val total = speedAdjustedDamage + additionalFactor + bonusAttackDamage

            return total
        }
    }

    override fun compareTo(other: ItemFacet): Int {
        if (other !is MaceItemFacet) return 0
        val cmp = COMPARATOR.compare(this, other)
        return cmp
    }
}
