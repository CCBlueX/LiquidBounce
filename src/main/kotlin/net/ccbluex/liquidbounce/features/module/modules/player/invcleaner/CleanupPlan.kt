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
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.component.ComponentChanges
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.registry.Registries
import kotlin.math.ceil

@JvmRecord
data class InventorySwap(val from: ItemSlot, val to: ItemSlot, val priority: Priority)

/**
 * Represents the "id" of [ItemStack].
 * [ItemStack]s with same [Item] and [ComponentChanges] can be merged.
 */
@JvmRecord
data class ItemAndComponents(val item: Item, val componentChanges: ComponentChanges) {
    constructor(itemStack: ItemStack) : this(itemStack.item, itemStack.componentChanges)

    fun toItemStack(count: Int): ItemStack {
        val itemKey = Registries.ITEM.getEntry(item)
        return ItemStack(itemKey, count, componentChanges)
    }
}

class InventoryCleanupPlan(
    val usefulItems: MutableSet<ItemSlot>,
    val swaps: MutableList<InventorySwap>,
    val mergeableItems: MutableMap<ItemAndComponents, MutableList<ItemSlot>>,
) {
    /**
     * Replaces the slot from key to value
     */
    fun remapSlots(slotMap: Map<ItemSlot, ItemSlot>) {
        val usefulItemsToAdd = mutableSetOf<ItemSlot>()
        val usefulItemsToRemove = hashSetOf<ItemSlot>()

        for ((from, to) in slotMap) {
            if (from in usefulItems) {
                usefulItemsToRemove.add(from)
                usefulItemsToAdd.add(to)
            }
        }

        this.usefulItems.removeAll(usefulItemsToRemove)
        this.usefulItems.addAll(usefulItemsToAdd)

        this.swaps.replaceAll { hotbarSwap ->
            hotbarSwap.copy(
                from = slotMap[hotbarSwap.from] ?: hotbarSwap.from,
                to = slotMap[hotbarSwap.to] ?: hotbarSwap.to
            )
        }

        this.mergeableItems.values.forEach { mergeableItems ->
            mergeableItems.replaceAll { itemSlot ->
                slotMap[itemSlot] ?: itemSlot
            }
        }
    }

    fun findItemsToThrowOut(
        itemSlots: List<ItemSlot>,
    ) = itemSlots.filter { it !in usefulItems }

    /**
     * Find all item stack ids which should be double-clicked in order to merge them
     */
    fun findSlotsToMerge(): List<ItemSlot> {
        class MergeableStack(val slot: ItemSlot, var count: Int)

        fun canMerge(
            items: List<ItemSlot>,
            maxStackSize: Int,
        ): Boolean {
            val totalCount = items.sumOf { it.itemStack.count }

            val mergedStackCount = ceil(totalCount.toDouble() / maxStackSize).toInt()

            return items.size > mergedStackCount
        }

        fun MutableList<ItemSlot>.mergeStacks(
            stacks: ArrayDeque<MergeableStack>,
            maxStackSize: Int,
        ) {
            if (stacks.size <= 1) {
                return
            }

            // Remove
            while (stacks.isNotEmpty() && stacks.last().count + stacks[0].count > maxStackSize) {
                stacks.removeLast()
            }

            // Find the biggest stack that can be merged
            val itemToDbclick = stacks.removeLastOrNull() ?: return

            add(itemToDbclick.slot)

            var itemsToRemove = maxStackSize - itemToDbclick.count

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

            mergeStacks(stacks, maxStackSize)
        }

        val itemsToMerge = mutableListOf<ItemSlot>()

        for (mergeableItem in mergeableItems) {
            val maxStackSize = mergeableItem.key.item.maxCount

            if (!canMerge(mergeableItem.value, maxStackSize)) {
                continue
            }

            val stacks = mergeableItem.value.mapTo(ArrayDeque(mergeableItem.value.size)) {
                MergeableStack(it, it.itemStack.count)
            }
            stacks.sortBy { it.count }

            itemsToMerge.mergeStacks(stacks, maxStackSize)
        }

        return itemsToMerge
    }

}
