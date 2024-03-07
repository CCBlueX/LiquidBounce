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
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.MathHelper
import java.math.BigDecimal
import java.math.RoundingMode

class ArmorParameter(
    val defensePoints: Float,
    val toughness: Float
)

/**
 * Compares armor pieces by their damage reduction.
 *
 * @property expectedDamage armor might have different damage reduction behaviour based on damage. Thus, the expected
 * damage has to be provided.
 * @property armorParameterForSlot armor (i.e. iron with Protection II vs plain diamond) behaves differently based on
 * the other armor pieces. Thus, the expected defense points and toughness have to be provided. Since those are
 * dependent on the other armor pieces, the armor parameters have to be provided slot-wise.
 */
class ArmorComparator(
    private val expectedDamage: Float,
    private val armorParameterForSlot: Map<EquipmentSlot, ArmorParameter>
) : Comparator<ArmorPiece> {
    companion object {
        private val DAMAGE_REDUCTION_ENCHANTMENTS: Array<Enchantment> = arrayOf(
            Enchantments.PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.BLAST_PROTECTION
        )
        private val ENCHANTMENT_FACTORS = floatArrayOf(1.2f, 0.4f, 0.39f, 0.38f)
        private val ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = floatArrayOf(0.04f, 0.08f, 0.15f, 0.08f)
        private val OTHER_ENCHANTMENTS: Array<Enchantment> = arrayOf(
            Enchantments.FEATHER_FALLING,
            Enchantments.THORNS,
            Enchantments.RESPIRATION,
            Enchantments.AQUA_AFFINITY,
            Enchantments.UNBREAKING
        )
        private val OTHER_ENCHANTMENT_PER_LEVEL = floatArrayOf(3.0f, 1.0f, 0.1f, 0.05f, 0.01f)
    }

    private val comparator = ComparatorChain(
        compareByDescending { round(getThresholdedDamageReduction(it.itemSlot.itemStack).toDouble(), 3) },
        compareBy { round(getEnchantmentThreshold(it.itemSlot.itemStack).toDouble(), 3) },
        compareBy { it.itemSlot.itemStack.getEnchantmentCount() },
        compareBy { (it.itemSlot.itemStack.item as ArmorItem).enchantability },
        compareByCondition(ArmorPiece::isAlreadyEquipped),
        compareByCondition(ArmorPiece::isReachableByHand)
    )

    override fun compare(o1: ArmorPiece, o2: ArmorPiece): Int {
        return this.comparator.compare(o1, o2)
    }

    private fun getThresholdedDamageReduction(itemStack: ItemStack): Float {
        val item = itemStack.item as ArmorItem
        val parameters = this.armorParameterForSlot[item.slotType]!!

        return getDamageFactor(
            damage = expectedDamage,
            defensePoints = parameters.defensePoints + item.material.value().getProtection(item.type),
            toughness = parameters.toughness + item.material.value().toughness
        ) * (1 - getThresholdedEnchantmentDamageReduction(itemStack))
    }

    /**
     * Calculates the base damage factor (totalDamage = damage x damageFactor).
     *
     * See https://minecraft.fandom.com/wiki/Armor#Mechanics.
     *
     * @param damage the expected damage (the damage reduction depends on the dealt damage)
     */
    fun getDamageFactor(damage: Float, defensePoints: Float, toughness: Float): Float {
        val f = 2.0f + toughness / 4.0f
        val g = MathHelper.clamp(defensePoints - damage / f, defensePoints * 0.2f, 20.0f)

        return 1.0f - g / 25.0f
    }

    fun getThresholdedEnchantmentDamageReduction(itemStack: ItemStack): Float {
        var sum = 0.0f

        for (i in DAMAGE_REDUCTION_ENCHANTMENTS.indices) {
            val lvl = itemStack.getEnchantment(DAMAGE_REDUCTION_ENCHANTMENTS[i])

            sum += lvl * ENCHANTMENT_FACTORS[i] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[i]
        }

        return sum
    }

    private fun getEnchantmentThreshold(itemStack: ItemStack): Float {
        var sum = 0.0f

        for (i in OTHER_ENCHANTMENTS.indices) {
            sum += itemStack.getEnchantment(OTHER_ENCHANTMENTS[i]) * OTHER_ENCHANTMENT_PER_LEVEL[i]
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
