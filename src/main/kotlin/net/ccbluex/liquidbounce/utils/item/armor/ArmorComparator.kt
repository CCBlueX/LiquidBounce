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
package net.ccbluex.liquidbounce.utils.item.armor

import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.liquidbounce.utils.math.roundToDecimalPlaces
import net.ccbluex.liquidbounce.utils.item.armorToughness
import net.ccbluex.liquidbounce.utils.item.armorValue
import net.ccbluex.liquidbounce.utils.item.durability
import net.ccbluex.liquidbounce.utils.item.equipmentSlot
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.getEnchantmentCount
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.core.component.DataComponents
import net.minecraft.resources.ResourceKey
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Decides how [ArmorComparator] ranks armor pieces.
 */
enum class ArmorComparatorMode(override val tag: String) : Tagged {
    /**
     * The original, damage-model based ranking. It weighs every damage-reduction enchantment
     * (Protection, Projectile-, Fire- and Blast Protection) into a single reduction value, using the
     * toughness-aware 1.9+ damage formula. Kept for backwards compatibility and modern survival.
     */
    SMART("Smart"),

    /**
     * SkyWars/BedWars oriented ranking built on the legacy (1.8) armor model where one armor point is a
     * flat 4% reduction with no toughness and no damage-strength scaling. Only relevant enchantments
     * influence the choice (Protection always, Projectile Protection optionally). Niche enchantments
     * (Fire/Blast Protection, Thorns, Unbreaking, ...) never out-weigh real defense - they only act as a
     * final tie-breaker. This stops the module from picking, say, iron with high Fire Protection over
     * plain diamond.
     */
    RAW_DEFENSE("RawDefense")
}

@JvmRecord
data class ArmorParameter(val defensePoints: Float, val toughness: Float) {
    companion object {
        @JvmField
        val ZERO = ArmorParameter(0f, 0f)
    }
}

