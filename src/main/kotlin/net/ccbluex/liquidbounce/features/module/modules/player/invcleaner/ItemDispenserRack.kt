/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */

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
