/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import kotlin.math.ceil

object ItemMerge {
    /**
     * Find all item stack ids which should be double-clicked in order to merge them
     */
    internal fun findStacksToMerge(cleanupPlan: InventoryCleanupPlan): List<ItemSlot> {
        val itemsToMerge = mutableListOf<ItemSlot>()

        for (mergeableItem in cleanupPlan.mergeableItems) {
            val maxStackSize = mergeableItem.key.item.maxCount

            if (!canMerge(mergeableItem.value, maxStackSize)) {
                continue
            }

            val stacks = mergeableItem.value.map { MergeableStack(it, it.itemStack.count) }

            mergeStacks(itemsToMerge, stacks.toMutableList(), maxStackSize)
        }

        return itemsToMerge
    }

    class MergeableStack(val slot: ItemSlot, var count: Int)

    private fun mergeStacks(
        itemsToDoubleclick: MutableList<ItemSlot>,
        stacks: MutableList<MergeableStack>,
        maxStackSize: Int,
    ) {
        if (stacks.size <= 1) {
            return
        }

        stacks.sortBy { it.count }

        // Remove
        while (stacks.isNotEmpty() && stacks.last().count + stacks[0].count > maxStackSize) {
            stacks.removeLast()
        }

        // Find the biggest stack that can be merged
        val itemToDoubleclick = stacks.removeLastOrNull() ?: return

        itemsToDoubleclick.add(itemToDoubleclick.slot)

        var itemsToRemove = maxStackSize - itemToDoubleclick.count

        // Remove all small stacks that have been removed by last merge
        while (itemsToRemove > 0 && stacks.isNotEmpty()) {
            val stack = stacks.first()

            val count = stack.count

            if (count < itemsToRemove) {
                stacks.removeFirst()
            } else {
                stack.count -= itemsToRemove
            }

            itemsToRemove -= stack.count
        }

        mergeStacks(itemsToDoubleclick, stacks, maxStackSize)
    }

    private fun canMerge(
        items: List<ItemSlot>,
        maxStackSize: Int,
    ): Boolean {
        val totalCount = items.sumOf { it.itemStack.count }

        val mergedStackCount = ceil(totalCount.toDouble() / maxStackSize).toInt()

        return items.size > mergedStackCount
    }
}
