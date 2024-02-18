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
package net.ccbluex.liquidbounce.features.module.modules.player.autoshop

import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.isWool
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.Item
import net.minecraft.item.Items
import net.minecraft.screen.slot.SlotActionType
import kotlin.math.ceil
import kotlin.math.min

/**
 * AutoShop module
 *
 * Automatically buys specific items in a BedWars shop.
 */

object ModuleAutoShop : Module("AutoShop", Category.PLAYER) {
    var configName by text("Config", "pikanetwork").listen {
        loadAutoShopConfig(it)
        it
    }
    val startDelay by intRange("StartDelay", 1..2, 0..10, "ticks")
    val clickDelay by intRange("ClickDelay", 2..4, 2..10, "ticks")
    val autoClose by boolean("AutoClose", true)
    val quickBuy by boolean("QuickBuy", false)
    val reload by boolean("Reload", false).listen {
        if (it == true) {
            loadAutoShopConfig(configName)
        }
        false
    }

    var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    private var waitedBeforeTheFirstClick = false
    private var itemsFromInventory = mutableMapOf<Item, Int>()
    lateinit var currentConfig: AutoShopConfig

    val onUpdate = handler<GameTickEvent> {
        // update items from the player's inventory
        if (!isShopOpened()) {
            return@handler
        }
        synchronized(itemsFromInventory) {
            itemsFromInventory.clear()
            itemsFromInventory.putAll(getItemsFromInventory())
        }
    }


    val repeatable = repeatable {
        if (!isShopOpened()) {
            reset()
            return@repeatable
        }

        for (index in currentConfig.elements.indices) {
            val element = currentConfig.elements[index]

            var needToBuy = checkElement(element) != null
            if (!needToBuy) {
                continue
            }

            // wait between the opening a shop and the fist click
            if (!waitedBeforeTheFirstClick) {
                waitConditional(startDelay.random()) { !isShopOpened() }
                waitedBeforeTheFirstClick = true
            }

            needToBuy = checkElement(element) != null

            // buy an item
            while (needToBuy) {
                // check if it's capable of clicking
                if (!isShopOpened()) {
                    reset()
                    return@repeatable
                }

                checkElement(element) ?: break

                doClicks(currentConfig.elements, index)
                needToBuy = checkElement(element) != null
            }
        }

        // close the shop after buying the items
        if (waitedBeforeTheFirstClick && autoClose) {
            player.closeHandledScreen()
        }
    }

    private suspend fun Sequence<*>.doClicks(currentElements: List<AutoShopElement>, currentIndex: Int) {
        val categorySlot = currentElements[currentIndex].categorySlot
        val itemSlot = currentElements[currentIndex].itemSlot

        // we don't need to open, for example, "Blocks" category again if it's already opened,
        if (categorySlot != prevCategorySlot) {
            //mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, categorySlot, 0, 0, mc.thePlayer)
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                categorySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
            prevCategorySlot = categorySlot
            waitConditional(clickDelay.random()) { !isShopOpened() }
        }

        // check if it's capable of clicking
        if (!isShopOpened()) {
            return
        }

        if (!quickBuy) {
            //mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, itemSlot, 0, 0, mc.thePlayer)
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                itemSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
            waitConditional(clickDelay.random()) { !isShopOpened() }
            return
        }

        val slotsToClick = simulateNextPurchases(currentElements, currentIndex)
        for(slot in slotsToClick) {
            //mc.playerController.windowClick(mc.thePlayer.openContainer.windowId, slot, 0, 0, mc.thePlayer)
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
        }
        waitConditional(clickDelay.random()) { !isShopOpened() }
    }

    /**
     * Returns a list of slots which can be clicked in the same category where the current element is.
     * Some servers allow to buy everything at once (if the items are in the same category).
     * The only limitation is that the items are in different categories,
     * so we need to wait for a server response if we need to change an item category.
     * However, we can buy every item in the current category in no time, then move to the next category and do the same.
     *
     * It's required to calculate how many items it's possible to buy immediately, according to the resources the player has
     * and the buy order in the config, simply because waiting for a response from a server isn't the case
     */
    private fun simulateNextPurchases(currentElements: List<AutoShopElement>, currentIndex: Int) : List<Int> {
        val currentCategorySlot = currentElements[currentIndex].categorySlot
        val currentItems = getItemsFromInventory().toMutableMap()
        val limitedItemsAmount = currentItems.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
        val slots = mutableListOf<Int>()
        var nextCategorySlot = -1


        for (i in currentIndex until currentElements.size) {
            val element = currentElements[i]

            val clicks = getRequiredClicks(element, currentItems)
            if (clicks < 1) {
                continue    // we can't buy an item actually
            }

            // subtract the required items from the limited items we have
            val requiredItems = checkElement(element, currentItems)?.filterKeys { key -> key in LIMITED_ITEMS } ?: continue
            for (key in limitedItemsAmount.keys) {
                limitedItemsAmount[key] = (limitedItemsAmount[key] ?: 0) - (requiredItems[key] ?: 0) * clicks
            }
            currentItems.putAll(limitedItemsAmount) // update the current items
            currentItems[element.item] = (currentItems[element.item] ?: 0) + element.amountPerClick * clicks

            if (element.categorySlot == currentCategorySlot) {
                repeat(clicks) {
                    slots.add(element.itemSlot)
                }
                continue
            }

            // update the next category slot if it's empty
            if (nextCategorySlot == -1) {
                nextCategorySlot = element.categorySlot
            }
        }

        if (nextCategorySlot != -1) {
            slots.add(nextCategorySlot)
        }
        return slots
    }


