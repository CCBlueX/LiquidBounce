package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

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
