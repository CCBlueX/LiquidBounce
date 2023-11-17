package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

class WeightedBlockItem(itemSlot: ItemSlot) : WeightedItem(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<WeightedBlockItem>(
                { o1, o2 -> ModuleScaffold.BLOCK_COMPARATOR_FOR_INVENTORY.compare(o1.itemStack, o2.itemStack) },
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(ItemType.BLOCK, 0)

    override fun compareTo(other: WeightedItem): Int {
        return COMPARATOR.compare(this, other as WeightedBlockItem)
    }
}
