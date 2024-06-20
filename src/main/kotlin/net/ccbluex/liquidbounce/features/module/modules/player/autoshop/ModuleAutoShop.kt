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
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.item.LIMITED_ITEMS
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.isWool
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.registry.Registries
import net.minecraft.screen.slot.Slot
import net.minecraft.screen.slot.SlotActionType
import kotlin.math.ceil
import kotlin.math.min

/**
 * AutoShop module
 *
 * Automatically buys specific items in a BedWars shop.
 */

@Suppress("TooManyFunctions")
object ModuleAutoShop : Module("AutoShop", Category.PLAYER) {
    var configName by text("Config", "pikanetwork").onChanged {
        loadAutoShopConfig(it)
    }
    val startDelay by intRange("StartDelay", 1..2, 0..10, "ticks")
    val clickDelay by intRange("ClickDelay", 2..3, 0..10, "ticks")
    val categorySwitchDelay by intRange("CategorySwitchDelay", 3..4, 0..10, "ticks")
    val autoClose by boolean("AutoClose", true)
    val quickBuy by boolean("QuickBuy", false)
    val reload by boolean("Reload", false).onChange {
        if (it == true) {
            reset()
            loadAutoShopConfig(configName)
        }
        false
    }
    val debug by boolean("Debug", false)

    var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    private var waitedBeforeTheFirstClick = false
    private var itemsFromInventory = mutableMapOf<String, Int>()
    var currentConfig = ShopConfig.emptyConfig()
    private val recordedClicks = mutableListOf<Int>()

    val onTick = handler<GameTickEvent> {
        if (!isShopOpen()) {
            return@handler
        }

        // update items from the player's inventory
        synchronized(itemsFromInventory) {
            itemsFromInventory.clear()
            itemsFromInventory.putAll(getItemsFromInventory())
        }
    }

    val repeatable = repeatable {
        if (!isShopOpen()) {
            reset()
            return@repeatable
        }

        val startMilliseconds = System.currentTimeMillis()
        for (index in currentConfig.elements.indices) {
            val element = currentConfig.elements[index]

            var needToBuy = checkElement(element) != null
            if (!needToBuy) {
                continue
            }

            // wait between the opening a shop and the first click
            if (!waitedBeforeTheFirstClick) {
                waitConditional(startDelay.random()) { !isShopOpen() }
                waitedBeforeTheFirstClick = true
            }

            // check it again because it might be changed after "startDelay.random()" ticks
            needToBuy = checkElement(element) != null && isShopOpen()

            // buy an item
            while (needToBuy) {
                doClicks(currentConfig.elements, index)

                // check if it's capable of clicking
                if (!isShopOpen()) {
                    reset()
                    return@repeatable
                }
                needToBuy = checkElement(element) != null
            }
        }

        // close the shop after buying items
        if (waitedBeforeTheFirstClick && autoClose) {
            if (debug) {
                chat("[AutoShop] Time elapsed: ${System.currentTimeMillis() - startMilliseconds} ms")
                chat("[AutoShop] Clicked on the following slots: $recordedClicks")
            }
            reset()
            player.closeHandledScreen()
        }
    }

    @Suppress("LongMethod")
    private suspend fun Sequence<*>.doClicks(currentElements: List<ShopElement>, currentIndex: Int) {
        val categorySlot = currentElements[currentIndex].categorySlot
        val itemSlot = currentElements[currentIndex].itemSlot

        // we don't need to open, for example, "Blocks" category again if it's already open
        if (categorySlot != prevCategorySlot) {
            val prevShopStacks = (mc.currentScreen as GenericContainerScreen)
                .screenHandler
                .slots
                .filter { !it.stack.isNothing() &&
                    it.inventory === (mc.currentScreen as GenericContainerScreen).screenHandler.inventory }
                .toList()

            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                categorySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
            if (debug) {
                recordedClicks.add(categorySlot)
            }
            prevCategorySlot = categorySlot
            waitUntilItemCategoryChange(prevShopStacks)
            waitConditional(categorySwitchDelay.random()) { !isShopOpen() }
        }

        // check if it's capable of clicking
        if (!isShopOpen()) {
            return
        }

        if (!quickBuy) {
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                itemSlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )

            if (debug) {
                recordedClicks.add(itemSlot)
            }

            // waits to receive items from a server after clicking before performing the next click
            waitUntilItemUpdate(
                item = currentElements[currentIndex].id,
                prevItemAmount = findItem(currentElements[currentIndex].id, itemsFromInventory),
                amountPerClick = currentElements[currentIndex].amountPerClick)
            waitConditional(clickDelay.random()) { !isShopOpen() }
            return
        }


