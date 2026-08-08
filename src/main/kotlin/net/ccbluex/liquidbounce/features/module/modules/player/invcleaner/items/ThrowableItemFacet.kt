package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PREFER_ITEMS_IN_HOTBAR
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.STABILIZE_COMPARISON
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.PreferStackSize
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.minecraft.world.item.FishingRodItem

class ThrowableItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<ThrowableItemFacet>(
                PreferStackSize.PREFER_FEWER.asHolderComparator(),
                PREFER_ITEMS_IN_HOTBAR,
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemCategory(GenericItemType.THROWABLE)

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as ThrowableItemFacet)
    }
}
