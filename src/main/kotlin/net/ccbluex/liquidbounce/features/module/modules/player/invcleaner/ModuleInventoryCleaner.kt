/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Value
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.inventory.ClickInventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.minecraft.block.Blocks
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
import java.util.HashMap
import kotlin.math.min

/**
 * InventoryCleaner module
 *
 * Automatically throws away useless items and sorts them.
 */
object ModuleInventoryCleaner : Module("InventoryCleaner", Category.PLAYER) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private val maxBlocks by int("MaximumBlocks", 512, 0..2500)
    private val maxArrows by int("MaximumArrows", 256, 0..2500)
    private val maxThrowables by int("MaximumThrowables", 64, 0..600)

    private val isGreedy by boolean("Greedy", true)

    private var itemLimits: Map<Item, Int> = mapOf()
    private val presentSettings: MutableList<Pair<Value<MutableList<Item>>, Value<Int>>> = mutableListOf()

    private fun recount() {
        val limits = mutableMapOf<Item, Int>()
        presentSettings.forEach { (itemsValue, countValue) ->
            val count = countValue.get()
            itemsValue.get().forEach { item ->
                val limitState = limits[item]
                // we just follow the lowest filter
                limits[item] = if (limitState == null) {
                    count
                } else {
                    min(count, limitState)
                }
            }
        }
        itemLimits = limits
    }

    @Suppress("UnusedPrivateProperty")
    private val addNewFilter by boolean("AddNewFilter", false).onChange {
        val itemType: Value<MutableList<Item>> = items("ItemsToLimit", mutableListOf()).onChanged {
            recount()
        }
        val itemLimit: Value<Int> = int("MaxItemSlots", 0, 0..40).onChanged {
            recount()
        }
        presentSettings.add(Pair(itemType, itemLimit))
        false
    }

    @Suppress("UnusedPrivateProperty")
    private val deleteFilter by boolean("DeleteFilter", false).onChange {
        listOf("ItemsToLimit", "MaxItemSlots").forEach { name ->
            val index = inner.indexOfFirst { it.name == name }
            inner.removeAt(index)
        }
        recount()
        false
    }

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
            val slotTargets: HashMap<ItemSlot, ItemSortChoice> = hashMapOf(
                Pair(OffHandSlot, offHandItem),
                Pair(HotbarItemSlot(0), slotItem1),
                Pair(HotbarItemSlot(1), slotItem2),
                Pair(HotbarItemSlot(2), slotItem3),
                Pair(HotbarItemSlot(3), slotItem4),
                Pair(HotbarItemSlot(4), slotItem5),
                Pair(HotbarItemSlot(5), slotItem6),
                Pair(HotbarItemSlot(6), slotItem7),
                Pair(HotbarItemSlot(7), slotItem8),
                Pair(HotbarItemSlot(8), slotItem9),
            )

            val forbiddenSlots = slotTargets
                .filter { it.value == ItemSortChoice.IGNORE }
                .map { (slot, _) -> slot }
                .toHashSet()

            // Disallow tampering with armor slots since auto armor already handles them
            for (armorSlot in 0 until 4) {
                forbiddenSlots.add(ArmorItemSlot(armorSlot))
            }

            return CleanupPlanPlacementTemplate(
                slotTargets,
                itemLimitPerCategory =
                hashMapOf(
                    Pair(ItemSortChoice.BLOCK.category!!, maxBlocks),
                    Pair(ItemSortChoice.THROWABLES.category!!, maxThrowables),
                    Pair(ItemCategory(ItemType.ARROW, 0), maxArrows),
                ),
                itemLimitPerItem = itemLimits,
                forbiddenSlots = forbiddenSlots,
                isGreedy = isGreedy,
            )
        }

    @Suppress("unused")
    private val handleInventorySchedule = handler<ScheduleInventoryActionEvent> { event ->
        val cleanupPlan = CleanupPlanGenerator(cleanupTemplateFromSettings, findNonEmptySlotsInInventory())
            .generatePlan()

        // Step 1: Move items to the correct slots
        for (hotbarSwap in cleanupPlan.swaps) {
            check(hotbarSwap.to is HotbarItemSlot) { "Cannot swap to non-hotbar-slot" }

            event.schedule(
                inventoryConstraints,
                ClickInventoryAction.performSwap(null, hotbarSwap.from, hotbarSwap.to)
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
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP),
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP_ALL),
                ClickInventoryAction.click(null, slot, 0, SlotActionType.PICKUP),
            )
        }

        // It is important that we call findItemSlotsInInventory() here again, because the inventory has changed.
        val itemsToThrowOut = findItemsToThrowOut(cleanupPlan, findNonEmptySlotsInInventory())

        for (slot in itemsToThrowOut) {
            event.schedule(inventoryConstraints, ClickInventoryAction.performThrow(screen = null, slot))
        }
    }

    fun findItemsToThrowOut(
        cleanupPlan: InventoryCleanupPlan,
        itemsInInv: List<ItemSlot>,
    ) = itemsInInv.filter { it !in cleanupPlan.usefulItems }

}
