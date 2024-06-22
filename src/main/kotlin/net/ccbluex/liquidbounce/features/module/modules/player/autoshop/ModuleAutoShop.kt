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

    private var waitedBeforeTheFirstClick = false
    private var itemsFromInventory = mutableMapOf<String, Int>()
        set(value) {
            synchronized(field) {
                field.clear()
                field.putAll(value)
            }
        }
        get() {
            synchronized(field) {
                return field.toMutableMap()
            }
        }
    var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    var currentConfig = ShopConfig.emptyConfig()

    // Debug
    private val recordedClicks = mutableListOf<Int>()
    private var startMilliseconds = 0L

    val onTick = handler<GameTickEvent> {
        if (!isShopOpen()) {
            return@handler
        }

        // update items from the player's inventory
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

        mc.player!!.armorItems.filter { !it.isNothing() }.forEach { armorStack ->
            val id = Registries.ITEM.getId(armorStack.item).path
            items[id] = (items[id] ?: 0) + armorStack.count
        }
        itemsFromInventory = items
    }

    val repeatable = repeatable {
        if (!isShopOpen()) {
            return@repeatable
        }

        startMilliseconds = System.currentTimeMillis()
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
                    return@repeatable
                }
                needToBuy = checkElement(element) != null
            }
        }

        // close the shop after buying items
        if (waitedBeforeTheFirstClick && autoClose) {
            player.closeHandledScreen()
        }
    }

    @Suppress("LongMethod", "CognitiveComplexMethod")
    private suspend fun Sequence<*>.doClicks(currentElements: List<ShopElement>, currentIndex: Int) {
        val categorySlot = currentElements[currentIndex].categorySlot
        val itemSlot = currentElements[currentIndex].itemSlot

        // we don't need to open, for example, "Blocks" category again if it's already open
        if (categorySlot != prevCategorySlot) {
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
            waitInventoryUpdateAndCategoryChange(mc.currentScreen as GenericContainerScreen)
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
            val currentInventory = itemsFromInventory
            waitUntilItemUpdate(
                    targetItem = ItemInfo(id = currentElements[currentIndex].item.id,
                        minAmount = currentInventory.getOrDefault(currentElements[currentIndex].item.id, 0)),
                    targetItemAmountPerClick = currentElements[currentIndex].amountPerClick,
                    priceItem = ItemInfo(id = currentElements[currentIndex].price.id,
                        minAmount = currentInventory.getOrDefault(currentElements[currentIndex].price.id, 0)),
                    priceItemAmountPerClick = currentElements[currentIndex].price.minAmount)
            waitConditional(clickDelay.random()) { !isShopOpen() }
            return
        }


        val slotsToClick = simulateNextPurchases(currentElements, currentIndex)
        for(slot in slotsToClick) {
            if (slot == -1) {
                continue    // it looks as if it doesn't require to switch an item category anymore
            }

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

        val nextCategorySlot = slotsToClick.last()
        if (nextCategorySlot != -1) {
            prevCategorySlot = nextCategorySlot
        }
        // wait for inventory update and for item category update
        waitInventoryUpdateAndCategoryChange(
            shopContainer = if (nextCategorySlot != -1)
                {mc.currentScreen as GenericContainerScreen} else {null},
            prevInventory = itemsFromInventory)
        waitConditional(clickDelay.random()) { !isShopOpen() }
    }

    /**
     * Waits until an item category is changed and the player's inventory has been changed
     * If [prevInventory], it will wait only for an item category change
     */
    private suspend fun Sequence<*>.waitInventoryUpdateAndCategoryChange(
        shopContainer: GenericContainerScreen? = null,
        prevInventory: Map<String, Int>? = null) {

        val prevShopStacks = shopContainer?.screenHandler?.slots
            ?.filter { !it.stack.isNothing() &&
                it.inventory === (mc.currentScreen as GenericContainerScreen).screenHandler.inventory }
            ?.mapNotNull { Registries.ITEM.getId(it.stack.item).path }
            ?.toSet()
        var updatedItemCategory = shopContainer == null
        var updatedInventory = prevInventory == null

        waitUntil {
            if (!isShopOpen()) {
                return@waitUntil true
            }

            if (!updatedItemCategory) {
                val currentShopStacks = (mc.currentScreen as GenericContainerScreen)
                    .screenHandler.slots
                    .filter { !it.stack.isNothing() &&
                        it.inventory === (mc.currentScreen as GenericContainerScreen).screenHandler.inventory }
                    .mapNotNull { Registries.ITEM.getId(it.stack.item).path }
                    .toSet()
                val difference = (currentShopStacks - prevShopStacks).union((prevShopStacks!! - currentShopStacks))
                updatedItemCategory = difference.size > 1
            }
            if (!updatedInventory) {
                val currentInventory = itemsFromInventory
                updatedInventory = prevInventory?.size != currentInventory.size ||
                    !prevInventory.keys.containsAll(currentInventory.keys) ||
                    !prevInventory.values.containsAll(currentInventory.values)
            }

            return@waitUntil updatedInventory && updatedItemCategory
        }
    }

    /**
     * Waits until the amount of a specific [targetItem] is increased by [targetItemAmountPerClick]
     *
     * or the amount of the required items to buy [targetItem] is decreased according to the [priceItem]
     */
    private suspend fun Sequence<*>.waitUntilItemUpdate(
            targetItem: ItemInfo,
            targetItemAmountPerClick: Int,
            priceItem: ItemInfo,
            priceItemAmountPerClick: Int) {
        waitUntil {
            if (!isShopOpen()) {
                return@waitUntil true
            }

            val currentInventory = itemsFromInventory
            val targetItemAmount = currentInventory.getOrDefault(targetItem.id, 0)
            val priceItemAmount = currentInventory.getOrDefault(priceItem.id, 0)

            return@waitUntil targetItemAmount - targetItem.minAmount >= targetItemAmountPerClick
                || priceItem.minAmount - priceItemAmount >= priceItemAmountPerClick
        }
    }

    /**
     * Returns a list of clickable slots within the same category as the current element.
     * The last item in this list is a slot pointing to the next category
     * (if a category switch is unnecessary, it will be -1).
     *
     * Some servers allow players to purchase all the items in a category at once,
     * provided that they belong to the same category.
     * This allows avoiding waiting for a server response after each purchase.
     *
     * The primary constraint is that there may be items in different categories
     * and switching categories requires a server response.
     * However, items within the current category can be purchased instantly,
     * and then the same process can be repeated for the next categories.
     *
     * The function determines how many items can be bought immediately,
     * based on the player's resources and the purchase order specified in the configuration.
     */
    private fun simulateNextPurchases(currentElements: List<ShopElement>, currentIndex: Int) : List<Int> {
        val currentCategorySlot = currentElements[currentIndex].categorySlot
        val currentItems = itemsFromInventory
        val limitedItems = currentItems.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
        val slots = mutableListOf<Int>()
        var nextCategorySlot = -1

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
            currentItems[element.item.id] = (currentItems[element.item.id] ?: 0) + element.amountPerClick * clicks

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

        slots.add(nextCategorySlot)
        return slots
    }


    /**
     * Returns the limited items and their amounts required to buy an item
     * Returns null if an item can't be bought
     */
    private fun checkElement(
        shopElement: ShopElement,
        items: Map<String, Int> = itemsFromInventory) : Map<String, Int>? {

        // checks if the player already has the required item to be bought
        if (items.getOrDefault(shopElement.item.id, 0) >= shopElement.item.minAmount) {
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

        val currentItemAmount = min(items.getOrDefault(shopElement.item.id, 0), shopElement.item.minAmount)
        val maxBuyClicks = ceil(
            1f * (shopElement.item.minAmount - currentItemAmount) / shopElement.amountPerClick).toInt()
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
    private fun checkPrice(itemInfo: ItemInfo, items: Map<String, Int>) : Boolean {
        val requiredItemAmount = items.getOrDefault(itemInfo.id, 0)
        return requiredItemAmount >= itemInfo.minAmount
    }

    /**
     * Checks the whole purchaseConditions block
     */
    @Suppress("CognitiveComplexMethod", "ReturnCount")
    private fun checkPurchaseConditions(root: ConditionNode, items: Map<String, Int>) : Boolean {
        if (root is ItemConditionNode) {
            val itemAmount = items.getOrDefault(root.id, 0)
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

    private fun isShopOpen(): Boolean {
        mc.player ?: return reset()
        val screen = mc.currentScreen as? GenericContainerScreen ?: return reset()

        val title = screen.title.string.stripMinecraftColorCodes()
        if (!title.startsWith(currentConfig.traderTitle, ignoreCase = true)) {
            return reset()
        }

        return true
    }

    private fun reset() : Boolean {
        if (debug && startMilliseconds != 0L) {
            chat("[AutoShop] Time elapsed: ${System.currentTimeMillis() - startMilliseconds} ms")
            chat("[AutoShop] Clicked on the following slots: $recordedClicks")
            recordedClicks.clear()
            startMilliseconds = 0L
        }

        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
        return false
    }
}
