/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.api.enums.EnchantmentType
import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.ccbluex.liquidbounce.utils.extensions.enchantmentCount
import net.ccbluex.liquidbounce.utils.extensions.getEnchantmentLevel
import java.io.Serializable
import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.max
import kotlin.math.min

class ArmorComparator : MinecraftInstance(), Comparator<ArmorPiece>, Serializable
{
    override fun compare(armor: ArmorPiece, otherArmor: ArmorPiece): Int
    {
        // For damage reduction it is better if it is smaller, so it has to be inverted
        // The decimal values have to be rounded since in double math equals is inaccurate
        // For example 1.03 - 0.41 = 0.6200000000000001 and (1.03 - 0.41) == 0.62 would be false
        val stack = armor.itemStack ?: throw IllegalArgumentException("armor.itemStack is null")
        val otherStack = otherArmor.itemStack ?: throw IllegalArgumentException("otherArmor.itemStack is null")

        val compare = round(getThresholdedDamageReduction(otherStack)).compareTo(round(getThresholdedDamageReduction(stack)))

        // If both armor pieces have the exact same damage, compare enchantments
        if (compare == 0)
        {
            val otherEnchantmentCmp = round(getEnchantmentThreshold(stack)).compareTo(round(getEnchantmentThreshold(otherStack)))

            // If both have the same enchantment threshold, prefer the item with more enchantments
            if (otherEnchantmentCmp == 0)
            {
                val enchantmentCountCmp = stack.enchantmentCount.compareTo(otherStack.enchantmentCount)
                if (enchantmentCountCmp != 0) return enchantmentCountCmp

                // Then durability...
                val o1a = stack.item?.asItemArmor() ?: return -1
                val o2a = otherStack.item?.asItemArmor() ?: return 1
                val durabilityCmp = o1a.armorMaterial.getDurability(o1a.armorType).compareTo(o2a.armorMaterial.getDurability(o2a.armorType))

                // Last comparision: Enchantability
                return if (durabilityCmp != 0) durabilityCmp else o1a.armorMaterial.enchantability.compareTo(o2a.armorMaterial.enchantability)
            }
            return otherEnchantmentCmp
        }
        return compare
    }

    companion object
    {
        /**
         * Damage-reduction Enchantments
         */
        private val DAMAGE_REDUCTION_ENCHANTMENTS = run {
            val provider = classProvider

            arrayOf(

                provider.getEnchantmentEnum(EnchantmentType.PROTECTION), // PROTECTION
                provider.getEnchantmentEnum(EnchantmentType.PROJECTILE_PROTECTION), // PROJECTILE_PROTECTION
                provider.getEnchantmentEnum(EnchantmentType.FIRE_PROTECTION), // FIRE_PROTECTION
                provider.getEnchantmentEnum(EnchantmentType.BLAST_PROTECTION) // BLAST_PROTECTION

            )
        }

        /**
         * Enchantments factor
         */
        private val ENCHANTMENT_FACTORS = floatArrayOf(

            1.5f,  // PROTECTION
            0.4f,  // PROJECTILE_PROTECTION
            0.39f,  // FIRE_PROTECTION
            0.38f // BLAST_PROTECTION

        )

        /**
         * Damage-reduction Enchantments factor
         */
        private val ENCHANTMENT_DAMAGE_REDUCTION_FACTOR = floatArrayOf(

            0.04f,  // PROTECTION
            0.08f,  // PROJECTILE_PROTECTION
            0.15f,  // FIRE_PROTECTION
            0.08f // BLAST_PROTECTION

        )

        /**
         * Other Enchantments
         */
        private val OTHER_ENCHANTMENTS = run {
            val provider = classProvider

            arrayOf(

                provider.getEnchantmentEnum(EnchantmentType.FEATHER_FALLING), //FEATHER_FALLING
                provider.getEnchantmentEnum(EnchantmentType.THORNS), // THORNS
                provider.getEnchantmentEnum(EnchantmentType.RESPIRATION), // RESPIRATION
                provider.getEnchantmentEnum(EnchantmentType.AQUA_AFFINITY), // AQUA_AFFINITY
                provider.getEnchantmentEnum(EnchantmentType.UNBREAKING) // UNBREAKING

            )
        }

        /**
         * Other Enchantments factor
         */
        private val OTHER_ENCHANTMENT_FACTORS = floatArrayOf(

            3.0f,  // FEATHER_FALLING
            1.0f,  // THORNS
            0.1f,  // RESPIRATION
            0.05f,  // AQUA_AFFINITY
            0.01f // UNBREAKING

        )

        /**
         * Rounds a double. From https://stackoverflow.com/a/2808648/9140494
         *
         * @param  value
         * the value to be rounded
         * @param  places
         * Decimal places
         * @return        The rounded value
         */
        private fun round(value: Float, places: Int = 3): Float
        {
            require(places >= 0) { "places" }

            return BigDecimal.valueOf(value.toDouble()).setScale(places, RoundingMode.HALF_UP).toFloat()
        }

        private fun getThresholdedDamageReduction(itemStack: IItemStack): Float
        {
            val item = itemStack.item?.asItemArmor()!!
            return getDamageReduction(item.armorMaterial.getDamageReductionAmount(item.armorType)) * (1 - getThresholdedEnchantmentDamageReduction(itemStack))
        }

        private fun getDamageReduction(defensePoints: Int, toughness: Int = 0): Float = 1 - min(20.0f, max(defensePoints * 0.2f, defensePoints - 1 / (2 + toughness * 0.25f))) * 0.04f

        private fun getThresholdedEnchantmentDamageReduction(itemStack: IItemStack): Float = DAMAGE_REDUCTION_ENCHANTMENTS.indices.map { itemStack.getEnchantmentLevel(DAMAGE_REDUCTION_ENCHANTMENTS[it]) * ENCHANTMENT_FACTORS[it] * ENCHANTMENT_DAMAGE_REDUCTION_FACTOR[it] }.sum()

        private fun getEnchantmentThreshold(itemStack: IItemStack): Float = OTHER_ENCHANTMENTS.indices.map { itemStack.getEnchantmentLevel(OTHER_ENCHANTMENTS[it]) * OTHER_ENCHANTMENT_FACTORS[it] }.sum()

        private const val serialVersionUID = 5242270904486038484L
    }
}
