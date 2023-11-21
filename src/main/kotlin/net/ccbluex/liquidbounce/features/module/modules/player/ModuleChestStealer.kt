/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.NamedChoice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.CleanupPlanGenerator
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ContainerItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.InventoryCleanupPlan
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.utils.item.findNonEmptySlotsInInventory
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.text.Text
import kotlin.math.ceil

/**
 * ChestStealer module
 *
 * Automatically steals all items from a chest.
 */

object ModuleChestStealer : Module("ChestStealer", Category.PLAYER) {

    val delay by intRange("Delay", 2..4, 0..20)
    var closeDelay by intRange("CloseDelay", 1..5, 0..20)
    var selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE, SelectionMode.values())
    val checkTitle by boolean("CheckTitle", true)

    private var lastSlot = 0

    private var shouldClose = false

    val repeatable = repeatable {
        if (shouldClose) {
            player.closeHandledScreen()
            shouldClose = false
        }

        val screen = mc.currentScreen

        if (screen !is GenericContainerScreen) {
            return@repeatable
        }
        if (checkTitle && !isScreenTitleChest(screen)) {
            return@repeatable
        }

        val cleanupPlan = createCleanupPlan(screen)

        // Quick swap items in hotbar (i.e. swords)
        if (performQuickSwaps(cleanupPlan, screen)) {
            return@repeatable
        }

        val itemsToCollect = cleanupPlan.usefulItems
            .filterIsInstance<ContainerItemSlot>()

        var stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size, screen)

        val sortedItemsToCollect = selectionMode.processor(itemsToCollect)

        for (slot in sortedItemsToCollect) {
            val hasFreeSpace = (0..35).any { player.inventory.getStack(it).isNothing() }

            if (!hasFreeSpace && stillRequiredSpace > 0) {
                val shouldReturn = makeSpace(cleanupPlan, 1, screen)

                if (shouldReturn == true)
                    return@repeatable

                if (shouldReturn != null)
                    stillRequiredSpace -= 1
            }

            mc.interactionManager!!.clickSlot(
                screen.screenHandler.syncId,
                slot.slotInContainer,
                0,
                SlotActionType.QUICK_MOVE,
                player
            )

            lastSlot = slot.slotInContainer

            val delay = delay.random()

            if (delay > 0) {
                wait(delay - 1)
                return@repeatable
            }
        }


        wait(closeDelay.random())

        if (sortedItemsToCollect.isEmpty()) {
            player.closeHandledScreen()
        }
    }

    private fun makeSpace(
        cleanupPlan: InventoryCleanupPlan,
        requiredSpace: Int,
        screen: GenericContainerScreen
    ): Boolean? {
        val itemsInInv = findNonEmptySlotsInInventory()
        var stillRequiredSpace = requiredSpace
        val itemsToThrowOut = ModuleInventoryCleaner.findItemsToThrowOut(cleanupPlan, itemsInInv)

        for (slot in itemsToThrowOut) {
            if (stillRequiredSpace <= 0)
                return false

            interaction.clickSlot(
                screen.screenHandler.syncId,
                slot.getIdForServer(screen) ?: continue,
                1,
                SlotActionType.THROW,
                player
            )

            stillRequiredSpace--

            return true
        }

        if (stillRequiredSpace > 0)
            return null

        return false
    }

    private fun getStillRequiredSpace(
        cleanupPlan: InventoryCleanupPlan,
        slotsToCollect: Int,
        screen: GenericContainerScreen
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

        return titleString == Text.translatable("container.chest").string
            || titleString == Text.translatable("container.chestDouble").string
    }


    /**
     * WARNING: Due to the remap the hotbar swaps are not valid anymore after this function.
     *
     * @return true if the chest stealer should wait for the next tick to continue.
     */
    private suspend fun Sequence<*>.performQuickSwaps(
        cleanupPlan: InventoryCleanupPlan,
        screen: GenericContainerScreen
    ): Boolean {
        for (hotbarSwap in cleanupPlan.swaps) {
            // We only care about swaps from the chest to the hotbar
            if (hotbarSwap.from.slotType != ItemSlotType.CONTAINER) {
                continue
            }
            if (hotbarSwap.to !is HotbarItemSlot) {
                continue
            }

            interaction.clickSlot(
                screen.screenHandler.syncId,
                hotbarSwap.from.getIdForServer(screen) ?: continue,
                hotbarSwap.to.hotbarSlotForServer,
                SlotActionType.SWAP,
                player
            )

            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to),
                    Pair(hotbarSwap.to, hotbarSwap.from)
                )
            )


            val delay = delay.random()

            if (delay > 0) {
                wait(delay - 1)

                return true
            }

        }

        return false
    }

    /**
     * Either asks [ModuleInventoryCleaner] what to do or just takes everything.
     */
    private fun createCleanupPlan(screen: GenericContainerScreen): InventoryCleanupPlan {
        val cleanupPlan = if (!ModuleInventoryCleaner.enabled) {
            val usefulItems = findItemsInContainer(screen)

            InventoryCleanupPlan(usefulItems.toMutableSet(), mutableListOf(), hashMapOf())
        } else {
            val availableItems = findNonEmptySlotsInInventory() + this.findItemsInContainer(screen)

            CleanupPlanGenerator(ModuleInventoryCleaner.cleanupTemplateFromSettings, availableItems).generatePlan()
        }

        return cleanupPlan
    }

    private fun findItemsInContainer(screen: GenericContainerScreen) =
        screen.screenHandler.slots
            .filter { !it.stack.isNothing() && it.inventory === screen.screenHandler.inventory }
            .map { ContainerItemSlot(it.id) }

    enum class SelectionMode(
        override val choiceName: String,
        val processor: (List<ContainerItemSlot>) -> List<ContainerItemSlot>
    ) : NamedChoice {
        DISTANCE(
            "Distance",
            {
                it.sortedBy { slot ->
                    val slotId = slot.slotInContainer

                    val rowA = slotId / 9
                    val colA = slotId % 9

                    val rowB = lastSlot / 9
                    val colB = lastSlot % 9

                    (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
                }
            }
        ),
        INDEX("Index", { list -> list.sortedBy { it.slotInContainer } }),
        RANDOM("Random", List<ContainerItemSlot>::shuffled),
    }

}
