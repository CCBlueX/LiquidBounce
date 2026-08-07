package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

class ShieldItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        private val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.4f),
            )
        private val COMPARATOR =
            ComparatorChain<ShieldItemFacet>(
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.SHIELD)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as ShieldItemFacet)
    }
}
