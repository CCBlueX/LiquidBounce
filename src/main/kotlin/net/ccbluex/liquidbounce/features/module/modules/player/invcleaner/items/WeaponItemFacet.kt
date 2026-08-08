package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.attackDamage
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.getEnchantment
import net.ccbluex.liquidbounce.utils.item.isSword
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.enchantment.Enchantments
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
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
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.2f),
            )
        val SECONDARY_VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.LOOTING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.05f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.SWEEPING_EDGE, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.KNOCKBACK, 0.25f),
            )
        private val COMPARATOR =
            ComparatorChain<WeaponItemFacet>(
                compareBy { estimateDamage(it.itemStack) },
                compareBy { SECONDARY_VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                compareBy { it.itemStack.isSword },
                PREFER_BETTER_DURABILITY,
                PREFER_ENCHANTABLE,
                *DEFAULT_TIE_BREAK
            )

        private fun estimateDamage(stack: ItemStack): Double {
            // Already contains damage enchantments like sharpness
            val attackDamage = stack.attackDamage
            val attackSpeed = stack.attackSpeed

            val p = 0.85.pow(1 / 20.0)
            val bigT = 20.0 / attackSpeed

            val probabilityAdjustmentFactor = p.pow(ceil(bigT * 0.9))

            val speedAdjustedDamage = attackDamage * attackSpeed * probabilityAdjustmentFactor.toFloat()

            val damageFromFireAspect = (stack.getEnchantment(Enchantments.FIRE_ASPECT) * 4.0f - 1)
                    .coerceAtLeast(0.0F) * 0.33F

            val additionalFactor = DAMAGE_ESTIMATOR.estimateValue(stack)

            return speedAdjustedDamage * (1.0 + additionalFactor) + damageFromFireAspect
        }

        /**
         * Only create a new instance if the item is useful.
         *
         * An item is useful as a weapon if it is better than fighting with nothing.
         */
        fun createIfUsefulAsWeapon(slot: ItemSlot): WeaponItemFacet? {
            if (!isBetterThanNothing(slot.itemStack)) {
                return null
            }

            return WeaponItemFacet(slot)
        }

        /**
         * Decides if this item is better than fighting with nothing.
         */
        private fun isBetterThanNothing(stack: ItemStack): Boolean {
            val baseDamage = estimateDamage(ItemStack(Items.STICK, 1))
            val itemDamage = estimateDamage(stack)

            return itemDamage > baseDamage || SECONDARY_VALUE_ESTIMATOR.estimateValue(stack) > 0.0F
        }
    }

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.WEAPON)

    override val providedItemFunctions: List<ProvidedFunction>
        get() = listOf(ProvidedFunction(ItemFunction.WEAPON_LIKE, 1))

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as WeaponItemFacet)
    }
}
