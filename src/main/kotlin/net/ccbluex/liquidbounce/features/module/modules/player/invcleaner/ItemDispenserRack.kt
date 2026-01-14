package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot

class ItemDispenserRack(wishOrganizer: WishOrganizer, itemFacets: List<ItemFacet>) {
    private val dispensersForType: Map<WishOrganizer.WishItemGroupId, ItemDispenser>
    private val alreadyDispensedItemSlots = HashSet<ItemSlot>()

    init {
        val wishGroupAvailableFacetMap = HashMap<WishOrganizer.WishItemGroupId, ArrayList<ItemFacet>>()

        for (facet in itemFacets) {
            val wishGroupsForFacet = wishOrganizer.itemCategoryWishGroupMap[facet.category] ?: continue

            for (id in wishGroupsForFacet) {
                wishGroupAvailableFacetMap.computeIfAbsent(id) { ArrayList() }.add(facet)
            }
        }

        wishGroupAvailableFacetMap.values.forEach { facetList -> facetList.sortDescending() }

        this.dispensersForType = wishGroupAvailableFacetMap.mapValues { ItemDispenser(it.value) }
    }

    fun nextItemForGroup(id: WishOrganizer.WishItemGroupId) = this.dispensersForType[id]?.nextItem()

    private inner class ItemDispenser(itemList: List<ItemFacet>) {
        private val itemListIterable: Iterator<ItemFacet> = itemList.iterator()

        fun nextItem(): ItemFacet? {
            while (this.itemListIterable.hasNext()) {
                val currentItem = this.itemListIterable.next()

                // Check if this item slot has already been dispensed.
                // This is possible as an item might appear in multiple dispensers.
                if (alreadyDispensedItemSlots.add(currentItem.itemSlot)) {
                    return currentItem
                }
            }

            return null
        }
    }
}