@JvmInline
value class ArmorKitParameters private constructor(
    private val slots: Map<EquipmentSlot, ArmorParameter>
) {
    fun getParametersForSlot(slotType: EquipmentSlot) = this.slots[slotType]!!

    companion object {
        /**
         * Returns for each slot the summed up armor parameters without that slot.
         */
        @JvmStatic
        fun getParametersForSlots(currentKit: Map<EquipmentSlot, ArmorPiece?>): ArmorKitParameters {
            // Sum up all parameters
            val totalArmorKitParameters =
                currentKit.values.fold(ArmorParameter.ZERO) { acc, armorPiece ->
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
                currentKit.mapValuesTo(enumMapOf()) { (_, armorPiece) ->
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
    private val durabilityThreshold : Int = Int.MIN_VALUE,
    private val mode: ArmorComparatorMode = ArmorComparatorMode.SMART,
    private val considerProjectileProtection: Boolean = true
) : Comparator<ArmorPiece> {
    companion object {
        private val DAMAGE_REDUCTION_ENCHANTMENTS: Array<ResourceKey<Enchantment>> = arrayOf(
            Enchantments.PROTECTION,
            Enchantments.PROJECTILE_PROTECTION,
            Enchantments.FIRE_PROTECTION,
            Enchantments.BLAST_PROTECTION
        )
        private val ENCHANTMENT_FACTORS = floatArrayOf(1.2f, 0.4f, 0.39f, 0.38f)
        private val ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = floatArrayOf(0.04f, 0.08f, 0.15f, 0.08f)
        private val OTHER_ENCHANTMENTS: Array<ResourceKey<Enchantment>> = arrayOf(
            Enchantments.FEATHER_FALLING,
            Enchantments.THORNS,
            Enchantments.RESPIRATION,
            Enchantments.AQUA_AFFINITY,
            Enchantments.UNBREAKING
        )
        private val OTHER_ENCHANTMENT_PER_LEVEL = floatArrayOf(3.0f, 1.0f, 0.1f, 0.05f, 0.01f)

        /**
         * Legacy (1.8) armor model: one armor point reduces incoming damage by a flat 4%, capped at 80%.
         * There is no toughness and no damage-strength scaling.
         */
        private const val LEGACY_REDUCTION_PER_POINT = 0.04f
        private const val LEGACY_REDUCTION_CAP = 0.8f

        /**
         * Effective Protection Factor (EPF) modifiers used by the legacy enchantment-protection formula.
         * Protection is weak per level, Projectile Protection has a higher modifier.
         * See https://minecraft.wiki/w/Armor#Enchantments (pre-1.9 mechanics).
         */
        private const val PROTECTION_EPF_MODIFIER = 0.75f
        private const val PROJECTILE_PROTECTION_EPF_MODIFIER = 1.5f
        private const val EPF_REDUCTION_PER_POINT = 0.04f
    }

    private val comparator = when (mode) {
        ArmorComparatorMode.SMART -> smartComparator()
        ArmorComparatorMode.RAW_DEFENSE -> rawDefenseComparator()
    }

    override fun compare(o1: ArmorPiece, o2: ArmorPiece): Int {
        return this.comparator.compare(o1, o2)
    }

    private fun smartComparator() = ComparatorChain(
        compareBy { it.itemSlot.itemStack.durability > durabilityThreshold },
        compareByDescending { getThresholdedDamageReduction(it.itemSlot.itemStack).roundToDecimalPlaces(3) },
        compareBy { getEnchantmentThreshold(it.itemSlot.itemStack).roundToDecimalPlaces(3) },
        compareBy { it.itemSlot.itemStack.getEnchantmentCount() },
        compareBy { it.itemSlot.itemStack.get(DataComponents.ENCHANTABLE)?.value ?: 0 },
        compareByCondition(ArmorPiece::isAlreadyEquipped),
        compareByCondition(ArmorPiece::isReachableByHand)
    )

    /**
     * SkyWars/BedWars oriented ranking. The dominating criterion is the legacy total damage reduction
     * (raw armor points plus only the relevant protection enchantments). Niche enchantments never out-rank
     * real defense - they merely break ties.
     */
    private fun rawDefenseComparator() = ComparatorChain(
        compareBy { it.itemSlot.itemStack.durability > durabilityThreshold },
        // maxWithOrNull picks the greatest element, so higher reduction must compare as greater (ascending).
        compareBy { getLegacyDamageReduction(it.itemSlot.itemStack).roundToDecimalPlaces(4) },
        compareBy { it.itemSlot.itemStack.getEnchantment(Enchantments.PROTECTION) },
        compareBy { getEnchantmentThreshold(it.itemSlot.itemStack).roundToDecimalPlaces(3) },
        compareBy { it.itemSlot.itemStack.getEnchantmentCount() },
        compareBy { it.itemSlot.itemStack.get(DataComponents.ENCHANTABLE)?.value ?: 0 },
        compareByCondition(ArmorPiece::isAlreadyEquipped),
        compareByCondition(ArmorPiece::isReachableByHand)
    )

    /**
     * Total damage reduction of a piece (together with the rest of the kit) under the legacy 1.8 model.
     *
     * Armor points give a flat 4% each (capped at 80%). On top of that only [Enchantments.PROTECTION] and -
     * when [considerProjectileProtection] is enabled - [Enchantments.PROJECTILE_PROTECTION] are applied to the
     * remaining damage. Fire/Blast Protection and any non-defensive enchantments are ignored on purpose so they
     * can never beat actual armor points.
     */
    fun getLegacyDamageReduction(itemStack: ItemStack): Float {
        val parameters = this.armorKitParametersForSlot.getParametersForSlot(itemStack.equipmentSlot!!)
        val totalArmorPoints = parameters.defensePoints + itemStack.armorValue!!.toFloat()

        val armorReduction = (totalArmorPoints * LEGACY_REDUCTION_PER_POINT).coerceAtMost(LEGACY_REDUCTION_CAP)

        val protectionLevel = itemStack.getEnchantment(Enchantments.PROTECTION)
        var enchantReduction = legacyEpfReduction(protectionLevel, PROTECTION_EPF_MODIFIER)

        if (considerProjectileProtection) {
            val projectileLevel = itemStack.getEnchantment(Enchantments.PROJECTILE_PROTECTION)
            // Combine multiplicatively: protection enchantments stack on the remaining damage.
            val projectileReduction = legacyEpfReduction(projectileLevel, PROJECTILE_PROTECTION_EPF_MODIFIER)
            enchantReduction = 1f - (1f - enchantReduction) * (1f - projectileReduction)
        }

        // Enchantments apply to the damage that survives the armor points, capped at 80% total.
        val total = 1f - (1f - armorReduction) * (1f - enchantReduction)

        return total.coerceAtMost(LEGACY_REDUCTION_CAP)
    }

    private fun legacyEpfReduction(level: Int, modifier: Float): Float {
        if (level <= 0) {
            return 0f
        }

        val epf = Math.floor(((6 + level * level) * modifier / 3.0)).toInt()

        return (epf * EPF_REDUCTION_PER_POINT).coerceAtMost(LEGACY_REDUCTION_CAP)
    }

    private fun getThresholdedDamageReduction(itemStack: ItemStack): Float {
        val parameters = this.armorKitParametersForSlot.getParametersForSlot(itemStack.equipmentSlot!!)

        return getDamageFactor(
            damage = expectedDamage,
            defensePoints = parameters.defensePoints + itemStack.armorValue!!.toFloat(),
            toughness = parameters.toughness + itemStack.armorToughness!!.toFloat()
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
        val g = Math.clamp(defensePoints - damage / f, defensePoints * 0.2f, 20.0f)

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

}
