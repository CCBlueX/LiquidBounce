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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemNumberConstraintEnforcer.SatisfactionStatus
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot

class CleanupPlanGenerator(private val template: CleanupPlanTemplate, private val availableItems: List<ItemSlot>) {
    private val wishOrganizer = WishOrganizer(this.template)
    private val constraintEnforcer = ItemNumberConstraintEnforcer(template)

    val plan: InventoryCleanupPlan

    init {
        val allItemFacets = discoverItemFacets()
        // All slots the cleaner may swap into other slots
        val availableItemFacets = allItemFacets.filter {
            this.template.restrictions.getRestrictionFor(it.itemSlot) < RestrictionType.FORBID_REPLACING
        }

        val itemDispenserRack = ItemDispenserRack(this.wishOrganizer, availableItemFacets)

        val usefulItems = HashSet<ItemSlot>()

        // Consider all slots that may not be touched at all as useful.
        usefulItems.addAll(template.restrictions.getSlotsWithAtLeast(RestrictionType.FORBID_TAMPERING))

        val swaps = generateSwaps(itemDispenserRack, usefulItems)

        findOtherUsefulItems(usefulItems, allItemFacets)

        this.plan = InventoryCleanupPlan(
            usefulItems = usefulItems,
            swaps = swaps,
            mergeableItems = groupItemsByType(),
        )
    }

    /**
     * This function marks all useful items that aren't filled into hotbar slots (i.e., arrows) as useful.
     */
    private fun findOtherUsefulItems(usefulItems: HashSet<ItemSlot>, allItemFacets: List<ItemFacet>) {
        val facetsGroupedByCategory = allItemFacets
            .groupBy { it.category }
            .entries
            .sortedBy { this.template.itemAmountConstraintProvider.getAllocationPriority(it.key) }

        for ((_, facetsInCategory) in facetsGroupedByCategory) {
            for (facet in facetsInCategory.sortedDescending()) {
                val satisfactionStatus = this.constraintEnforcer.getSatisfactionStatus(facet)

                when (satisfactionStatus) {
                    SatisfactionStatus.NOT_SATISFIED -> {
                        this.constraintEnforcer.addItem(facet)

                        usefulItems.add(facet.itemSlot)
                    }
                    SatisfactionStatus.SATISFIED -> {}
                    SatisfactionStatus.OVERSATURATED -> {
                        throw IllegalArgumentException("Oversaturated behavior is currently not implemented.")
                    }
                }
            }
        }
    }

    private fun generateSwaps(
        itemDispenserRack: ItemDispenserRack,
        usefulItems: HashSet<ItemSlot>
    ): ArrayList<InventorySwap> {
        val finishedSlots = HashSet<ItemSlot>()

        // Consider all slots that we aren't allowed to change as done.
        finishedSlots.addAll(template.restrictions.getSlotsWithAtLeast(RestrictionType.FORBID_REPLACING))

        val swaps: ArrayList<InventorySwap> = ArrayList()

        for (wish in this.wishOrganizer.organizedWishes) {
            // If a better wish was already fulfilled, skip this second wish.
            if (wish.targetSlot in finishedSlots) {
                continue
            }

            val availableItem = itemDispenserRack.nextItemForGroup(wish.id)

            if (availableItem == null) {
                continue
            }

            finishedSlots.add(wish.targetSlot)
            usefulItems.add(availableItem.itemSlot)

            // Move the item to the target slot if necessary.
            if (availableItem.itemSlot != wish.targetSlot) {
                swaps.add(
                    InventorySwap(
                        from = availableItem.itemSlot,
                        to = wish.targetSlot,
                        priority = availableItem.category.type.allocationPriority
                    )
                )
            }
        }
        return swaps
    }

    /**
     * Discovers all facets from [availableItems]. Filters out any slot that has been restricted
     */
    private fun discoverItemFacets(): List<ItemFacet> {
        val categorizer = ItemCategorization(availableItems)

        val availableItemFacets = availableItems.flatMap { categorizer.getItemFacets(it).asIterable() }

        return availableItemFacets
    }

    private fun groupItemsByType(): HashMap<ItemAndComponents, MutableList<ItemSlot>> {
        val itemsByType = HashMap<ItemAndComponents, MutableList<ItemSlot>>()

        for (availableSlot in this.availableItems) {
            val stack = availableSlot.itemStack

            if (stack.isEmpty) {
                continue
            }
            if (!stack.isStackable || stack.count >= stack.maxStackSize) {
                continue
            }

            val itemType = ItemAndComponents(stack)
            val stacksOfType = itemsByType.computeIfAbsent(itemType) { mutableListOf() }

            stacksOfType.add(availableSlot)
        }

        return itemsByType
    }
}
