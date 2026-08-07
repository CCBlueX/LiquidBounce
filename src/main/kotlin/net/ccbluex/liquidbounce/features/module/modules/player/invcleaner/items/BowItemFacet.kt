package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import it.unimi.dsi.fastutil.objects.ObjectIntPair
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

class BowItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        val VALUE_ESTIMATOR =
            EnchantmentValueEstimator(
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.POWER, 0.25f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.PUNCH, 0.33f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.FLAME, 1.25f * 0.9f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.INFINITY, 4.0f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.UNBREAKING, 0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.VANISHING_CURSE, -0.1f),
                EnchantmentValueEstimator.WeightedEnchantment(Enchantments.MENDING, 0.2f),
            )
        private val COMPARATOR =
            ComparatorChain<BowItemFacet>(
                compareBy { VALUE_ESTIMATOR.estimateValue(it.itemStack) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val providedItemFunctions: List<ProvidedFunction>
        get() = listOf(ProvidedFunction(ItemFunction.BOW_LIKE, 1))

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.BOW)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as BowItemFacet)
    }
}
