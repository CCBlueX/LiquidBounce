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
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.component.DataComponentTypes
import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.Enchantments
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack
import net.minecraft.registry.RegistryKey
import net.minecraft.util.math.MathHelper
import java.math.BigDecimal
import java.math.RoundingMode

class ArmorParameter(val defensePoints: Float, val toughness: Float)

class ArmorKitParameters(
    private val slots: Map<EquipmentSlot, ArmorParameter>
) {
    fun getParametersForSlot(slotType: EquipmentSlot) = this.slots[slotType]!!

    companion object {
        /**
         * Returns for each slot the summed up armor parameters without that slot.
         */
        fun getParametersForSlots(currentKit: Map<EquipmentSlot, ArmorPiece?>): ArmorKitParameters {
            // Sum up all parameters
            val totalArmorKitParameters =
                currentKit.values.fold(ArmorParameter(0.0F, 0.0F)) { acc, armorPiece ->
                    if (armorPiece != null) {
                        ArmorParameter(
                            acc.defensePoints + armorPiece.defensePoints,
                            acc.toughness + armorPiece.toughness
                        )
                    } else {
                        acc
                    }
                }

            // Return the parameter sum for each slot without the current slot
            return ArmorKitParameters(
                currentKit.mapValues { (_, armorPiece) ->
                    if (armorPiece != null) {
                        ArmorParameter(
                            totalArmorKitParameters.defensePoints - armorPiece.defensePoints,
                            totalArmorKitParameters.toughness - armorPiece.toughness
                        )
                    } else {
                        totalArmorKitParameters
                    }
                }
            )
        }
    }
}

/**
 * Compares armor pieces by their damage reduction.
 *
 * @property expectedDamage armor might have different damage reduction behaviour based on damage. Thus, the expected
 * damage has to be provided.
 * @property armorKitParametersForSlot armor (i.e. iron with Protection II vs plain diamond) behaves differently based
 * on the other armor pieces. Thus, the expected defense points and toughness have to be provided. Since those are
 * dependent on the other armor pieces, the armor parameters have to be provided slot-wise.
 * @property durabilityThreshold the minimum durability an armor piece must have to be prioritized for use.
 * If an armor piece's remaining durability is lower than this threshold,
 * the piece is not prioritized anymore, and it can be replaced with another piece
 * so that this piece can be preserved.
 */
class ArmorComparator(
    private val expectedDamage: Float,
    private val armorKitParametersForSlot: ArmorKitParameters,
    private val durabilityThreshold : Int = Int.MIN_VALUE
) : Comparator<ArmorPiece> {
    companion object {
        private val DAMAGE_REDUCTION_ENCHANTMENTS: Array<RegistryKey<Enchantment>> = arrayOf(
            Enchantments.PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.BLAST_PROTECTION
        )
        private val ENCHANTMENT_FACTORS = floatArrayOf(1.2f, 0.4f, 0.39f, 0.38f)
        private val ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = floatArrayOf(0.04f, 0.08f, 0.15f, 0.08f)
        private val OTHER_ENCHANTMENTS: Array<RegistryKey<Enchantment>> = arrayOf(
            Enchantments.FEATHER_FALLING,
            Enchantments.THORNS,
            Enchantments.RESPIRATION,
            Enchantments.AQUA_AFFINITY,
            Enchantments.UNBREAKING
        )
        private val OTHER_ENCHANTMENT_PER_LEVEL = floatArrayOf(3.0f, 1.0f, 0.1f, 0.05f, 0.01f)
    }

    private val comparator = ComparatorChain(
        compareBy { it.itemSlot.itemStack.durability > durabilityThreshold },
        compareByDescending { round(getThresholdedDamageReduction(it.itemSlot.itemStack).toDouble(), 3) },
        compareBy { round(getEnchantmentThreshold(it.itemSlot.itemStack).toDouble(), 3) },
        compareBy { it.itemSlot.itemStack.getEnchantmentCount() },
        compareBy { it.itemSlot.itemStack.get(DataComponentTypes.ENCHANTABLE)?.value ?: 0 },
        compareByCondition(ArmorPiece::isAlreadyEquipped),
        compareByCondition(ArmorPiece::isReachableByHand)
    )

    override fun compare(o1: ArmorPiece, o2: ArmorPiece): Int {
        return this.comparator.compare(o1, o2)
    }

    private fun getThresholdedDamageReduction(itemStack: ItemStack): Float {
        val item = itemStack.item as ArmorItem
        val parameters = this.armorKitParametersForSlot.getParametersForSlot(
            itemStack.get(DataComponentTypes.EQUIPPABLE)!!.slot
        )

        val material = item.material()
        return getDamageFactor(
            damage = expectedDamage,
            defensePoints = parameters.defensePoints + material.defense.getOrDefault(item.type(), 0),
            toughness = parameters.toughness + material.toughness
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