        val slotsToClick = simulateNextPurchases(currentElements, currentIndex)
        for(slot in slotsToClick) {
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
            if (debug) {
                recordedClicks.add(slot)
            }
        }

        waitUntilInventoryUpdate(itemsFromInventory.toMap())
        waitConditional(clickDelay.random()) { !isShopOpen() }
    }

    /**
     * Waits until the player's inventory has been changed
     */
    private suspend fun Sequence<*>.waitUntilInventoryUpdate(prevInventory: Map<String, Int>) {
        waitUntil {
            if (!isShopOpen()) {
                return@waitUntil true
            }

            synchronized(itemsFromInventory) {
                val currentInventory = itemsFromInventory
                return@waitUntil prevInventory.size != currentInventory.size ||
                    !prevInventory.keys.containsAll(currentInventory.keys) ||
                    !prevInventory.values.containsAll(currentInventory.values)
            }
        }
    }

    /**
     * Waits until the amount of a specific [item] is increased by [amountPerClick]
     */
    private suspend fun Sequence<*>.waitUntilItemUpdate(item: String, prevItemAmount: Int, amountPerClick: Int) {
        waitUntil {
            if (!isShopOpen()) {
                return@waitUntil true
            }

            synchronized(itemsFromInventory) {
                val currentItemAmount = findItem(item, itemsFromInventory)
                logger.info("itemsFromInventory=$itemsFromInventory")
                logger.info("prevItemAmount=$prevItemAmount")
                logger.info("currentItemAmount=$currentItemAmount")
                //TODO: fix tracking armor purchase (pika network)
                return@waitUntil currentItemAmount - prevItemAmount >= amountPerClick
            }
        }
    }

    /**
     * Waits until an item category has been changed
     */
    private suspend fun Sequence<*>.waitUntilItemCategoryChange(prevShopStacks: List<Slot>) {
        waitUntil {
            if (!isShopOpen()) {
                return@waitUntil true
            }

            val currentShopStacks = (mc.currentScreen as GenericContainerScreen)
                .screenHandler
                .slots
                .filter { !it.stack.isNothing() &&
                    it.inventory === (mc.currentScreen as GenericContainerScreen).screenHandler.inventory }
                .toList()
            return@waitUntil currentShopStacks.size != prevShopStacks.size ||
                !currentShopStacks.containsAll(prevShopStacks)
        }
    }

    /**
     * Returns a list of slots which can be clicked in the same category where the current element is.
     * Some servers allow to buy everything at once (if the items are in the same category).
     * The only limitation is that the items are in different categories,
     * so we need to wait for a server response if we need to change an item category.
     * However, we can buy every item in the current category in no time,
     * then move to the next category and do the same.
     *
     * It's required to calculate how many items it's possible to buy immediately,
     * according to the resources the player has and the buy order in the config,
     * simply because waiting for a response from a server after every purchase isn't the case
     */
    private fun simulateNextPurchases(currentElements: List<ShopElement>, currentIndex: Int) : List<Int> {
        val currentCategorySlot = currentElements[currentIndex].categorySlot
        val currentItems = getItemsFromInventory().toMutableMap()
        val limitedItems = currentItems.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
        val slots = mutableListOf<Int>()


        for (i in currentIndex until currentElements.size) {
            val element = currentElements[i]

            val clicks = getRequiredClicks(element, currentItems)
            if (clicks < 1) {
                continue    // we can't buy an item actually
            }

            // subtract the required items from the limited items we have
            val requiredItems = checkElement(element, currentItems) ?: continue
            for (key in limitedItems.keys) {
                limitedItems[key] = (limitedItems[key] ?: 0) - (requiredItems[key] ?: 0) * clicks
            }
            currentItems.putAll(limitedItems) // update the current items
            currentItems[element.id] = (currentItems[element.id] ?: 0) + element.amountPerClick * clicks

            if (element.categorySlot == currentCategorySlot) {
                repeat(clicks) {
                    slots.add(element.itemSlot)
                }
                continue
            }
        }

        return slots
    }


    /**
     * Returns the limited items required to buy an item
     * Returns null if an item can't be bought
     */
    private fun checkElement(
        shopElement: ShopElement,
        items: Map<String, Int> = itemsFromInventory) : Map<String, Int>? {

        // checks if the player already has the required item to be bought
        if (findItem(shopElement.id, items) >= shopElement.minAmount) {
            return null
        }

        // checks the item's price
        if (!checkPrice(shopElement.price, items)) {
            return null
        }

        // makes sure that other conditions are met
        if (shopElement.purchaseConditions != null && !checkPurchaseConditions(shopElement.purchaseConditions, items)) {
            return null
        }

        return mapOf(shopElement.price.id to shopElement.price.minAmount)
    }

    /**
     * Returns the amount of clicks which can be performed to buy an item
     * For example, it might need 4 clicks to buy wool blocks
     * but there might be enough resources only for 3 clicks
     */
    private fun getRequiredClicks(shopElement: ShopElement, items: Map<String, Int>) : Int {
        val requiredLimitedItems = checkElement(shopElement, items) ?: return 0
        val currentLimitedItems = items.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()

        val currentItemAmount = min(findItem(shopElement.id, items), shopElement.minAmount)
        val maxBuyClicks = ceil(
            1f * (shopElement.minAmount - currentItemAmount) / shopElement.amountPerClick).toInt()
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
     * Checks the whole price block
     */
    private fun checkPrice(itemInfo: PriceInfo, items: Map<String, Int>) : Boolean {
        val requiredItemAmount = findItem(itemInfo.id, items)
        return requiredItemAmount >= itemInfo.minAmount
    }

    /**
     * Checks the whole purchaseConditions block
     */
    @Suppress("CognitiveComplexMethod", "ReturnCount")
    private fun checkPurchaseConditions(root: ConditionNode, items: Map<String, Int>) : Boolean {
        if (root is ItemInfo) {
            val itemAmount = findItem(root.id, items)
            val currentResult = itemAmount >= root.min.coerceAtMost(root.max) && itemAmount <= root.max
            return currentResult
        }

        if (root is AllConditionNode) {
            for (node in root.all) {
                if (!checkPurchaseConditions(node, items)) {
                    return false
                }
            }
            return true
        } else if (root is AnyConditionNode) {
            for (node in root.any) {
                if (checkPurchaseConditions(node, items)) {
                    return true
                }
            }
            return false
        }

        return false
    }

    /**
     * Returns the amount of a specific item.
     */
    private fun findItem(id: String, items: Map<String, Int>): Int {
        return items[id] ?: 0
    }

    /**
     * Returns the items which the player has and their amounts
     */
    private fun getItemsFromInventory() : Map<String, Int> {
        val items = mutableMapOf<String, Int>()

        for (slot in 0 until 36) {
            val stack = mc.player!!.inventory.getStack(slot)
            if (stack.isNothing()) {
                continue
            }

            // collect all kinds of wool blocks together
            // so that there is no dependency on color
            if (stack.item.isWool()) {
                items["wool"] = (items["wool"] ?: 0) + stack.count
            }
            val id = Registries.ITEM.getId(stack.item).path
            items[id] = (items[id] ?: 0) + stack.count
        }

        mc.player!!.armorItems.forEach {
            armorStack -> run {
                if (!armorStack.isNothing()) {
                    val id = Registries.ITEM.getId(armorStack.item).path
                    items[id] = (items[id] ?: 0) + armorStack.count
                }
            }
        }
        return items
    }

    private fun isShopOpen(): Boolean {
        mc.player ?: return false

        val screen = mc.currentScreen as? GenericContainerScreen ?: return false

        val title = screen.title.string.stripMinecraftColorCodes()
        return title.startsWith(currentConfig.traderTitle, ignoreCase = true)
    }

    private fun reset() {
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
        recordedClicks.clear()
    }
}
