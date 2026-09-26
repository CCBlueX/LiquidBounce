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

import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

class WishOrganizer(template: CleanupPlanTemplate) {
    val organizedWishes: List<OrganizedWish>
        field = ArrayList()
    val itemCategoryWishGroupMap: Map<ItemCategory, ArrayList<WishItemGroupId>>
        field = HashMap()

    companion object {
        /**
         * Decides which wish should come first. If wishA > wishB, wishA should be fulfilled first.
         */
        private val wishComparator = ComparatorChain<OrganizedWish>(
            compareByDescending { it.slotPriority },
            compareByDescending { it.indexInSlot },
            // Fill in specific items first.
            // The user expects this behavior.
            // For example, if there is a slot for golden apples and a slot for food, the user expects the
            // golden apple slot to contain golden apples and not the food slot.
            compareBy { it.wish.itemType == GenericItemType.ANY_ITEM },
            compareBy { it.wish.itemType.allocationPriority },
        )
    }

    init {
        // Deduplicate wishes for performance reasons.
        val wishIdMap = HashMap<CleanupPlanTemplate.SlotContentPreference, WishItemGroupId>()

        for ((slot, content) in template.slotContentMap.entries) {
            content.slotContentPreferences.forEachIndexed { wishIndexInSlot, wish ->
                val id = wishIdMap.computeIfAbsent(wish) { WishItemGroupId() }

                organizedWishes.add(
                    OrganizedWish(
                        id = id,
                        slotPriority = content.priority,
                        indexInSlot = wishIndexInSlot,
                        targetSlot = slot,
                        wish = wish
                    )
                )
            }
        }

        // Sort the wishes so that the wishes, which should be fulfilled first, are first.
        organizedWishes.sortWith(wishComparator.reversed())

        wishIdMap.forEach { (wish, itemGroupId) ->
            for (subtype in wish.subtypes) {
                val itemCategory = ItemCategory(wish.itemType, subtype)

                val wishItemGroups = itemCategoryWishGroupMap.computeIfAbsent(itemCategory) { ArrayList() }

                wishItemGroups.add(itemGroupId)
            }
        }
    }

    data class OrganizedWish(
        val id: WishItemGroupId,
        val targetSlot: ItemSlot,
        val slotPriority: Int,
        val indexInSlot: Int,
        val wish: CleanupPlanTemplate.SlotContentPreference
    )

    class WishItemGroupId
}
