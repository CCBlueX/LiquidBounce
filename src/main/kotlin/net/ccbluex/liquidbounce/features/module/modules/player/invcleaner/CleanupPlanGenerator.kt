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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.isNothing

class CleanupPlanGenerator(
    private val template: CleanupPlanPlacementTemplate,
    private val availableItems: List<ItemSlot>,
) : ItemPacker.ItemAmountContraintProvider {
    private val hotbarSwaps: ArrayList<InventorySwap> = ArrayList()

    private val packer = ItemPacker()

    private val currentLimit = HashMap<ItemNumberContraintGroup, Int>()

    // TODO Implement greedy check
    /**
     * Keeps track of where a specific type of item should be placed. e.g. BLOCK -> [Hotbar 7, Hotbar 8]
     */
    private val categoryToSlotsMap: Map<ItemCategory, List<ItemSlot>> =
        template.slotContentMap.entries
            .filter { (_, itemType) -> itemType.category != null }
            .groupBy { (_, itemType) -> itemType.category!! }
            .mapValues { (_, entries) -> entries.map { (slot, _) -> slot } }

    fun generatePlan(): InventoryCleanupPlan {
        val categorizer = ItemCategorization(availableItems)

        // Contains all facets that the available items represent. i.e. if we have an axe in slot 5, this would be
        // (Axe(Slot 5), Weapon(Slot 5)) since the axe can also function as a weapon.
        val itemFacets = availableItems.flatMap { categorizer.getItemFacets(it).asIterable() }

        // i.e. BLOCK -> [Block(Slot 5), Block(Slot 6)]
        // Keep priority in mind (Tool slots are processed before weapon slots)
        val facetsGroupedByType =
            itemFacets
                .groupBy { it.category }
                .entries
                .sortedByDescending { it.key.type.allocationPriority }

        for ((category, availableItems) in facetsGroupedByType) {
            processItemCategory(category, availableItems)
        }

        // We aren't allowed to touch those, so we just consider them as useful.
        packer.usefulItems.addAll(this.template.forbiddenSlots)

        return InventoryCleanupPlan(
            usefulItems = packer.usefulItems,
            swaps = hotbarSwaps,
            mergeableItems = groupItemsByType(),
        )
    }

    private fun processItemCategory(
        category: ItemCategory,
        availableItems: List<ItemFacet>,
    ) {
        val hotbarSlotsToFill = this.categoryToSlotsMap[category]

        // We need to fill all hotbar slots with this item type.

        // Use a descending sort order so that we can fill the slots with the best items first.
        val prioritizedItemList = availableItems.sortedDescending()

        // Decide where the items should go.
        val requiredMoves =
            this.packer.packItems(
                itemsToFillIn = prioritizedItemList,
                hotbarSlotsToFill = hotbarSlotsToFill,
                contraintProvider = this,
                forbiddenSlots = this.template.forbiddenSlots,
                forbiddenSlotsToFill = this.template.forbiddenSlotsToFill
            )

        this.hotbarSwaps.addAll(requiredMoves)
    }

    private fun groupItemsByType(): HashMap<ItemId, MutableList<ItemSlot>> {
        val itemsByType = HashMap<ItemId, MutableList<ItemSlot>>()

        for (availableSlot in this.availableItems) {
            val stack = availableSlot.itemStack

            if (stack.isNothing()) {
                continue
            }
            if (!stack.isStackable || stack.count >= stack.maxCount) {
                continue
            }

            val itemType = ItemId(stack.item, stack.components)
            val stacksOfType = itemsByType.computeIfAbsent(itemType) { mutableListOf() }

            stacksOfType.add(availableSlot)
        }

        return itemsByType
    }

    override fun getSatisfactionStatus(item: ItemFacet): ItemPacker.ItemAmountContraintProvider.SatisfactionStatus {
        val constraints = this.template.itemAmountConstraintProvider(item)

        constraints.sortBy { it.group.priority }

        for (constraintInfo in constraints) {
            val currentCount = this.currentLimit[constraintInfo.group] ?: 0

            if (currentCount > constraintInfo.group.acceptableRange.last) {
                return ItemPacker.ItemAmountContraintProvider.SatisfactionStatus.OVERSATURATED
            } else if (currentCount < constraintInfo.group.acceptableRange.first) {
                return ItemPacker.ItemAmountContraintProvider.SatisfactionStatus.NOT_SATISFIED
            }
        }

        return ItemPacker.ItemAmountContraintProvider.SatisfactionStatus.SATISFIED
    }

    override fun addItem(item: ItemFacet) {
        val constraints = this.template.itemAmountConstraintProvider(item)

        for (constraintInfo in constraints) {
            val current = this.currentLimit.getOrDefault(constraintInfo.group, 0)

            this.currentLimit[constraintInfo.group] = current + constraintInfo.amountAddedByItem
        }
    }
}

class CleanupPlanPlacementTemplate(
    /**
     * Contains requests for each slot (e.g. Slot 1 -> SWORD, Slot 8 -> BLOCK, etc.)
     */
    val slotContentMap: Map<ItemSlot, ItemSortChoice>,
    /**
     * A function which provides constraint groups for each item category and the number which the item counts against
     * the given constraint. More info on how constraints work at [ItemNumberContraintGroup].
     */
    val itemAmountConstraintProvider: (ItemFacet) -> ArrayList<ItemConstraintInfo>,
    /**
     * If false, slots which also contains items of that category, those items are not replaced with other items.
     */
    val isGreedy: Boolean,
    val forbiddenSlots: Set<ItemSlot>,
    val forbiddenSlotsToFill: Set<ItemSlot>
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
