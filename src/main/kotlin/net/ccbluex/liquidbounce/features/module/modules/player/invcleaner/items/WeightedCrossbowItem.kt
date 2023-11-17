package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.enchantment.Enchantments

class WeightedCrossbowItem(itemSlot: ItemSlot) : WeightedItem(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.QUICK_CHARGE, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MULTISHOT, 1.5f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.PIERCING, 1.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, 0.2f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.25f),
            )
        private val COMPARATOR =
            ComparatorChain<WeightedCrossbowItem>(
                { o1, o2 ->
                    (VALUE_ESTIMATOR.estimateValue(o1.itemStack)).compareTo(
                        VALUE_ESTIMATOR.estimateValue(o2.itemStack),
                    )
                },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.CROSSBOW, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedCrossbowItem)
    }
}
