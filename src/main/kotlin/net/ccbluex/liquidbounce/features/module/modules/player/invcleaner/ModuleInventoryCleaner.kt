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

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.kotlin.Priority

/**
 * InventoryManager module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : ClientModule(
    name = "InventoryCleaner",
    category = ModuleCategories.PLAYER,
    aliases = listOf("InventoryManager")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    @Suppress("unused")
    private val inventoryPresets by inventoryPreset()

    val cleanupTemplateFromSettings: CleanupPlanTemplate
        get() {
            val specifiedSlotTargets = this.inventoryPresets.items
            val currentRestrictionMap = hashMapOf<ItemSlot, RestrictionType>()

            val mapped = specifiedSlotTargets
                .map { (slot, choice) ->
                    val wishes = choice.mapNotNull {
                        val representation = it.toBackendRepresentation()

                        currentRestrictionMap.compute(slot) { _, b ->
                            maxOf(b ?: RestrictionType.NONE, representation.slotRestriction)
                        }

                        representation.contentPreference
                    }

                    slot to CleanupPlanSlotContent(wishes, 0)
                }
                .toTypedArray()

            val slotTargets = hashMapOf<ItemSlot, CleanupPlanSlotContent>(pairs = mapped)


            // Disallow tampering with armor slots since auto armor already handles them
            Slots.Armor.forEach { currentRestrictionMap[it] = RestrictionType.FORBID_TAMPERING }

            if (ModuleOffhand.isOperating()) {
                // Disallow tampering with off-hand slot when AutoTotem is active
                currentRestrictionMap[OffHandSlot] = RestrictionType.FORBID_REPLACING
            }

            val desiredItemCounts = this.inventoryPresets.itemLimitRules.map { rule ->
                val converted = rule.items
                    .mapNotNull { item -> item.toBackendRepresentation().contentPreference }
                    .flatMap { preference ->
                        preference.subtypes.map { ItemCategory(preference.itemType, it) }
                    }

                converted to rule.itemCount
            }

            val constraintProvider = AmountItemAmountConstraintProvider(
                desiredValuePerFunction = hashMapOf(),
                desiredItemsInSpecificCategories = desiredItemCounts
            )


            return CleanupPlanTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider,
                restrictions = CleanupPlanRestrictions(currentRestrictionMap)
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val cleanupPlan = CleanupPlanGenerator(
            cleanupTemplateFromSettings,
            findNonEmptySlotsInInventory()
        ).plan

        // Step 1: Move items to the correct slots
        for (hotbarSwap in cleanupPlan.swaps) {
            check(hotbarSwap.to is HotbarItemSlot) { "Cannot swap to non-hotbar-slot" }

            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performSwap(null, hotbarSwap.from, hotbarSwap.to)
            )

            // todo: run when successful or do not care?
            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to),
                    Pair(hotbarSwap.to, hotbarSwap.from),
                )
            )
        }

        // Step 2: Merge stacks
        val stacksToMerge = ItemMerge.findStacksToMerge(cleanupPlan)
        for (slot in stacksToMerge) {
            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performPickup(null, slot),
                InventoryAction.Click.performPickupAll(null, slot),
                InventoryAction.Click.performPickup(null, slot),
            )
        }

        // It is important that we call findItemSlotsInInventory() here again, because the inventory has changed.
        val itemsToThrowOut = findItemsToThrowOut(cleanupPlan, findNonEmptySlotsInInventory())

        for (slot in itemsToThrowOut) {
            event.schedule(
                inventoryConstraints,
                InventoryAction.Click.performThrow(screen = null, slot),
                Priority.NOT_IMPORTANT
            )
        }
    }

    fun findItemsToThrowOut(
        cleanupPlan: InventoryCleanupPlan,
        itemsInInv: List<ItemSlot>,
    ) = itemsInInv.filter { it !in cleanupPlan.usefulItems }

    private class AmountItemAmountConstraintProvider(
        val desiredValuePerFunction: Map<ItemFunction, Int>,
        /**
         * Contains information about specific item groups constraints like `[snowball, egg] -> 32`.
         * In that example, the inventory cleaner would not start throwing out items until at least 32 items of
         * snowballs or eggs are in the inventory.
         */
        desiredItemsInSpecificCategories: List<Pair<List<ItemCategory>, Int>>
    ) : ItemAmountConstraintProvider {
        /**
         * Contains all specific item groups in which an item is.
         *
         * For these rules: `[egg, snowball] -> 32, [egg, carrot] -> 64`, this list would look like this:
         * - `egg` -> `[0, 1]`
         * - `snowball` -> `[0]`
         * - `carrot` -> `[1]`
         */
        private val itemSpecificGroupMap: Map<ItemCategory, List<SpecificItemGroup>> = run {
            desiredItemsInSpecificCategories
                .flatMapIndexed { idx, (items, desiredAmount) ->
                    val group = SpecificItemGroup(id = idx, desiredAmount = desiredAmount, priority = idx)

                    items.map { it to group }
                }
                .groupBy { it.first }
                .mapValues { list -> list.value.map { it.second } }
        }

        override fun getConstraints(facet: ItemFacet): ArrayList<ItemConstraintInfo> {
            val constraints = ArrayList<ItemConstraintInfo>()

            for (group in this.itemSpecificGroupMap.getOrDefault(facet.category, emptyList())) {
                val info = ItemConstraintInfo(
                    group = SpecificItemGroupConstraintGroup(
                        acceptableRange = group.desiredAmount..Integer.MAX_VALUE,
                        priority = group.priority,
                        groupId = group.id
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = false
                )

                constraints.add(info)
            }

            for ((function, amountAdded) in facet.providedItemFunctions) {
                val configuredDesiredAmount = desiredValuePerFunction[function]

                val (default, desiredAmount) = if (configuredDesiredAmount != null) {
                    false to configuredDesiredAmount
                } else {
                    true to 1
                }

                val info = ItemConstraintInfo(
                    group = ItemFunctionCategoryConstraintGroup(
                        desiredAmount..Integer.MAX_VALUE,
                        1000,
                        function
                    ),
                    amountAddedByItem = amountAdded,
                    default = default
                )

                constraints.add(info)
            }

            if (facet.providedItemFunctions.isEmpty() && facet.category.type != GenericItemType.ANY_ITEM) {
                val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Integer.MAX_VALUE

                val info = ItemConstraintInfo(
                    group = ItemCategoryConstraintGroup(
                        defaultDesiredAmount..Integer.MAX_VALUE,
                        1000,
                        facet.category
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = true
                )

                constraints.add(info)
            }

            return constraints
        }

        override fun getAllocationPriority(itemGroup: ItemCategory): Int {
            return -(this.itemSpecificGroupMap[itemGroup]?.maxBy { it.priority }?.priority ?: 0)
        }

        private class SpecificItemGroup(val id: Int, val desiredAmount: Int, val priority: Int)
    }
}