    /**
     * Returns the limited items required to buy an item
     * Returns null if an item shouldn't or can't be bought
     */
    private fun checkElement(autoShopElement: AutoShopElement, items: Map<Item, Int> = itemsFromInventory) : Map<Item, Int>? {
        if (!shouldBuy(autoShopElement, items)) {
            return null
        }

        if (findItem(autoShopElement.item, items) >= autoShopElement.minAmount) {
            return null
        }

        return checkPrice(autoShopElement.price, items)?.filterKeys { key -> key in LIMITED_ITEMS }
    }

    /**
     * Returns the amount of clicks which can be performed to buy an item
     * For example, it might need 4 clicks to buy wool blocks but there might be enough resources only for 3 clicks
     */
    private fun getRequiredClicks(autoShopElement: AutoShopElement, items: Map<Item, Int> = itemsFromInventory) : Int {
        checkElement(autoShopElement, items) ?: return 0

        val requiredLimitedItems = checkElement(autoShopElement, items)?.filterKeys { key -> key in LIMITED_ITEMS } ?: return 0
        val currentLimitedItems = items.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
        // after this requiredLimitedItems and currentLimitedItems will be something like that:
        // iron_ingots - 46
        // gold_ingots - 12
        // emeralds - 3


        val currentItemAmount = min(findItem(autoShopElement.item, items), autoShopElement.minAmount)
        val maxBuyClicks = ceil(1f * (autoShopElement.minAmount - currentItemAmount) / autoShopElement.amountPerClick).toInt()
        var minMultiplier = Int.MAX_VALUE
        for (key in requiredLimitedItems.keys) {
            val requiredItemsAmount = (requiredLimitedItems[key] ?: 0)
            val currentItemsAmount = (currentLimitedItems[key] ?: 0)
            val newMultiplier = min(maxBuyClicks, currentItemsAmount / requiredItemsAmount)
            minMultiplier = min(minMultiplier, newMultiplier)
        }
        return minMultiplier
    }

    /**
     * Checks the whole CheckItems block
     */
    private fun shouldBuy(autoShopElement: AutoShopElement, items: Map<Item, Int> = itemsFromInventory) : Boolean {
        if (autoShopElement.itemsToCheckBeforeBuying == null) {
            return true
        }

        for (currentItem in autoShopElement.itemsToCheckBeforeBuying) {
            val hasItem = findItem(item = currentItem.first, items) >= currentItem.second
            if (hasItem) {
                return false
            }
        }
        return true
    }

    /**
     * Checks the whole Price block
     */
    private fun checkPrice(price: List<Map<Item, Int>>, items: Map<Item, Int> = itemsFromInventory) : Map<Item, Int>? {
        for (currentItems in price) {
            if (findItems(currentItems, items)) {
                return currentItems
            }
        }
        return null
    }

    /**
     * Returns the amount of a specific item.
     */
    private fun findItem(item: Item, items: Map<Item, Int> = itemsFromInventory): Int {
        return items[item] ?: 0
    }

    /**
     * Checks if there are enough [requiredItems] in [items]
     */
    private fun findItems(requiredItems: Map<Item, Int>, items: Map<Item, Int>) : Boolean {
        val currentItems : MutableMap<Item, Int>
        synchronized(itemsFromInventory) {
            currentItems = items.toMutableMap()
        }

        for (item in requiredItems.keys) {
            if (!currentItems.containsKey(item)) {
                return false    // we don't have all the required items
            }
            currentItems[item] = (currentItems[item] ?: 0) - (requiredItems[item] ?: 0)
        }

        for (value in currentItems.values) {
            if (value < 0) {
                return false    // there is a lack of something which means we can't buy an item
            }
        }

        return true
    }

    /**
     * Returns the items which the player has and their amounts
     */
    private fun getItemsFromInventory() : Map<Item, Int> {
        val items = mutableMapOf<Item, Int>()

        for (slot in 0 until 36) {
            val stack = mc.player!!.inventory.getStack(slot)
            if (stack.isNothing()) {
                continue
            }
            // substitute all wool blocks with a single one (for instance, blue_wool)
            val currentItem = if (stack.item.isWool()) Items.BLUE_WOOL else stack.item
            items[currentItem] = (items[currentItem] ?: 0) + stack.count
        }

        mc.player!!.armorItems.forEach {
            armorStack -> run {
                if (!armorStack.isNothing()) {
                    items[armorStack.item] = (items[armorStack.item] ?: 0) + armorStack.count
                }
            }
        }
        return items
    }

    private fun isShopOpened(): Boolean {
        mc.player ?: return false
        val screen = mc.currentScreen as? GenericContainerScreen ?: return false

        val title = screen.title.string.stripMinecraftColorCodes()
        return title.startsWith(currentConfig.traderTitle, ignoreCase = true)
    }

    private fun reset() {
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
    }
}

// The items which are usually used to buy the other items in BedWars
// A server will take them from the player if the latter wants to buy something
private val LIMITED_ITEMS = arrayOf(
    Items.BRICK, Items.IRON_INGOT, Items.GOLD_INGOT, Items.DIAMOND, Items.EMERALD
)
