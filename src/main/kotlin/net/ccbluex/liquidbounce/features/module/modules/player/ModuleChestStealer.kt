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
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.InventoryCleanupPlan
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.PLAYER_INVENTORY_SIZE
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
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

    var delay by intRange("Delay", 50..200, 0..2000)
    var closeDelay by intRange("CloseDelay", 1..10, 0..200)
    var selectionMode by enumChoice("SelectionMode", SelectionMode.DISTANCE, SelectionMode.values())
    val checkTitle by boolean("CheckTitle", true)

    private var lastSlot = 0
    private val timer = Chronometer()

    val repeatable = handler<WorldRenderEvent> {
        if (!timer.hasElapsed()) {
            return@handler
        }

        val screen = mc.currentScreen

        if (screen !is GenericContainerScreen) {
            return@handler
        }
        if (checkTitle && !isScreenTitleChest(screen)) {
            return@handler
        }

        val cleanupPlan = createCleanupPlan(screen)

        // Quick swap items in hotbar (i.e. swords)
        if (performQuickSwaps(cleanupPlan, screen)) {
            return@handler
        }

        val itemsToCollect = cleanupPlan.usefulItems
            .filter { it >= PLAYER_INVENTORY_SIZE }
            .map { it - PLAYER_INVENTORY_SIZE }

        var stillRequiredSpace = getStillRequiredSpace(cleanupPlan, itemsToCollect.size, screen)

        val sortedItemsToCollect = this.selectionMode.processor(itemsToCollect)

        for (slotId in sortedItemsToCollect) {
            val hasFreeSpace = (0..35).any { player.inventory.getStack(it).isNothing() }

            if (!hasFreeSpace && stillRequiredSpace > 0) {
                val shouldReturn = makeSpace(cleanupPlan, 1, screen)

                if (shouldReturn == true)
                    return@handler

                if (shouldReturn != null)
                    stillRequiredSpace -= 1
            }

            mc.interactionManager!!.clickSlot(screen.screenHandler.syncId, slotId, 0, SlotActionType.QUICK_MOVE, player)

            this.lastSlot = slotId

            if (waitForTimer())
                continue

            return@handler
        }

        if (sortedItemsToCollect.isEmpty() && !waitForCloseTimer()) {
            player.closeHandledScreen()
        }
    }

    private fun makeSpace(
        cleanupPlan: InventoryCleanupPlan,
        requiredSpace: Int,
        screen: GenericContainerScreen
    ): Boolean? {
        var stillRequiredSpace = requiredSpace
        val itemsToThrowOut = ModuleInventoryCleaner.findItemsToThrowOut(cleanupPlan)

        for (slotId in itemsToThrowOut) {
            if (stillRequiredSpace <= 0)
                return false

            interaction.clickSlot(
                screen.screenHandler.syncId,
                convertClientSlotToServerSlot(slotId, screen),
                1,
                SlotActionType.THROW,
                player
            )

            this.lastSlot = slotId

            stillRequiredSpace--

            if (!waitForTimer())
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
                if (itemStackWithSlot.slot >= PLAYER_INVENTORY_SIZE) {
                    slotsInChest++
                }
                totalCount += itemStackWithSlot.itemStack.count
            }

            val mergedStackCount = ceil(totalCount.toDouble() / mergeableItem.key.maxCount.toDouble()).toInt()

            spaceGainedThroughMerge += (mergeableItem.value.size - mergedStackCount).coerceAtMost(slotsInChest)
        }

        return (slotsToCollect - freeSlotsInInv - spaceGainedThroughMerge).coerceAtLeast(0)
    }

    private fun isScreenTitleChest(screen: GenericContainerScreen): Boolean {
        val titleString = screen.title.string

        return titleString == Text.translatable("container.chest").string || titleString == Text.translatable("container.chestDouble").string
    }

    private fun waitForTimer(): Boolean {
        val time = delay.random()

        if (time == 0) {
            return true
        }

        timer.waitFor(time.toLong())
        return false
    }

    private fun waitForCloseTimer(): Boolean {
        val time = closeDelay.random()

        if (time == 0) {
            return true
        }

        timer.waitFor(time.toLong())
        return false
    }

    /**
     * WARNING: Due to the remap the hotbar swaps are not valid anymore after this function.
     */
    private fun performQuickSwaps(cleanupPlan: InventoryCleanupPlan, screen: GenericContainerScreen): Boolean {
        for (hotbarSwap in cleanupPlan.hotbarSwaps) {
            if (hotbarSwap.from < PLAYER_INVENTORY_SIZE) {
                continue
            }

            interaction.clickSlot(
                screen.screenHandler.syncId,
                hotbarSwap.from - PLAYER_INVENTORY_SIZE,
                hotbarSwap.to,
                SlotActionType.SWAP,
                player
            )

            cleanupPlan.remapSlots(
                hashMapOf(
                    Pair(hotbarSwap.from, hotbarSwap.to),
                    Pair(hotbarSwap.to, hotbarSwap.from)
                )
            )

            if (!waitForTimer())
                return true
        }

        return false
    }

    /**
     * Either asks [ModuleInventoryCleaner] what to do or just takes everything.
     */
    private fun createCleanupPlan(screen: GenericContainerScreen): InventoryCleanupPlan {
        val cleanupPlan = if (!enabled) {
            val usefulItems = screen.screenHandler.slots
                .filter { !it.stack.isNothing() && it.inventory === screen.screenHandler.inventory }
                .map { PLAYER_INVENTORY_SIZE + it.id }

            InventoryCleanupPlan(usefulItems.toMutableSet(), mutableListOf(), hashMapOf())
        } else {
            net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.createCleanupPlan(screen)
        }
        return cleanupPlan
    }

    enum class SelectionMode(override val choiceName: String, val processor: (List<Int>) -> List<Int>) : NamedChoice {
        DISTANCE(
            "Distance",
            {
                it.sortedBy { slot ->
                    val rowA = slot / 9
                    val colA = slot % 9

                    val rowB = lastSlot / 9
                    val colB = lastSlot % 9

                    (colA - colB) * (colA - colB) + (rowA - rowB) * (rowA - rowB)
                }
            }
        ),
        INDEX("Index", List<Int>::sorted),
        RANDOM("Random", List<Int>::shuffled),
    }

}
