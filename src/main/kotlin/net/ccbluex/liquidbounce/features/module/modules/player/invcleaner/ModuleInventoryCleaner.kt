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

import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference.GroupSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference.GroupSlotPreference.ItemGroupType
import net.ccbluex.liquidbounce.features.inventoryPreset.FrontendSlotPreference.SingleSlotPreference
import net.ccbluex.liquidbounce.features.inventoryPreset.InventoryPreset
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.CleanupPlanRestrictions.RestrictionType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanTemplate.SlotContentPreference
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryManager
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.ccbluex.liquidbounce.utils.item.canMerge
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.world.item.ArrowItem
import net.minecraft.world.item.Items
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

    // The slots the separate slot settings had before
    private val inventoryPresets by inventoryPreset(
        InventoryPreset(
            arrayOf(
                listOf(GroupSlotPreference(ItemGroupType.SHIELD)),
                listOf(GroupSlotPreference(ItemGroupType.WEAPON)),
                listOf(SingleSlotPreference(Items.BOW)),
                listOf(GroupSlotPreference(ItemGroupType.PICKAXE_TOOL)),
                listOf(GroupSlotPreference(ItemGroupType.AXE_TOOL)),
                emptyList(),
                listOf(GroupSlotPreference(ItemGroupType.POTION)),
                listOf(GroupSlotPreference(ItemGroupType.FOOD)),
                listOf(GroupSlotPreference(ItemGroupType.BLOCK)),
                listOf(GroupSlotPreference(ItemGroupType.BLOCK)),
            ),
        ),
    )
    private val order by enumChoice("Order", Order.VALUE)

    private enum class Order(override val tag: String) : Tagged {
        /** What matters most first, a better sword before blocks. */
        VALUE("Value"),

        /** Whatever is closest to the cursor first. */
        DISTANCE("Distance"),
    }

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
    private val maxTotems by int("MaximumTotems", 37, 0..37)

    private val itemsBlackList by items("ItemsBlacklist", itemSortedSetOf())
    private val throwUnknownItems by boolean("ThrowUnknownItems", false)

    private fun bucketCategory(fluid: Fluid) =
        ItemCategory(GenericItemType.BUCKET, ItemSubtype.BucketFluid(fluid))

    val cleanupTemplateFromSettings: CleanupPlanTemplate
        get() {
            val currentRestrictionMap = hashMapOf<ItemSlot, RestrictionType>()

            val mapped = this.inventoryPresets.itemRules
                .mapIndexed { index, choice ->
                    val slot = HotbarItemSlot.entries[index]

                    val wishes = choice.mapNotNull {
                        val representation = it.toBackendRepresentation()

                        currentRestrictionMap.compute(slot) { _, b ->
                            maxOf(b ?: RestrictionType.NONE, representation.slotRestriction)
                        }

                        representation.contentPreference
                    }

                    // Servers up to 1.15.2 cannot swap the off-hand slot, so it is never filled in there.
                    val targetable = slot != HotbarItemSlot.OFFHAND || HotbarItemSlot.OFFHAND.canBeSwapTarget

                    slot to if (targetable) wishes else emptyList()
                }
                .toTypedArray()

            val slotTargets = linkedMapOf<ItemSlot, List<SlotContentPreference>>(*mapped)

            // Disallow tampering with armor slots since auto armor already handles them
            Slots.Armor.forEach { currentRestrictionMap[it] = RestrictionType.FORBID_TAMPERING }

            if (ModuleOffhand.isOperating()) {
                // Disallow tampering with off-hand slot when AutoTotem is active
                currentRestrictionMap[HotbarItemSlot.OFFHAND] = RestrictionType.FORBID_REPLACING
            }

            // What the server keeps putting back stays where it is
            if (InventoryManager.hasServerLockedItems) {
                findNonEmptySlotsInInventory()
                    .filter { InventoryManager.isServerLocked(it.itemStack) }
                    .forEach { currentRestrictionMap[it] = RestrictionType.FORBID_TAMPERING }
            }

            val configuredItemCounts = this.inventoryPresets.itemLimitRules.map { rule ->
                val converted = rule.items
                    .mapNotNull { item -> item.toBackendRepresentation().contentPreference }
                    .map { ItemCategory(it.itemType, it.subtype) }

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
                    listOf(ItemCategory(GenericItemType.TOTEM)) to maxTotems,
                    listOf(ItemCategory(GenericItemType.JUNK)) to 0,
                ),
            )

            return CleanupPlanTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider,
                restrictions = CleanupPlanRestrictions(currentRestrictionMap),
                itemBlacklist = itemsBlackList,
                keepsUnknownItems = !throwUnknownItems,
            )
        }

    @Suppress("unused")
    private val cleanup = inventoryPlan(inventoryConstraints) {
        val currentInventorySlots = findNonEmptySlotsInInventory()
        val template = cleanupTemplateFromSettings
        val cleanupPlan = CleanupPlanGenerator(template, currentInventorySlots).plan

        val swaps = when (order) {
            Order.VALUE -> cleanupPlan.swaps.sortedByDescending { it.priority }
            Order.DISTANCE -> InventoryManager.nearestFirst(cleanupPlan.swaps) { it.from }
        }

        var hasSwapped = false
        for (hotbarSwap in swaps) {
            val to = hotbarSwap.to
            require(to is HotbarItemSlot) {
                "Invalid swap target: $to. Only hotbar slots are supported."
            }

            // An earlier swap may have brought it there already
            val from = slotOf(hotbarSwap.from)
            if (from != to) {
                perform(InventoryAction.Click.performSwap(null, from, to))
                hasSwapped = true
            }
        }

        // The slots of the plan are the ones from before its swaps, the next plan has the new ones
        if (hasSwapped) {
            return@inventoryPlan
        }

        val slotsToMerge = ItemMerge.findStacksToMerge(cleanupPlan)
        val slotsToThrow = cleanupPlan.findItemsToThrowOut(currentInventorySlots)
            .filterNot { InventoryManager.isServerLocked(it.itemStack) }
            .let { slots -> if (order == Order.DISTANCE) InventoryManager.nearestFirst(slots) { it } else slots }

        for (slot in findHotbarArrows(template)) {
            perform(InventoryAction.Click.performQuickMove(slot = slot))
        }

        for (slot in slotsToMerge) {
            if (!slot.itemStack.isEmpty) {
                // pickup -> pickup all -> pickup to handle remaining items
                perform(InventoryAction.Click.performMergeStack(slot = slot))
            }
        }

        for (slot in slotsToThrow) {
            // Merged into another stack or moved away by now
            if (!slot.itemStack.isEmpty) {
                perform(InventoryAction.Click.performThrow(screen = null, slot), priority = Priority.NOT_IMPORTANT)
            }
        }
    }

    /**
     * Arrows are shot from any slot, so in a hotbar slot that does not ask for them they only take the slot up.
     */
    private fun findHotbarArrows(template: CleanupPlanTemplate): List<HotbarItemSlot> = Slots.Hotbar.filter { slot ->
        val stack = slot.itemStack
        stack.item is ArrowItem &&
            template.restrictions.getRestrictionFor(slot) < RestrictionType.FORBID_TAMPERING &&
            template.slotContentMap[slot].orEmpty().none {
                it.itemType == GenericItemType.ARROW || it.subtype == ItemSubtype.SpecificItem(stack.item)
            } &&
            !InventoryManager.isServerLocked(stack) &&
            // A shift-click that does not fit leaves the rest behind and would be repeated forever.
            Slots.Inventory.any { it.itemStack.isEmpty || it.itemStack.canMerge(stack) }
    }

}
