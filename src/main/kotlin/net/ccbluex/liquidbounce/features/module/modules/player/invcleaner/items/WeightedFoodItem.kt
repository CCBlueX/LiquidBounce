package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.Items

class WeightedFoodItem(itemSlot: ItemSlot) : WeightedItem(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<WeightedFoodItem>(
                { o1, o2 ->
                    compareByCondition(
                        o1,
                        o2,
                    ) { it.itemStack.item == Items.ENCHANTED_GOLDEN_APPLE }
                },
                { o1, o2 -> compareByCondition(o1, o2) { it.itemStack.item == Items.GOLDEN_APPLE } },
                { o1, o2 -> o1.itemStack.item.foodComponent!!.hunger.compareTo(o2.itemStack.item.foodComponent!!.hunger) },
                {
                        o1,
                        o2,
                    ->
                    o1.itemStack.item.foodComponent!!.saturationModifier.compareTo(o2.itemStack.item.foodComponent!!.saturationModifier)
                },
                { o1, o2 ->
                    o1.itemStack.count.compareTo(o2.itemStack.count)
                },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.FOOD, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedFoodItem)
    }
}
