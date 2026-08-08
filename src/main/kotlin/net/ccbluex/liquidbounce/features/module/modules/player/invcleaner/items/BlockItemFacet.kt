package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

class BlockItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<BlockItemFacet>(
                ModuleScaffold.BLOCK_COMPARATOR_FOR_INVENTORY.asHolderComparator(),
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.BLOCK)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as BlockItemFacet)
    }
}
