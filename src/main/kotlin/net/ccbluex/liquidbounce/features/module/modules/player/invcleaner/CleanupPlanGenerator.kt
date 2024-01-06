/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedItem
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class CleanupPlanGenerator(
    private val template: CleanupPlanPlacementTemplate,
    private val availableItems: List<ItemSlot>
) {
    /**
     * Items that have already been used. For example if already we used Inventory slot 12 as a sword, we cannot reuse
     * it as an axe in slot 2.
     */
    private val alreadyAllocatedItems: HashSet<ItemSlot> = HashSet()

    private val usefulItems: HashSet<ItemSlot> = HashSet()
    private val hotbarSwaps: ArrayList<InventorySwap> = ArrayList()

    // TODO Implement greedy check
    /**
     * Keeps track of where a specific type of item should be placed. e.g. BLOCK -> [Hotbar 7, Hotbar 8]
     */
    private val categoryToSlotsMap: Map<ItemCategory, List<ItemSlot>>
        = template.slotContentMap.entries
            .filter { (_, itemType) -> itemType.category != null }
            .groupBy { (_, itemType) -> itemType.category!! }
            .mapValues { (_, entries) -> entries.map { (slot, _) -> slot } }

    fun generatePlan(): InventoryCleanupPlan {
        // Contains all facets that the available items represent. i.e. if we have an axe in slot 5, this would be
        // (Axe(Slot 5), Sword(Slot 5)) since the axe can also function as a sword.
        val itemFacets = availableItems.flatMap { ItemCategorization.getItemFacets(it).asIterable() }

        // i.e. BLOCK -> [Block(Slot 5), Block(Slot 6)]
        val facetsGroupedByType = itemFacets.groupBy { it.category }

        for ((category, availableItems) in facetsGroupedByType.entries) {
            processItemCategory(category, availableItems)
        }

        return InventoryCleanupPlan(
            usefulItems = usefulItems,
            swaps = hotbarSwaps,
            mergeableItems = groupItemsByType()
        )
    }

    private fun processItemCategory(category: ItemCategory, availableItems: List<WeightedItem>) {
        val maxItemCount = if (category.type.allowOnlyOne) {
            1
        } else {
            template.itemLimitPerCategory[category] ?: Int.MAX_VALUE
        }

        val hotbarSlotsToFill = this.categoryToSlotsMap[category]

        // We need to fill all hotbar slots with this item type.

        // Use a descending sort order so that we can fill the slots with the best items first.
        val prioritizedItemList = availableItems.sortedDescending()

        markAndAllocateItems(
            itemsToFillIn = prioritizedItemList,
            hotbarSlotsToFill = hotbarSlotsToFill,
            maxItemCount = maxItemCount,
            requiredStackCount = hotbarSlotsToFill?.size ?: 0
        )
    }

    /**
     * Takes items from the [itemsToFillIn] list until it collected [maxItemCount] items is and [requiredStackCount]
     * stacks. The items are marked as useful and fills in hotbar slots if there are still slots to fill.
     */
    private fun markAndAllocateItems(
        itemsToFillIn: List<WeightedItem>,
        hotbarSlotsToFill: List<ItemSlot>?,
        maxItemCount: Int,
        requiredStackCount: Int
    ) {
        var currentStackCount = 0
        var currentItemCount = 0

        for (filledInItem in itemsToFillIn) {
            val maxCountReached = currentItemCount >= maxItemCount
            val allStacksFilled = currentStackCount >= requiredStackCount

            if (maxCountReached && allStacksFilled) {
                break
            }

            val filledInItemSlot = filledInItem.itemSlot

            // The item is already allocated and marked as useful, so we cannot use it again.
            if (filledInItemSlot in alreadyAllocatedItems) {
                continue
            }

            usefulItems.add(filledInItemSlot)

            // If we have a slot to fill...
            if (hotbarSlotsToFill != null && currentStackCount < hotbarSlotsToFill.size) {
                val hotbarSlotToFill = hotbarSlotsToFill[currentStackCount]

                // We don't need to move around equivalent items
                val areStacksSame = ItemStack.areEqual(
                    filledInItemSlot.itemStack,
                    hotbarSlotToFill.itemStack
                )

                if (filledInItemSlot != hotbarSlotToFill && !areStacksSame) {
                    hotbarSwaps.add(InventorySwap(filledInItemSlot, hotbarSlotToFill))
                }

                alreadyAllocatedItems.add(filledInItemSlot)
                alreadyAllocatedItems.add(hotbarSlotToFill)
            }

            currentItemCount += filledInItem.itemStack.count
            currentStackCount++
        }
    }

    fun groupItemsByType(): HashMap<ItemId, MutableList<ItemSlot>> {
        val itemsByType = HashMap<ItemId, MutableList<ItemSlot>>()

        for (availableSlot in this.availableItems) {
            val stack = availableSlot.itemStack

            if (stack.isNothing()) {
                continue
            }
            if (!stack.isStackable || stack.count >= stack.maxCount) {
                continue
            }

            val itemType = ItemId(stack.item, stack.nbt)
            val stacksOfType = itemsByType.computeIfAbsent(itemType) { mutableListOf() }

            stacksOfType.add(availableSlot)
        }

        return itemsByType
    }
}

class CleanupPlanPlacementTemplate(
    /**
     * Contains requests for each slot (e.g. Slot 1 -> SWORD, Slot 8 -> BLOCK, etc.)
     */
    val slotContentMap: Map<ItemSlot, ItemSortChoice>,
    /**
     * Contains an item limit for each category. e.g. BLOCK -> 128 will cause every stack above two to be thrown out.
     * If an item is not in this map, there is no limit.
     */
    val itemLimitPerCategory: Map<ItemCategory, Int>,
    /**
     * If false, slots which also contains items of that category, those items are not replaced with other items.
     */
    val isGreedy: Boolean
)

enum class ItemSlotType {
    HOTBAR,
    OFFHAND,
    ARMOR,
    INVENTORY,

    /**
     * e.g. chests
     */
    CONTAINER,
}
