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
package net.ccbluex.liquidbounce.features.module.modules.player.chestStealer

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.chestStealer.features.FeatureChestAura
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.*
import net.ccbluex.liquidbounce.utils.inventory.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.inventory.performQuickMove
import net.ccbluex.liquidbounce.utils.inventory.performSwapToHotbar
import net.ccbluex.liquidbounce.utils.inventory.performThrow
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.text.Text
import kotlin.math.ceil

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */

object ModuleChestStealer : Module("ChestStealer", Category.PLAYER) {

    val startDelay by intRange("StartDelay", 1..2, 0..20, "ticks")
    val clickDelay by intRange("ClickDelay", 2..4, 0..20, "ticks")
    val closeDelay by intRange("CloseDelay", 1..5, 0..20, "ticks")

    val selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE)
    val itemMoveMode by enumChoice("MoveMode", ItemMoveMode.QUICK_MOVE)
    val quickSwaps by boolean("QuickSwaps", true)

    val checkTitle by boolean("CheckTitle", true)

    init {
        tree(FeatureChestAura)
    }

    private val stolenSlots = mutableListOf<ContainerItemSlot>()
    private var lastSlot = 0
    private var isFirstTime = true

    override fun disable() {
        FeatureChestAura.interactedBlocksSet.clear()
        super.disable()
    }

    val repeatable = repeatable {
        if (!screenIsChest()) {
            stolenSlots.clear()
            isFirstTime = true
            return@repeatable
        }

        val screen = mc.currentScreen as GenericContainerScreen

        val cleanupPlan = createCleanupPlan(screen)
        val itemsToCollect = cleanupPlan.usefulItems.filterIsInstance<ContainerItemSlot>()

        val startDelay = startDelay.random()

        if (isFirstTime && (cleanupPlan.swaps.isNotEmpty() || itemsToCollect.isNotEmpty())) {
            isFirstTime = false

            if (startDelay > 0) {
                waitConditional(startDelay - 1) { !screenIsChest() }
                return@repeatable
            }
        }

        // Quick swap items in hotbar (i.e. swords), some servers hate them
        if (itemMoveMode == ItemMoveMode.QUICK_MOVE && quickSwaps && performQuickSwaps(cleanupPlan, screen) != null) {
            return@repeatable
        }

        val stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size)

        val sortedItemsToCollect = selectionMode.processor(itemsToCollect)

        val delay = clickDelay.random()

        for (slot in sortedItemsToCollect) {
            if (itemMoveMode == ItemMoveMode.SWAP && stolenSlots.contains(slot)) continue

            val hasFreeSpace = (0..35).any { player.inventory.getStack(it).isNothing() }

            if (!hasFreeSpace && stillRequiredSpace > 0) {
                // we exit out of the loop if inventory is full
                val shouldPause = throwItem(cleanupPlan, screen) ?: break

                if (shouldPause && delay > 0) {
                    waitConditional(delay - 1) { !screenIsChest() }
                    return@repeatable
                }
            }

            // now we have some free space, so we perform item move
            if (itemMoveMode == ItemMoveMode.SWAP) {
                interaction.performSwapToHotbar(slot, HotbarItemSlot(0), screen)

                stolenSlots.add(slot)
            } else {
                interaction.performQuickMove(slot, screen = screen)
            }

            lastSlot = slot.slotInContainer

            if (delay > 0) {
                waitConditional(delay - 1) { !screenIsChest() }
                return@repeatable
            }
        }

        if (itemMoveMode == ItemMoveMode.SWAP && quickSwaps && performQuickSwaps(cleanupPlan, screen) != null) {
            return@repeatable
        }

        waitConditional(closeDelay.random()) { !screenIsChest() }

        val finished = when (itemMoveMode) {
            ItemMoveMode.SWAP -> !sortedItemsToCollect.none { stolenSlots.contains(it) }
            ItemMoveMode.QUICK_MOVE -> sortedItemsToCollect.isEmpty()
        }

        if (finished) {
            player.closeHandledScreen()
        }
    }

    /**
     * @return if we should wait
     */
    private fun throwItem(
        cleanupPlan: InventoryCleanupPlan, screen: GenericContainerScreen
    ): Boolean? {
        val itemsInInv = findNonEmptySlotsInInventory()
        val itemToThrowOut = ModuleInventoryCleaner.findItemsToThrowOut(cleanupPlan, itemsInInv)
            .firstOrNull { it.getIdForServer(screen) != null } ?: return null

        interaction.performThrow(itemToThrowOut, screen = screen)

        return true
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
    private suspend fun Sequence<*>.performQuickSwaps(
        cleanupPlan: InventoryCleanupPlan, screen: GenericContainerScreen
    ): Boolean? {
        for (hotbarSwap in cleanupPlan.swaps) {
            // We only care about swaps from the chest to the hotbar
            if (hotbarSwap.from.slotType != ItemSlotType.CONTAINER) {
                continue
            }

            if (hotbarSwap.to !is HotbarItemSlot) {
                continue
            }

            val success = interaction.performSwapToHotbar(hotbarSwap.from, hotbarSwap.to, screen)

            if (!success) {
               continue
            }

            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to), Pair(hotbarSwap.to, hotbarSwap.from)
                )
            )

            val delay = clickDelay.random()

            if (delay > 0) {
                return waitConditional(delay - 1) { !screenIsChest() }
            }
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

    private fun findItemsInContainer(screen: GenericContainerScreen) =
        screen.screenHandler.slots.filter { !it.stack.isNothing() && it.inventory === screen.screenHandler.inventory }
            .map { ContainerItemSlot(it.id) }

    enum class SelectionMode(
        override val choiceName: String,
        val processor: (List<ContainerItemSlot>) -> List<ContainerItemSlot>
    ) : NamedChoice {
        DISTANCE("Distance", {
            it.sortedBy { slot ->
                val slotId = slot.slotInContainer

                val rowA = slotId / 9
                val colA = slotId % 9

                val rowB = lastSlot / 9
                val colB = lastSlot % 9

                (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
            }
        }),
        INDEX("Index", { list -> list.sortedBy { it.slotInContainer } }),
        RANDOM("Random", List<ContainerItemSlot>::shuffled ),
    }

    private fun screenIsChest(): Boolean {
        val screen = mc.currentScreen

        if (screen !is GenericContainerScreen || checkTitle && !isScreenTitleChest(screen)) {
            isFirstTime = true

            return false
        }

        return true
    }

    enum class ItemMoveMode(override val choiceName: String) : NamedChoice {
        SWAP("Swap"),
        QUICK_MOVE("QuickMove")
    }

}
