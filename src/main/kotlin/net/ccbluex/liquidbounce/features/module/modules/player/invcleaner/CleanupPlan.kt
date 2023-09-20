package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.WeightedItem
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.ItemStackWithSlot
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Item
import net.minecraft.item.ItemStack

class InventorySwap(val from: Int, val to: Int)

class InventoryCleanupPlan(
    val usefulItems: MutableSet<Int>,
    val hotbarSwaps: MutableList<InventorySwap>,
    val mergeableItems: HashMap<Item, MutableList<ItemStackWithSlot>>,
) {
    /**
     * Replaces the slot from key to value
     */
    fun remapSlots(slotMap: Map<Int, Int>) {
        val usefulItemsToAdd = mutableSetOf<Int>()
        val usefulItemsToRemove = mutableSetOf<Int>()

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

        this.hotbarSwaps.forEachIndexed { index, hotbarSwap ->
            val newSwap =
                InventorySwap(
                    slotMap[hotbarSwap.from] ?: hotbarSwap.from,
                    slotMap[hotbarSwap.to] ?: hotbarSwap.to,
                )

            this.hotbarSwaps[index] = newSwap
        }

        mergeableItems.values.forEach { mergeableItems ->
            mergeableItems.forEachIndexed { index, itemStackWithSlot ->
                val newStack = ItemStackWithSlot(
                    slotMap[itemStackWithSlot.slot] ?: itemStackWithSlot.slot,
                    itemStackWithSlot.itemStack
                )

                mergeableItems[index] = newStack
            }
        }
    }
}

/**
 * Contains the item categories and the corresponding item slots. e.g. BLOCK -> [Slot 8, Slot 9]
 */
private fun getCategorySlotsMap() =
    arrayOf(
        Pair(ModuleInventoryCleaner.offHandItem, 40),
        Pair(ModuleInventoryCleaner.slotItem1, 0),
        Pair(ModuleInventoryCleaner.slotItem2, 1),
        Pair(ModuleInventoryCleaner.slotItem3, 2),
        Pair(ModuleInventoryCleaner.slotItem4, 3),
        Pair(ModuleInventoryCleaner.slotItem5, 4),
        Pair(ModuleInventoryCleaner.slotItem6, 5),
        Pair(ModuleInventoryCleaner.slotItem7, 6),
        Pair(ModuleInventoryCleaner.slotItem8, 7),
        Pair(ModuleInventoryCleaner.slotItem9, 8),
    ).groupBy { it.first.category }

const val PLAYER_INVENTORY_SIZE = 41

/**
 * @param otherScreen An aditionally opened screen (i.e. chest). Items from this container will have their
 * slot number increased by [PLAYER_INVENTORY_SIZE] (i.e. Player Inventory Slot 5 -> Slot 5,
 * Chest inventory slot 5 -> Slot [PLAYER_INVENTORY_SIZE] + 5).
 */
fun createCleanupPlan(otherScreen: GenericContainerScreen? = null): InventoryCleanupPlan {
    val hotbarSlotMap = getCategorySlotsMap()

    val inventory = mc.player!!.inventory

    val usefulItems = hashSetOf<Int>()
    val itemsUsedInHotbar = mutableSetOf<Int>()
    val hotbarSwaps = mutableListOf<InventorySwap>()

    val mergeableItems = hashMapOf<Item, MutableList<ItemStackWithSlot>>()

    val items = mutableListOf<WeightedItem>()

    (0..40).forEach {
        val stack = inventory.getStack(it)

        ItemCategorization.categorizeItem(items, stack, it)
        updateMergableItems(mergeableItems, stack, it)
    }

    otherScreen?.screenHandler?.slots?.forEach {
        if (it.inventory === otherScreen.screenHandler.inventory) {
            val adjustedSlotId = it.id + PLAYER_INVENTORY_SIZE

            ItemCategorization.categorizeItem(items, it.stack, adjustedSlotId)
            updateMergableItems(mergeableItems, it.stack, adjustedSlotId)
        }
    }

    val groupedByItemCategory = items.groupBy { it.category }

    for ((key, value) in groupedByItemCategory) {
        val maxCount =
            when {
                key.type.allowOnlyOne -> 1
                key.type == ItemType.BLOCK -> ModuleInventoryCleaner.maxBlocks
                key.type == ItemType.ARROW -> ModuleInventoryCleaner.maxArrows
                else -> Int.MAX_VALUE
            }

        val hotbarSlotsToFill = hotbarSlotMap[key]

        var requiredStackCount = hotbarSlotsToFill?.size

        if (requiredStackCount == null) {
            requiredStackCount = 0
        }

        var currentStackCount = 0
        var currentItemCount = 0

        for (weightedItem in value.sortedDescending()) {
            val maxCountReached = currentItemCount >= maxCount
            val allStacksFilled = currentStackCount >= requiredStackCount

            if (maxCountReached && allStacksFilled) {
                break
            }

            usefulItems.add(weightedItem.slot)

            if (hotbarSlotsToFill != null && currentStackCount < hotbarSlotsToFill.size && weightedItem.slot !in itemsUsedInHotbar) {
                val hotbarSlotToFill = hotbarSlotsToFill[currentStackCount]

                val stack = inventory.getStack(hotbarSlotToFill.second)
                val alreadySatisfied = hotbarSlotToFill.first.satisfactionCheck?.invoke(stack) != true

                if ((ModuleInventoryCleaner.isGreedy || alreadySatisfied) && weightedItem.slot != hotbarSlotToFill.second) {
                    hotbarSwaps.add(InventorySwap(weightedItem.slot, hotbarSlotToFill.second))
                }

                itemsUsedInHotbar.add(weightedItem.slot)
            }

            currentItemCount += weightedItem.itemStack.count
            currentStackCount++
        }
    }

    return InventoryCleanupPlan(usefulItems, hotbarSwaps, mergeableItems)
}

fun updateMergableItems(
    mergeableItems: HashMap<Item, MutableList<ItemStackWithSlot>>,
    stack: ItemStack,
    slotId: Int,
) {
    if (stack.isNothing()) {
        return
    }
    if (!stack.isStackable || stack.count >= stack.maxCount) {
        return
    }

    val stacksOfType = mergeableItems.computeIfAbsent(stack.item) { mutableListOf() }

    stacksOfType.add(ItemStackWithSlot(slotId, stack))
}