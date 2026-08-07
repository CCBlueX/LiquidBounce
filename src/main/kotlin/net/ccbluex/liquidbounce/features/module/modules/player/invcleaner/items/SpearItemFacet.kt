package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.ccbluex.liquidbounce.utils.item.attackSpeed
import net.ccbluex.liquidbounce.utils.item.comparingEnchantmentLevel
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

/**
 * Specialization of weapon type. Used in order to allow the user to specify that they want a spear and not an axe
 * or something.
 */
class SpearItemFacet(itemSlot: ItemSlot) : WeaponItemFacet(itemSlot) {
    companion object {
        internal val COMPARING_LUNGE_AND_SPEED = comparingEnchantmentLevel(Enchantments.LUNGE).asHolderComparator()
            .thenComparingDouble { it.itemStack.attackSpeed }

        private val COMPARATOR_FOR_PIERCING_ATTACK =
            ComparatorChain<SpearItemFacet>(
                COMPARING_LUNGE_AND_SPEED.reversed(),
                SECONDARY_VALUE_ESTIMATOR.asHolderComparator(),
                PREFER_BETTER_DURABILITY,
                PREFER_ENCHANTABLE,
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.SPEAR)

}
