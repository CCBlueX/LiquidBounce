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

import kotlinx.coroutines.delay
import net.ccbluex.liquidbounce.config.AutoShopConfig.loadAutoShopConfig
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.clickModes.NormalClickMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.clickModes.QuickClickMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.*
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.*
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.item.*
import net.ccbluex.liquidbounce.utils.kotlin.incrementOrSet
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
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
    private val startDelay by intRange("StartDelay", 1..2, 0..10, "ticks")
    val purchaseMode = choices(this, "PurchaseMode", NormalClickMode,
        arrayOf(NormalClickMode, QuickClickMode))

    private val extraCategorySwitchDelay by intRange("ExtraCategorySwitchDelay", 3..4, 0..10, "ticks")
    private val autoClose by boolean("AutoClose", true)
    val reload by boolean("Reload", false).onChange {
        if (it == true) {
            reset()
            loadAutoShopConfig(configName)
        }
        false
    }
    val debug by boolean("Debug", false)

    private var waitedBeforeTheFirstClick = false
    private var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    var currentConfig = ShopConfig.emptyConfig()

    // Debug
    private val recordedClicks = mutableListOf<Int>()
    private var startMilliseconds = 0L

    @Suppress("unused")
    val repeatable = repeatable {
        if (!isShopOpen()) {
            return@repeatable
        }

        if (debug) {
            startMilliseconds = System.currentTimeMillis()
        }

        // wait after opening a shop (before the first click)
        if (!waitedBeforeTheFirstClick) {
            waitConditional(startDelay.random()) { !isShopOpen() }
            waitedBeforeTheFirstClick = true
        }

        if (!isShopOpen()) {
            reset()
            return@repeatable
        }

        for (index in currentConfig.elements.indices) {
            val element = currentConfig.elements[index]
            var needToBuy = checkElement(element) != null

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
            reset()
            player.closeHandledScreen()
        }
    }

    private suspend fun Sequence<*>.doClicks(currentElements: List<ShopElement>, currentIndex: Int) {
        val categorySlot = currentElements[currentIndex].categorySlot
        val itemSlot = currentElements[currentIndex].itemSlot

        // switches an item category to be able to buy an item
        switchCategory(categorySlot)

        // checks if it's capable of clicking
        // as a shop might get closed during switching the category
        if (!isShopOpen()) {
            return
        }

        // buys an item (1 click only)
        if (purchaseMode.activeChoice == NormalClickMode) {
            buyItem(itemSlot, shopElement = currentElements[currentIndex])
            return
        }

        // buys all items in a category and switch to the next one
        buyAllItemsInCategory(currentElements, currentIndex)
    }

    private suspend fun Sequence<*>.switchCategory(nextCategorySlot: Int) {
        // we don't need to open, for example, "Blocks" category again if it's already open
        if (prevCategorySlot == nextCategorySlot) {
            return
        }

        interaction.clickSlot(
            (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
            nextCategorySlot,
            0,
            SlotActionType.PICKUP,
            mc.player
        )
        if (debug) {
            recordedClicks.add(nextCategorySlot)
        }
        prevCategorySlot = nextCategorySlot
        waitInventoryUpdateAndCategoryChange(mc.currentScreen as GenericContainerScreen)
        waitConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    private suspend fun Sequence<*>.buyItem(itemSlot: Int, shopElement: ShopElement) {
        // expect getting an item later
        val currentInventory = AutoShopInventoryManager.getInventoryItems()
        AutoShopInventoryManager.addPendingItems(mapOf(
            shopElement.item.id to shopElement.amountPerClick,
            shopElement.price.id to -shopElement.price.minAmount
        ))
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
            targetItem = ItemInfo(id = shopElement.item.id,
                minAmount = currentInventory[shopElement.item.id] ?: 0),
            targetItemAmountPerClick = shopElement.amountPerClick,
            priceItem = ItemInfo(id = shopElement.price.id,
                minAmount = currentInventory[shopElement.price.id] ?: 0),
            priceItemAmountPerClick = shopElement.price.minAmount)
        // waits extra ticks
        waitConditional(NormalClickMode.extraDelay.random()) { !isShopOpen() }
    }

    private suspend fun Sequence<*>.buyAllItemsInCategory(currentElements: List<ShopElement>, currentIndex: Int) {
        val simulationResult = simulateNextPurchases(currentElements, currentIndex)
        val slotsToClick = simulationResult.first

        // expect getting items later
        val prevInventory = AutoShopInventoryManager.getInventoryItems()
        val newPendingItems = simulationResult.second
        AutoShopInventoryManager.addPendingItems(newPendingItems)
        for(slot in slotsToClick) {
            if (slot == -1) {
                continue    // it looks as if it doesn't require to switch an item category anymore
            }

            delay(QuickClickMode.delay.random().toLong())

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

        // waits for an inventory update and for an item category update
        waitInventoryUpdateAndCategoryChange(
            shopContainer = if (nextCategorySlot != -1)
                {mc.currentScreen as GenericContainerScreen} else { null },
            prevInventory = prevInventory,
            expectedNewItems = simulationResult.second)
        // waits extra ticks
        waitConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    /**
     * Waits until an item category is changed and the player's inventory has been changed
     * If [prevInventory], it will wait only for an item category change
     */
    private suspend fun Sequence<*>.waitInventoryUpdateAndCategoryChange(
        shopContainer: GenericContainerScreen? = null,
        prevInventory: Map<String, Int>? = null,
        expectedNewItems: Map<String, Int>? = null) {

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

                val difference = currentShopStacks
                    .filter { !prevShopStacks!!.contains(it) }
                    .union(prevShopStacks!!.filter { !currentShopStacks.contains(it) })
                    .toSet()

                updatedItemCategory = difference.size > 1
            }
            if (!updatedInventory) {
                val currentItems = AutoShopInventoryManager.getInventoryItems()

                updatedInventory = checkItemsUpdate(
                    prevItems = prevInventory!!,
                    currentItems = currentItems,
                    expectedNewItems = expectedNewItems!!)
            }

            return@waitUntil updatedInventory && updatedItemCategory
        }
    }

    /**
     * Check whether [currentItems] has [expectedNewItems] (or more) than [prevItems]
     */
    private fun checkItemsUpdate(
        prevItems: Map<String, Int>,
        currentItems: Map<String, Int>,
        expectedNewItems: Map<String, Int>) : Boolean {

        val receivedNewItems = expectedNewItems.filter { it.value > 0 }.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevItems[item] ?: 0
            val newItemAmount = currentItems[item] ?: 0

            newItemAmount - prevItemAmount >= expectedNewAmount
        }

        val lostPriceItems = expectedNewItems.filter { it.value < 0 }.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevItems[item] ?: 0
            val newItemAmount = currentItems[item] ?: 0

            prevItemAmount - newItemAmount >= expectedNewAmount
        }

        return receivedNewItems || lostPriceItems
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

            val currentInventory = AutoShopInventoryManager.getInventoryItems()
            val targetItemAmount = currentInventory[targetItem.id] ?: 0
            val priceItemAmount = currentInventory[priceItem.id] ?: 0

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
    private fun simulateNextPurchases(
        currentElements: List<ShopElement>,
        currentIndex: Int) : Pair<List<Int>, Map<String, Int>> {

        val currentCategorySlot = currentElements[currentIndex].categorySlot
        val currentItems = AutoShopInventoryManager.getInventoryItems().toMutableMap()

        val slots = mutableListOf<Int>()
        val expectedItems = mutableMapOf<String, Int>()
        var nextCategorySlot = -1

        for (i in currentIndex until currentElements.size) {
            val element = currentElements[i]

            val clicks = getRequiredClicks(element, currentItems)
            if (clicks < 1) {
                continue    // we can't buy an item actually
            }

            // subtract the required items from the limited items we have
            val requiredItems = checkElement(element, currentItems) ?: continue
            currentItems.sumValues(requiredItems.mapValues { -it.value * clicks })
            currentItems.incrementOrSet(element.item.id, element.amountPerClick * clicks)

            if (element.categorySlot == currentCategorySlot) {
                repeat(clicks) {
                    slots.add(element.itemSlot)
                }
                // for example, [wool: 64, iron_ingot: -16]
                expectedItems.incrementOrSet(element.item.id, element.amountPerClick * clicks)
                expectedItems.incrementOrSet(element.price.id, element.price.minAmount * -clicks)
                continue
            }

            // update the next category slot if it's empty
            if (nextCategorySlot == -1) {
                nextCategorySlot = element.categorySlot
            }
        }

        slots.add(nextCategorySlot)
        return Pair(slots, expectedItems)
    }


    /**
     * Returns the limited items and their amounts required to buy an item
     * Returns null if an item can't be bought
     */
    private fun checkElement(
        shopElement: ShopElement,
        items: Map<String, Int> = AutoShopInventoryManager.getInventoryItems()) : Map<String, Int>? {

        // checks if the player already has the required item to be bought
        if ((items[shopElement.item.id] ?: 0) >= shopElement.item.minAmount) {
            return null
        }

        // checks the item's price
        if (!checkPrice(shopElement.price, items)) {
            return null
        }

        // makes sure that other conditions are met
        if (shopElement.purchaseConditions != null &&
            !ConditionCalculator.items(items).process(shopElement.purchaseConditions)) {

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

        val currentItemAmount = min(items[shopElement.item.id] ?: 0, shopElement.item.minAmount)
        val maxBuyClicks = ceil(
            1f * (shopElement.item.minAmount - currentItemAmount) / shopElement.amountPerClick).toInt()
        var minMultiplier = Int.MAX_VALUE

        for (key in requiredLimitedItems.keys) {
            val requiredItemsAmount = requiredLimitedItems[key] ?: 0
            val currentItemsAmount = currentLimitedItems[key] ?: 0
            val newMultiplier = min(maxBuyClicks, currentItemsAmount / requiredItemsAmount)
            minMultiplier = min(minMultiplier, newMultiplier)
        }
        return minMultiplier
    }

    /**
     * Checks the whole price block
     */
    private fun checkPrice(price: ItemInfo, items: Map<String, Int>) : Boolean {
        val requiredItemAmount = items[price.id] ?: 0
        return requiredItemAmount >= price.minAmount
    }


    private fun isShopOpen(): Boolean {
        mc.player ?: return false
        val screen = mc.currentScreen as? GenericContainerScreen ?: return false

        val title = screen.title.string.stripMinecraftColorCodes()
        val isTitleValid = currentConfig.traderTitles.any {
            title.contains(it, ignoreCase = true)
        }

        return isTitleValid
    }

    private fun reset() {
        if (debug && startMilliseconds != 0L) {
            chat("[AutoShop] Time elapsed: ${System.currentTimeMillis() - startMilliseconds} ms")
            chat("[AutoShop] Clicked on the following slots: $recordedClicks")
            recordedClicks.clear()
            startMilliseconds = 0L
        }

        AutoShopInventoryManager.clearPendingItems()
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
    }
}
