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

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanSlotContent
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.level.material.Fluid
import net.minecraft.world.level.material.Fluids

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

    /**
     * Limits that are only applied for the categories the preset does not configure.
     */
    private val maxBlocks by int("MaximumBlocks", 512, 0..2500)
    private val maxArrows by int("MaximumArrows", 128, 0..2500)
    private val maxThrowables by int("MaximumThrowables", 64, 0..600)
    private val maxFoods by int("MaximumFoodPoints", 200, 0..2000)
    private val maxWaterBuckets by int("MaximumWaterBuckets", 2, 0..16)
    private val maxLavaBuckets by int("MaximumLavaBuckets", 2, 0..16)
    private val maxMilkBuckets by int("MaximumMilkBuckets", 2, 0..16)

    private val itemsBlackList by items("ItemsBlacklist", itemSortedSetOf())

    private fun bucketCategory(fluid: Fluid) =
        ItemCategory(GenericItemType.BUCKET, ItemSubtype.BucketFluid(fluid))

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

                    // Servers up to 1.15.2 cannot swap the off-hand slot, so it is never filled in there.
                    val targetable = slot != HotbarItemSlot.OFFHAND || HotbarItemSlot.OFFHAND.canBeSwapTarget

                    slot to CleanupPlanSlotContent(if (targetable) wishes else emptyList())
                }
                .toTypedArray()

            val slotTargets = linkedMapOf<ItemSlot, CleanupPlanSlotContent>(*mapped)


            // Disallow tampering with armor slots since auto armor already handles them
            Slots.Armor.forEach { currentRestrictionMap[it] = RestrictionType.FORBID_TAMPERING }

            if (ModuleOffhand.isOperating()) {
                // Disallow tampering with off-hand slot when AutoTotem is active
                currentRestrictionMap[HotbarItemSlot.OFFHAND] = RestrictionType.FORBID_REPLACING
            }

            val configuredItemCounts = this.inventoryPresets.itemLimitRules.map { rule ->
                val converted = rule.items
                    .mapNotNull { item -> item.toBackendRepresentation().contentPreference }
                    .flatMap { preference ->
                        preference.subtypes.map { ItemCategory(preference.itemType, it) }
                    }

                converted to rule.itemCount
            }

            val constraintProvider = AmountItemAmountConstraintProvider(
                fallbackAmountPerFunction = mapOf(ItemFunction.FOOD to maxFoods),
                configuredItemsInSpecificCategories = configuredItemCounts,
                fallbackItemsInSpecificCategories = listOf(
                    listOf(ItemCategory(GenericItemType.BLOCK)) to maxBlocks,
                    listOf(ItemCategory(GenericItemType.ARROW)) to maxArrows,
                    listOf(ItemCategory(GenericItemType.THROWABLE)) to maxThrowables,
                    listOf(bucketCategory(Fluids.WATER)) to maxWaterBuckets,
                    listOf(bucketCategory(Fluids.LAVA)) to maxLavaBuckets,
                    listOf(ItemCategory(GenericItemType.BUCKET, ItemSubtype.MilkBucket)) to maxMilkBuckets,
                ),
            )

            return CleanupPlanTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider,
                restrictions = CleanupPlanRestrictions(currentRestrictionMap),
                itemBlacklist = itemsBlackList,
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val currentInventorySlots = findNonEmptySlotsInInventory()
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, currentInventorySlots).plan

        // Process inventory actions in priority order
        when {
            // Step 1: Move items to the correct slots
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
        val slotToMerge = ItemMerge.findStacksToMerge(cleanupPlan).firstOrNull() ?: return false

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
        currentInventorySlots: List<ItemSlot>,
    ): Boolean {
        val itemToThrow = cleanupPlan.findItemsToThrowOut(currentInventorySlots).firstOrNull() ?: return false

        event.schedule(
            inventoryConstraints,
            InventoryAction.Click.performThrow(screen = null, itemToThrow),
            Priority.NOT_IMPORTANT
        )

        return true
    }

}
