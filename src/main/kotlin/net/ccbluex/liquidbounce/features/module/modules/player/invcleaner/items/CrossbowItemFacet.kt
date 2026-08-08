package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.DEFAULT_TIE_BREAK
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemFunction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.EnchantmentValueEstimator
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.enchantment.Enchantments

class CrossbowItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
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
            @Suppress("SpreadOperator")
            ComparatorChain<CrossbowItemFacet>(
                VALUE_ESTIMATOR.asHolderComparator(),
                *DEFAULT_TIE_BREAK
            )
    }

    override val providedItemFunctions: List<ProvidedFunction>
        get() = listOf(ProvidedFunction(ItemFunction.BOW_LIKE, 1))

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.CROSSBOW)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as CrossbowItemFacet)
    }
}
