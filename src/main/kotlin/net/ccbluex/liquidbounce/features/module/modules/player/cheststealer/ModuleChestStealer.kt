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
 *
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.player.cheststealer

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text
import kotlin.math.ceil

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */

object ModuleChestStealer : ClientModule("ChestStealer", Category.PLAYER) {

    private val inventoryConstrains = tree(InventoryConstraints())
    private val autoClose by boolean("AutoClose", true)

    private val selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE)
    private val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)
    private val quickSwaps by boolean("QuickSwaps", true)

    private val checkTitle by boolean("CheckTitle", true)

    init {
        tree(FeatureChestAura)
    }

    override fun onDisabled() {
        FeatureChestAura.interactedBlocksSet.clear()
        super.onDisabled()
    }

    val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        // Check if we are in a chest screen
        val screen = getChestScreen() ?: return@handler

        val cleanupPlan = createCleanupPlan(screen)
        val itemsToCollect = cleanupPlan.usefulItems.filterIsInstance<ContainerItemSlot>()

        // Quick swap items in hotbar (i.e. swords), some servers hate them
        if (quickSwaps && performQuickSwaps(event, cleanupPlan, screen) != null) {
            return@handler
        }

        val stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size)
        val sortedItemsToCollect = selectionMode.processor(itemsToCollect)

        for (slot in sortedItemsToCollect) {
            if (!hasInventorySpace() && stillRequiredSpace > 0) {
                event.schedule(inventoryConstrains, throwItem(cleanupPlan, screen) ?: break)
            }

            val emptySlot = findEmptyStorageSlotsInInventory().firstOrNull() ?: break

            val actions = getActionsForMove(screen, from = slot, to = emptySlot)

            event.schedule(inventoryConstrains, actions,
                /**
                 * we prioritize item based on how important it is
                 * for example we should prioritize armor over apples
                 */
                ItemCategorization(listOf()).getItemFacets(slot).maxOf { it.category.type.allocationPriority }
            )
        }

        // Check if stealing the chest was completed
        if (autoClose && sortedItemsToCollect.isEmpty()) {
            event.schedule(inventoryConstrains, CloseContainerAction(screen))
        }
    }

    /**
     * Create a list of actions that will move the item in the slot [from] to the slot [to].
     */
    private fun getActionsForMove(
        screen: GenericContainerScreen,
        from: ContainerItemSlot,
        to: ItemSlot
    ): List<ClickInventoryAction> {
        return when (itemMoveMode) {
            ItemMoveMode.QUICK_MOVE -> listOf(ClickInventoryAction.performQuickMove(screen, from))
            ItemMoveMode.DRAG_AND_DROP -> listOf(
                ClickInventoryAction.performPickup(screen, from),
                ClickInventoryAction.performPickup(screen, to),
            )
        }
    }

    /**
     * @return if we should wait
     */
    private fun throwItem(
        cleanupPlan: InventoryCleanupPlan,
        screen: GenericContainerScreen
    ): InventoryAction? {
        val itemsInInv = findNonEmptySlotsInInventory()
        val itemToThrowOut = ModuleInventoryCleaner.findItemsToThrowOut(cleanupPlan, itemsInInv)
            .firstOrNull { it.getIdForServer(screen) != null } ?: return null

        return ClickInventoryAction.performThrow(screen, itemToThrowOut)
    }

    /**
     * @param slotsToCollect amount of items we need to take
     */
    private fun getStillRequiredSpace(
        cleanupPlan: InventoryCleanupPlan,
        slotsToCollect: Int,
    ): Int {
        val freeSlotsInInv = (Slots.Inventory + Slots.Hotbar).count { it.itemStack.isEmpty }

        val spaceGainedThroughMerge = cleanupPlan.mergeableItems.entries.sumOf { (id, slots) ->
            val slotsInChest = slots.count { it.slotType == ItemSlotType.CONTAINER }
            val totalCount = slots.sumOf { it.itemStack.count }

            val mergedStackCount = ceil(totalCount.toDouble() / id.item.maxCount.toDouble()).toInt()

            (slots.size - mergedStackCount).coerceAtMost(slotsInChest)
        }

        return (slotsToCollect - freeSlotsInInv - spaceGainedThroughMerge).coerceAtLeast(0)
    }

    private fun isScreenTitleChest(screen: GenericContainerScreen): Boolean {
        val titleString = screen.title.string

        return arrayOf(
            "container.chest", "container.chestDouble", "container.enderchest", "container.shulkerBox",
            "container.barrel"
        ).any { Text.translatable(it).string == titleString }
    }


    /**
     * WARNING: Due to the remap the hotbar swaps are not valid anymore after this function.
     *
     * @return true if the chest stealer should wait for the next tick to continue. null if we didn't do anything
     */
    private fun performQuickSwaps(
        event: ScheduleInventoryActionEvent,
        cleanupPlan: InventoryCleanupPlan,
        screen: GenericContainerScreen
    ): Boolean? {
        for (hotbarSwap in cleanupPlan.swaps) {
            // We only care about swaps from the chest to the hotbar
            if (hotbarSwap.from.slotType != ItemSlotType.CONTAINER) {
                continue
            }

            if (hotbarSwap.to !is HotbarItemSlot) {
                continue
            }

            event.schedule(
                inventoryConstrains,
                ClickInventoryAction.performSwap(screen, hotbarSwap.from, hotbarSwap.to),
                /**
                 * we prioritize item based on how important it is
                 * for example we should prioritize armor over apples
                 */
                hotbarSwap.priority
            )

            // todo: hook to schedule and check if swap was successful
            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to), Pair(hotbarSwap.to, hotbarSwap.from)
                )
            )

        }

        return null
    }

    /**
     * Either asks [ModuleInventoryCleaner] what to do or just takes everything.
     */
    private fun createCleanupPlan(screen: GenericContainerScreen): InventoryCleanupPlan {
        val cleanupPlan = if (!ModuleInventoryCleaner.running) {
            val usefulItems = findItemsInContainer(screen)

            InventoryCleanupPlan(usefulItems.toMutableSet(), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + findItemsInContainer(screen)

            CleanupPlanGenerator(ModuleInventoryCleaner.cleanupTemplateFromSettings, availableItems).generatePlan()
        }

        return cleanupPlan
    }

    @Suppress("unused")
    private enum class SelectionMode(
        override val choiceName: String,
        val processor: (List<ContainerItemSlot>) -> List<ContainerItemSlot>
    ) : NamedChoice {
        DISTANCE("Distance", {
            it.sortedBy { slot ->
                val slotId = slot.slotInContainer

                val rowA = slotId / 9
                val colA = slotId % 9

                val rowB = InventoryManager.lastClickedSlot / 9
                val colB = InventoryManager.lastClickedSlot % 9

                (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
            }
        }),
        INDEX("Index", { list -> list.sortedBy { it.slotInContainer } }),
        RANDOM("Random", List<ContainerItemSlot>::shuffled),
    }

    /**
     * @return the chest screen if it is open and the title matches the chest title
     */
    private fun getChestScreen(): GenericContainerScreen? {
        val screen = mc.currentScreen

        return if (screen is GenericContainerScreen && (!checkTitle || isScreenTitleChest(screen))) {
            screen
        } else {
            null
        }
    }

    private enum class ItemMoveMode(override val choiceName: String) : NamedChoice {
        QUICK_MOVE("QuickMove"),
        DRAG_AND_DROP("DragAndDrop"),
    }

}
