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

import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : ClientModule("InventoryCleaner", Category.PLAYER,
    aliases = listOf("InventoryManager")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private val maxBlocks by int("MaximumBlocks", 512, 0..2500)
    private val maxArrows by int("MaximumArrows", 128, 0..2500)
    private val maxThrowables by int("MaximumThrowables", 64, 0..600)
    private val maxFoods by int("MaximumFoodPoints", 200, 0..2000)

    private val isGreedy by boolean("Greedy", true)

    private val offHandItem by enumChoice("OffHandItem", ItemSortChoice.SHIELD)
    private val slotItem1 by enumChoice("SlotItem-1", ItemSortChoice.WEAPON)
    private val slotItem2 by enumChoice("SlotItem-2", ItemSortChoice.BOW)
    private val slotItem3 by enumChoice("SlotItem-3", ItemSortChoice.PICKAXE)
    private val slotItem4 by enumChoice("SlotItem-4", ItemSortChoice.AXE)
    private val slotItem5 by enumChoice("SlotItem-5", ItemSortChoice.NONE)
    private val slotItem6 by enumChoice("SlotItem-6", ItemSortChoice.POTION)
    private val slotItem7 by enumChoice("SlotItem-7", ItemSortChoice.FOOD)
    private val slotItem8 by enumChoice("SlotItem-8", ItemSortChoice.BLOCK)
    private val slotItem9 by enumChoice("SlotItem-9", ItemSortChoice.BLOCK)

    val cleanupTemplateFromSettings: CleanupPlanPlacementTemplate
        get() {
            val slotTargets = hashMapOf<ItemSlot, ItemSortChoice>(
                Pair(OffHandSlot, offHandItem),
                Pair(Slots.Hotbar[0], slotItem1),
                Pair(Slots.Hotbar[1], slotItem2),
                Pair(Slots.Hotbar[2], slotItem3),
                Pair(Slots.Hotbar[3], slotItem4),
                Pair(Slots.Hotbar[4], slotItem5),
                Pair(Slots.Hotbar[5], slotItem6),
                Pair(Slots.Hotbar[6], slotItem7),
                Pair(Slots.Hotbar[7], slotItem8),
                Pair(Slots.Hotbar[8], slotItem9),
            )

            val forbiddenSlots = slotTargets
                .filterValues { it == ItemSortChoice.IGNORE }
                .keys.toHashSet()

            // Disallow tampering with armor slots since auto armor already handles them
            forbiddenSlots += Slots.Armor

            if (ModuleOffhand.isOperating()) {
                // Disallow tampering with off-hand slot when AutoTotem is active
                forbiddenSlots.add(OffHandSlot)
            }

            val forbiddenSlotsToFill = setOfNotNull(
                // Disallow tampering with off-hand slot when AutoTotem is active
                if (ModuleOffhand.isOperating()) OffHandSlot else null
            )

            val constraintProvider = AmountConstraintProvider(
                desiredItemsPerCategory = hashMapOf(
                    Pair(ItemSortChoice.BLOCK.category!!, maxBlocks),
                    Pair(ItemSortChoice.THROWABLES.category!!, maxThrowables),
                    Pair(ItemCategory(ItemType.ARROW, 0), maxArrows),
                ),
                desiredValuePerFunction = hashMapOf(
                    Pair(ItemFunction.FOOD, maxFoods),
                    Pair(ItemFunction.WEAPON_LIKE, 1),
                )
            )

            return CleanupPlanPlacementTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider::getConstraints,
                forbiddenSlots = forbiddenSlots,
                forbiddenSlotsToFill = forbiddenSlotsToFill,
                isGreedy = isGreedy,
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val currentInventorySlots = findNonEmptySlotsInInventory()
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, currentInventorySlots)
            .generatePlan()

        // Process inventory actions in priority order
        when {
            // Step 1: Prioritize hotbar swaps
            processHotbarSwaps(event, cleanupPlan) -> return@handler
            // Step 2: Merge stackable items to optimize space
            processStackMerging(event, cleanupPlan) -> return@handler
            // Step 3: Remove unwanted items (lowest priority)
            processItemDisposal(event, cleanupPlan, currentInventorySlots) -> return@handler
        }
    }

    /**
     * Handles swapping items to correct hotbar positions
     * @return true if a swap was scheduled, false otherwise
     */
    private fun processHotbarSwaps(event: ScheduleInventoryActionEvent, cleanupPlan: InventoryCleanupPlan): Boolean {
        val hotbarSwap = cleanupPlan.swaps.firstOrNull() ?: return false

        require(hotbarSwap.to is HotbarItemSlot) {
            "Invalid swap target: ${hotbarSwap.to}. Only hotbar slots are supported."
        }

        event.schedule(
            inventoryConstraints,
            InventoryAction.Click.performSwap(null, hotbarSwap.from, hotbarSwap.to)
        )

        return true
    }

    /**
     * Handles merging stackable items to optimize inventory space
     * @return true if a merge was scheduled, false otherwise
     */
    private fun processStackMerging(event: ScheduleInventoryActionEvent, cleanupPlan: InventoryCleanupPlan): Boolean {
        val stacksToMerge = cleanupPlan.findSlotsToMerge()
        val slotToMerge = stacksToMerge.firstOrNull() ?: return false

        // pickup -> pickup all -> pickup to handle remaining items
        event.schedule(
            inventoryConstraints,
            InventoryAction.Click.performMergeStack(slot = slotToMerge),
        )

        return true
    }

    /**
     * Handles disposal of unwanted items
     * @return true if an item was scheduled for disposal, false otherwise
     */
    private fun processItemDisposal(
        event: ScheduleInventoryActionEvent,
        cleanupPlan: InventoryCleanupPlan,
        currentInventorySlots: List<ItemSlot>
    ): Boolean {
        val itemsToDispose = cleanupPlan.findItemsToThrowOut(currentInventorySlots)
        val itemToThrow = itemsToDispose.firstOrNull() ?: return false

        event.schedule(
            inventoryConstraints,
            InventoryAction.Click.performThrow(screen = null, itemToThrow),
            Priority.NOT_IMPORTANT
        )

        return true
    }

    private class AmountConstraintProvider(
        val desiredItemsPerCategory: Map<ItemCategory, Int>,
        val desiredValuePerFunction: Map<ItemFunction, Int>,
    ) {
        fun getConstraints(facet: ItemFacet): ArrayList<ItemConstraintInfo> {
            val constraints = ArrayList<ItemConstraintInfo>()

            if (facet.providedItemFunctions.isEmpty()) {
                val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Integer.MAX_VALUE
                val desiredAmount = this.desiredItemsPerCategory.getOrDefault(facet.category, defaultDesiredAmount)

                val info = ItemConstraintInfo(
                    group = ItemCategoryConstraintGroup(
                        desiredAmount..Integer.MAX_VALUE,
                        10,
                        facet.category
                    ),
                    amountAddedByItem = facet.itemStack.count
                )

                constraints.add(info)
            } else {
                for ((function, amountAdded) in facet.providedItemFunctions) {
                    val info = ItemConstraintInfo(
                        group = ItemFunctionCategoryConstraintGroup(
                            desiredValuePerFunction.getOrDefault(function, 1)..Integer.MAX_VALUE,
                            10,
                            function
                        ),
                        amountAddedByItem = amountAdded
                    )

                    constraints.add(info)
                }
            }

            return constraints
        }
    }

}
