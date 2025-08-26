package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemPacker.ItemAmountContraintProvider.SatisfactionStatus.OVERSATURATED
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemPacker.ItemAmountContraintProvider.SatisfactionStatus.SATISFIED
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.minecraft.item.ItemStack

/**
 * After discovery phase (find all items, group them by their type, sort them by usefulness), this class tries to fit
 * the given requirements (max blocks, required stack cound, etc.) and packs the given items in their target slots.
 *
 * Items that were deemed useful can be found in [usefulItems].
 */
class ItemPacker {
    /**
     * Items that have already been used. For example if already we used Inventory slot 12 as a sword, we cannot reuse
     * it as an axe in slot 2.
     */
    private val alreadyAllocatedItems: HashSet<ItemSlot> = HashSet()

    /**
     * If an item is used by a move, it will be in this list.
     */
    val usefulItems = HashSet<ItemSlot>()

    /**
     * Takes items from the [itemsToFillIn] list until it collected [maxItemCount] items is and [requiredStackCount]
     * stacks. The items are marked as useful and fills in hotbar slots if there are still slots to fill.
     *
     * @return returns the item moves ("swaps") that should to be executed.
     */
    fun packItems(
        itemsToFillIn: List<ItemFacet>,
        hotbarSlotsToFill: List<ItemSlot>?,
        forbiddenSlots: Set<ItemSlot>,
        forbiddenSlotsToFill: Set<ItemSlot>,
        contraintProvider: ItemAmountContraintProvider
    ): List<InventorySwap> {
        val moves = ArrayList<InventorySwap>()

        val requriedStackCount = hotbarSlotsToFill?.size ?: 0

        var currentStackCount = 0
        var currentItemCount = 0

        // The iterator of hotbar slots that still need filling.
        val leftHotbarSlotIterator = hotbarSlotsToFill?.iterator()

        for (filledInItem in itemsToFillIn) {
            val constraintsSatisfied = contraintProvider.getSatisfactionStatus(filledInItem)
            val allStacksFilled = currentStackCount >= requriedStackCount

            if (allStacksFilled && constraintsSatisfied == SATISFIED || constraintsSatisfied == OVERSATURATED) {
                continue
            }

            val filledInItemSlot = filledInItem.itemSlot

            // The item is already allocated and marked as useful, so we cannot use it again.
            if (filledInItemSlot in alreadyAllocatedItems) {
                continue
            }

            usefulItems.add(filledInItemSlot)

            contraintProvider.addItem(filledInItem)

            currentItemCount += filledInItem.itemStack.count
            currentStackCount++

            // Don't fill in the item if (a) there is no place for it to go or (b) we aren't allowed to touch it.
            if (leftHotbarSlotIterator == null || filledInItemSlot in forbiddenSlots) {
                continue
            }

            // Now find a fitting slot for the item.
            val targetSlot = fillItemIntoSlot(filledInItemSlot, leftHotbarSlotIterator)

            if (targetSlot != null && targetSlot !in forbiddenSlotsToFill) {
                moves.add(InventorySwap(filledInItemSlot, targetSlot, filledInItem.category.type.allocationPriority))
            }
        }

        // Keep items that should be kept
        itemsToFillIn.filter(ItemFacet::shouldKeep).forEach { this.usefulItems.add(it.itemSlot) }

        return moves
    }

    /**
     * Packs the given item into a good slot in the given target slots.
     *
     * @return the target slot that this item should be moved to, if a move should occur.
     */
    private fun fillItemIntoSlot(
        filledInItemSlot: ItemSlot,
        leftTargetSlotsToFill: Iterator<ItemSlot>,
    ): ItemSlot? {
        while (leftTargetSlotsToFill.hasNext()) {
            // Get the slots that still need to be filled if there are any (left/at all).

            val hotbarSlotToFill = leftTargetSlotsToFill.next()

            // We don't need to move around equivalent items
            val areStacksSame =
                ItemStack.areEqual(
                    filledInItemSlot.itemStack,
                    hotbarSlotToFill.itemStack,
                )

            when {
                // The item is already in the potential target slot, don't change anything about it.
                filledInItemSlot == hotbarSlotToFill -> {
                    // We mark the slot as used to prevent it being used for another slot.
                    alreadyAllocatedItems.add(hotbarSlotToFill)

                    return null
                }

                areStacksSame -> {
                    // We mark the slot as used to prevent it being used for another slot.
                    alreadyAllocatedItems.add(hotbarSlotToFill)

                    // Find a new slot for the item
                    continue
                }
                // A move should occur
                else -> {
                    // We will a swap. Both items have changed and should not be touched.
                    alreadyAllocatedItems.add(filledInItemSlot)
                    alreadyAllocatedItems.add(hotbarSlotToFill)

                    return hotbarSlotToFill
                }
            }
        }

        // We found no target slot
        return null
    }

    interface ItemAmountContraintProvider {
        fun getSatisfactionStatus(item: ItemFacet): SatisfactionStatus
        fun addItem(item: ItemFacet)

        enum class SatisfactionStatus {
            /**
             * Keep the item
             */
            NOT_SATISFIED,

            /**
             * The item is not needed - except for filling slots.
             */
            SATISFIED,

            /**
             * The item shouldn't be kept - even if there are still slots to fill.
             */
            OVERSATURATED,
        }
    }
}
