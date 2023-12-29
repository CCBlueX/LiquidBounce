package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

class WeightedPrimitiveItem(itemSlot: ItemSlot, override val category: ItemCategory, val worth: Int = 0) :
    WeightedItem(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<WeightedPrimitiveItem>(
                { o1, o2 -> o1.worth.compareTo(o2.worth) },
                { o1, o2 -> o1.itemStack.count.compareTo(o2.itemStack.count) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override fun compareTo(other: WeightedItem): Int = COMPARATOR.compare(this, other as WeightedPrimitiveItem)
}
