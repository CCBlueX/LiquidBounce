/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import java.math.BigDecimal
import java.math.RoundingMode

object ArmorComparator : Comparator<ArmorPiece> {
    private val DAMAGE_REDUCTION_ENCHANTMENTS: Array<Enchantment> = arrayOf(
        Enchantments.PROTECTION,
        Enchantments.PROJECTILE_PROTECTION,
        Enchantments.FIRE_PROTECTION,
        Enchantments.BLAST_PROTECTION
    )
    private val ENCHANTMENT_FACTORS = floatArrayOf(1.5f, 0.4f, 0.39f, 0.38f)
    private val ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = floatArrayOf(0.04f, 0.08f, 0.15f, 0.08f)
    private val OTHER_ENCHANTMENTS: Array<Enchantment> = arrayOf(
        Enchantments.FEATHER_FALLING,
        Enchantments.THORNS,
        Enchantments.RESPIRATION,
        Enchantments.AQUA_AFFINITY,
        Enchantments.UNBREAKING
    )
    private val OTHER_ENCHANTMENT_FACTORS = floatArrayOf(3.0f, 1.0f, 0.1f, 0.05f, 0.01f)

    override fun compare(o1: ArmorPiece, o2: ArmorPiece): Int {
        val o1ItemStack = o1.itemSlot.itemStack
        val o2ItemStack = o2.itemSlot.itemStack

        // For damage reduction it is better if it is smaller, so it has to be inverted
        // The decimal values have to be rounded since in double math equals is inaccurate
        // For example 1.03 - 0.41 = 0.6200000000000001 and (1.03 - 0.41) == 0.62 would be false
        val compare = round(getThresholdedDamageReduction(o2ItemStack).toDouble(), 3).compareTo(
            round(
                getThresholdedDamageReduction(o1ItemStack).toDouble(),
                3
            )
        )

        // If both armor pieces have the exact same damage, compare enchantments
        if (compare == 0) {
            val otherEnchantmentCmp = round(
                getEnchantmentThreshold(o1ItemStack).toDouble(),
                3
            ).compareTo(round(getEnchantmentThreshold(o2ItemStack).toDouble(), 3))

            // If both have the same enchantment threshold, prefer the item with more enchantments
            if (otherEnchantmentCmp == 0) {
                val enchantmentCountCmp =
                    o1ItemStack.getEnchantmentCount().compareTo(o2ItemStack.getEnchantmentCount())

                if (enchantmentCountCmp != 0) return enchantmentCountCmp

                // Then durability...
                val o1a = o1ItemStack.item as ArmorItem
                val o2a = o2ItemStack.item as ArmorItem

                val durabilityCmp =
                    o1a.material.getDurability(o1a.type).compareTo(o2a.material.getDurability(o2a.type))

                if (durabilityCmp != 0) {
                    return durabilityCmp
                }

                // Last comparision: Enchantability
                val enchantabilityCmp = o1a.material.enchantability.compareTo(o2a.material.enchantability)

                if (enchantabilityCmp != 0) {
                    return enchantabilityCmp
                }

                val alreadyEquippedCmp = compareByCondition(o1, o2, ArmorPiece::isAlreadyEquipped)

                if (alreadyEquippedCmp != 0) {
                    return alreadyEquippedCmp
                }

                return compareByCondition(o1, o2, ArmorPiece::isReachableByHand)
            }
            return otherEnchantmentCmp
        }
        return compare
    }

    private fun getThresholdedDamageReduction(itemStack: ItemStack): Float {
        val item = itemStack.item as ArmorItem

        return getDamageReduction(
            item.material.getProtection(item.type),
            0
        ) * (1 - getThresholdedEnchantmentDamageReduction(itemStack))
    }

    private fun getDamageReduction(defensePoints: Int, toughness: Int): Float {
        return 1 - 20.0f.coerceAtMost((defensePoints / 5.0f).coerceAtLeast(defensePoints - 1 / (2 + toughness / 4.0f))) / 25.0f
    }

    private fun getThresholdedEnchantmentDamageReduction(itemStack: ItemStack): Float {
        var sum = 0.0f

        for (i in DAMAGE_REDUCTION_ENCHANTMENTS.indices) {
            sum += itemStack.getEnchantment(DAMAGE_REDUCTION_ENCHANTMENTS[i]) * ENCHANTMENT_FACTORS[i] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[i]
        }

        return sum
    }

    private fun getEnchantmentThreshold(itemStack: ItemStack): Float {
        var sum = 0.0f

        for (i in OTHER_ENCHANTMENTS.indices) {
            sum += itemStack.getEnchantment(OTHER_ENCHANTMENTS[i]) * OTHER_ENCHANTMENT_FACTORS[i]
        }

        return sum
    }

    /**
     * Rounds a double. From https://stackoverflow.com/a/2808648/9140494
     *
     * @param value  the value to be rounded
     * @param places Decimal places
     * @return The rounded value
     */
    fun round(value: Double, places: Int): Double {
        require(places >= 0)

        var bd = BigDecimal.valueOf(value)
        bd = bd.setScale(places, RoundingMode.HALF_UP)

        return bd.toDouble()
    }

}
