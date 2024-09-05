/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.cheststealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text
import kotlin.math.ceil

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */
@IncludeModule
object ModuleChestStealer : Module("ChestStealer", Category.PLAYER) {

    val inventoryConstrains = tree(InventoryConstraints())
    val autoClose by boolean("AutoClose", true)

    val selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE)
    val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)
    val quickSwaps by boolean("QuickSwaps", true)

    val checkTitle by boolean("CheckTitle", true)

    init {
        tree(FeatureChestAura)
    }

    override fun disable() {
        FeatureChestAura.interactedBlocksSet.clear()
        super.disable()
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
            event.schedule(inventoryConstrains, when (itemMoveMode) {
                ItemMoveMode.QUICK_MOVE -> listOf(ClickInventoryAction.performQuickMove(screen, slot))
                ItemMoveMode.DRAG_AND_DROP -> listOf(
                    ClickInventoryAction.performPickup(screen, slot),
                    ClickInventoryAction.performPickup(screen, emptySlot),
                )
            })
        }

        // Check if stealing the chest was completed
        if (autoClose && sortedItemsToCollect.isEmpty()) {
            event.schedule(inventoryConstrains, CloseContainerAction(screen))
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
        val freeSlotsInInv = (0..35).count { player.inventory.getStack(it).isNothing() }

        var spaceGainedThroughMerge = 0

        for (mergeableItem in cleanupPlan.mergeableItems) {
            var slotsInChest = 0
            var totalCount = 0

            for (itemStackWithSlot in mergeableItem.value) {
                if (itemStackWithSlot.slotType == ItemSlotType.CONTAINER) {
                    slotsInChest++
                }
                totalCount += itemStackWithSlot.itemStack.count
            }

            val mergedStackCount = ceil(totalCount.toDouble() / mergeableItem.key.item.maxCount.toDouble()).toInt()

            spaceGainedThroughMerge += (mergeableItem.value.size - mergedStackCount).coerceAtMost(slotsInChest)
        }

        return (slotsToCollect - freeSlotsInInv - spaceGainedThroughMerge).coerceAtLeast(0)
    }

    private fun isScreenTitleChest(screen: GenericContainerScreen): Boolean {
        val titleString = screen.title.string

        return arrayOf("container.chest", "container.chestDouble", "container.enderchest", "container.shulkerBox",
            "container.barrel")
            .map { Text.translatable(it); }
            .any { it.string == titleString }
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

            event.schedule(inventoryConstrains,
                ClickInventoryAction.performSwap(screen, hotbarSwap.from, hotbarSwap.to))

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
        val cleanupPlan = if (!ModuleInventoryCleaner.enabled) {
            val usefulItems = findItemsInContainer(screen)

            InventoryCleanupPlan(usefulItems.toMutableSet(), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + findItemsInContainer(screen)

            CleanupPlanGenerator(ModuleInventoryCleaner.cleanupTemplateFromSettings, availableItems).generatePlan()
        }

        return cleanupPlan
    }

    enum class SelectionMode(
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
        RANDOM("Random", List<ContainerItemSlot>::shuffled ),
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

    enum class ItemMoveMode(override val choiceName: String) : NamedChoice {
        QUICK_MOVE("QuickMove"),
        DRAG_AND_DROP("DragAndDrop"),
    }

}
