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

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Reference2IntMap
import net.ccbluex.fastutil.enumMapOf
import net.ccbluex.fastutil.objectIntArrayMapOf
import net.ccbluex.fastutil.referenceIntArrayMapOf
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet
import net.ccbluex.liquidbounce.features.module.modules.player.offhand.ModuleOffhand
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.ArmorItemSlot
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

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : ClientModule(
    "InventoryCleaner", ModuleCategories.PLAYER,
    aliases = listOf("InventoryManager")
) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())


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

    private fun buildSlotTargetMap(): Map<HotbarItemSlot, ItemSortChoice> {
        val slotTargets = enumMapOf<HotbarItemSlot, ItemSortChoice>()

        if (HotbarItemSlot.OFFHAND.canBeSwapTarget) slotTargets[HotbarItemSlot.OFFHAND] = offHandItem
        slotTargets[HotbarItemSlot.SLOT_0] = slotItem1
        slotTargets[HotbarItemSlot.SLOT_1] = slotItem2
        slotTargets[HotbarItemSlot.SLOT_2] = slotItem3
        slotTargets[HotbarItemSlot.SLOT_3] = slotItem4
        slotTargets[HotbarItemSlot.SLOT_4] = slotItem5
        slotTargets[HotbarItemSlot.SLOT_5] = slotItem6
        slotTargets[HotbarItemSlot.SLOT_6] = slotItem7
        slotTargets[HotbarItemSlot.SLOT_7] = slotItem8
        slotTargets[HotbarItemSlot.SLOT_8] = slotItem9
        return slotTargets
    }

    val cleanupTemplateFromSettings: CleanupPlanPlacementTemplate
        get() {
            val slotTargets = buildSlotTargetMap()
            val blockedSlots = if (InventoryManager.hasServerLockedItems) {
                findNonEmptySlotsInInventory().filter { InventoryManager.isServerLocked(it.itemStack) }
            } else {
                emptyList()
            }

            val forbiddenSlots = buildSet<ItemSlot> {
                for ((slot, choice) in slotTargets) {
                    if (choice == ItemSortChoice.IGNORE) this += slot
                }

                // Disallow tampering with armor slots since auto armor already handles them
                this += ArmorItemSlot.entries

                if (ModuleOffhand.isOperating()) {
                    // Disallow tampering with off-hand slot when AutoTotem is active
                    this.add(HotbarItemSlot.OFFHAND)
                }

                this += blockedSlots
            }

            val forbiddenSlotsToFill = buildSet<ItemSlot> {
                // Disallow tampering with off-hand slot when AutoTotem is active
                if (ModuleOffhand.isOperating()) this += HotbarItemSlot.OFFHAND

                this += blockedSlots
            }

            val constraintProvider = AmountConstraintProvider(
                desiredItemsPerCategory = objectIntArrayMapOf(
                    ItemType.BLOCK.defaultCategory, maxBlocks,
                    ItemType.THROWABLE.defaultCategory, maxThrowables,
                    ItemType.ARROW.defaultCategory, maxArrows,
                    ItemSortChoice.WATER.category, maxWaterBuckets,
                    ItemSortChoice.LAVA.category, maxLavaBuckets,
                    ItemSortChoice.MILK.category, maxMilkBuckets,
                    ItemType.TOTEM.defaultCategory, maxTotems,
                    ItemType.JUNK.defaultCategory, 0,
                    ItemType.NONE.defaultCategory, if (throwUnknownItems) 0 else Int.MAX_VALUE,
                ),
                desiredValuePerFunction = referenceIntArrayMapOf(
                    ItemFunction.FOOD, maxFoods,
                    ItemFunction.WEAPON_LIKE, 1,
                )
            )

            return CleanupPlanPlacementTemplate(
                slotTargets,
                itemAmountConstraintProvider = constraintProvider::getConstraints,
                itemBlacklist = itemsBlackList,
                forbiddenSlots = forbiddenSlots,
                forbiddenSlotsToFill = forbiddenSlotsToFill,
                isGreedy = isGreedy,
            )
        }

    @Suppress("unused")
    private val cleanup = inventoryPlan(inventoryConstraints) {
        val currentInventorySlots = findNonEmptySlotsInInventory()
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, currentInventorySlots)
            .generatePlan()

        var hasSwapped = false
        for (hotbarSwap in cleanupPlan.swaps) {
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

        // The plan does not say what becomes of the items its swaps push out of the hotbar, the next one does
        if (hasSwapped) {
            return@inventoryPlan
        }

        val slotsToMerge = cleanupPlan.findSlotsToMerge()
        val slotsToThrow = (currentInventorySlots.filter { it.itemStack.item in itemsBlackList } +
            cleanupPlan.findItemsToThrowOut(currentInventorySlots))
            .distinct()
            .filterNot { InventoryManager.isServerLocked(it.itemStack) }

        for (slot in findHotbarArrows()) {
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
     * Arrows are shot from any slot and no hotbar slot can be set to them, so there they only take one up.
     */
    private fun findHotbarArrows(): List<HotbarItemSlot> {
        val slotTargets = buildSlotTargetMap()
        return Slots.Hotbar.filter { slot ->
            val stack = slot.itemStack
            stack.item is ArrowItem && slotTargets[slot] != ItemSortChoice.IGNORE &&
                !InventoryManager.isServerLocked(stack) &&
                // A shift-click that does not fit leaves the rest behind and would be repeated forever.
                Slots.Inventory.any { it.itemStack.isEmpty || it.itemStack.canMerge(stack) }
        }
    }

    private class AmountConstraintProvider(
        val desiredItemsPerCategory: Object2IntMap<ItemCategory>,
        val desiredValuePerFunction: Reference2IntMap<ItemFunction>,
    ) {
        fun getConstraints(facet: ItemFacet): MutableList<ItemConstraintInfo> {
            val constraints = mutableListOf<ItemConstraintInfo>()

            if (facet.providedItemFunctions.isEmpty()) {
                val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Int.MAX_VALUE
                val desiredAmount = this.desiredItemsPerCategory.getOrDefault(facet.category, defaultDesiredAmount)

                val info = ItemConstraintInfo(
                    group = ItemCategoryConstraintGroup(
                        desiredAmount..Int.MAX_VALUE,
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
                            desiredValuePerFunction.getOrDefault(function, 1)..Int.MAX_VALUE,
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
