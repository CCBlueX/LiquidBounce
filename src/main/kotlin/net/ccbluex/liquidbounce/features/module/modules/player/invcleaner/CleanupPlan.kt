package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.minecraft.item.Item
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement

class InventorySwap(val from: ItemSlot, val to: ItemSlot)

data class ItemId(val item: Item, val nbt: NbtCompound?)

class InventoryCleanupPlan(
    val usefulItems: MutableSet<ItemSlot>,
    val swaps: MutableList<InventorySwap>,
    val mergeableItems: HashMap<ItemId, MutableList<ItemSlot>>,
) {
    /**
     * Replaces the slot from key to value
     */
    fun remapSlots(slotMap: Map<ItemSlot, ItemSlot>) {
        val usefulItemsToAdd = mutableSetOf<ItemSlot>()
        val usefulItemsToRemove = mutableSetOf<ItemSlot>()

        for (entry in slotMap) {
            val from = entry.key
            val to = entry.value

            if (from in usefulItems) {
                usefulItemsToRemove.add(from)
                usefulItemsToAdd.add(to)
            }
        }

        this.usefulItems.removeAll(usefulItemsToRemove)
        this.usefulItems.addAll(usefulItemsToAdd)

        this.swaps.forEachIndexed { index, hotbarSwap ->
            val newSwap =
                InventorySwap(
                    slotMap[hotbarSwap.from] ?: hotbarSwap.from,
                    slotMap[hotbarSwap.to] ?: hotbarSwap.to,
                )

            this.swaps[index] = newSwap
        }

        mergeableItems.values.forEach { mergeableItems ->
            mergeableItems.forEachIndexed { index, itemSlot ->
                mergeableItems[index] = slotMap[itemSlot] ?: itemSlot
            }
        }
    }
}
