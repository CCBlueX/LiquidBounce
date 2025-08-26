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
import net.ccbluex.liquidbounce.config.ShopConfigPreset
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.purchasemode.NormalPurchaseMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.purchasemode.QuickPurchaseMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ItemInfo
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ShopConfig
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ShopElement
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionCalculator
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.kotlin.incrementOrSet
import net.ccbluex.liquidbounce.utils.kotlin.subList
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.screen.slot.SlotActionType
import kotlin.math.ceil
import kotlin.math.min

/**
 * AutoShop module
 *
 * Automatically buys specific items in a BedWars shop.
 */
@Suppress("TooManyFunctions")
object ModuleAutoShop : ClientModule("AutoShop", Category.PLAYER) {

    private var shopConfig by enumChoice("Config", ShopConfigPreset.PIKA_NETWORK).onChanged {
        loadAutoShopConfig(it)
    }

    private val startDelay by intRange("StartDelay", 1..2, 0..10, "ticks")
    val purchaseMode = choices(this, "PurchaseMode", NormalPurchaseMode,
        arrayOf(NormalPurchaseMode, QuickPurchaseMode)
    )

    private val extraCategorySwitchDelay by intRange("ExtraCategorySwitchDelay", 3..4,
        0..10, "ticks")
    private val autoClose by boolean("AutoClose", true)

    private val autoShopInventoryManager = AutoShopInventoryManager()
    private var waitedBeforeTheFirstClick = false
    private var canAutoClose = false    // allows closing the shop menu only after a purchase
    private var prevCategorySlot = -1
    var currentConfig = ShopConfig.emptyConfig()

    // Debug
    private val recordedClicks = mutableListOf<Int>()
    private var startMilliseconds = 0L

    init {
        // Update [currentConfig] on module initialization
        loadAutoShopConfig(shopConfig)
    }

    @Suppress("unused")
    private val repeatable = tickHandler {
        if (!isShopOpen()) {
            return@tickHandler
        }

        if (ModuleDebug.running) {
            startMilliseconds = System.currentTimeMillis()
        }

        // wait after opening a shop (before the first click)
        if (!waitedBeforeTheFirstClick) {
            waitConditional(startDelay.random()) { !isShopOpen() }
            waitedBeforeTheFirstClick = true
        }

        if (!isShopOpen()) {
            reset()
            return@tickHandler
        }

        for (index in currentConfig.elements.indices) {
            val element = currentConfig.elements[index]
            val remainingElements = currentConfig.elements.subList(index)
            var needToBuy = checkElement(element, remainingElements) != null
            // buy an item
            while (needToBuy) {
                canAutoClose = true
                doClicks(currentConfig.elements.subList(index))

                // check if it's capable of clicking
                if (!isShopOpen()) {
                    reset()
                    return@tickHandler
                }
                needToBuy = checkElement(element, remainingElements) != null
            }
        }

        // close the shop after buying items
        if (waitedBeforeTheFirstClick && autoClose && canAutoClose) {
            player.closeHandledScreen()
        }
        reset()
    }

    private suspend fun Sequence.doClicks(remainingElements: List<ShopElement>) {
        val currentElement = remainingElements.first()
        val categorySlot = currentElement.categorySlot
        val itemSlot = currentElement.itemSlot

        // switches an item category to be able to buy an item
        switchCategory(categorySlot)

        // checks if it's capable of clicking
        // as a shop might get closed during switching the category
        if (!isShopOpen()) {
            return
        }

        // buys an item (1 click only)
        if (purchaseMode.activeChoice == NormalPurchaseMode) {
            buyItem(itemSlot, currentElement)
            return
        }

        // buys all items in a category and switch to the next one
        buyAllItemsInCategory(remainingElements)
    }

    private suspend fun Sequence.switchCategory(nextCategorySlot: Int) {
        // we don't need to open, for example, "Blocks" category again if it's already open
        if (prevCategorySlot == nextCategorySlot) {
            return
        }

        val prevShopStacks = (mc.currentScreen as GenericContainerScreen).stacks()
        interaction.clickSlot(
            (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
            nextCategorySlot,
            0,
            SlotActionType.PICKUP,
            mc.player
        )

        if (ModuleDebug.running) {
            recordedClicks.add(nextCategorySlot)
        }

        prevCategorySlot = nextCategorySlot
        waitUntil { !isShopOpen() || hasItemCategoryChanged(prevShopStacks) }
        waitConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    private suspend fun Sequence.buyItem(itemSlot: Int, shopElement: ShopElement) {
        val currentInventory = autoShopInventoryManager.getInventoryItems()

        interaction.clickSlot(
            (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
            itemSlot,
            0,
            SlotActionType.PICKUP,
            mc.player
        )

        if (ModuleDebug.running) {
            recordedClicks.add(itemSlot)
        }

        // waits to receive items from a server after clicking before performing the next click
        waitUntil { !isShopOpen() || hasReceivedItems(
                prevInventory = currentInventory,
                expectedItems = mapOf(
                    shopElement.item.id to shopElement.amountPerClick,
                    shopElement.price.id to -shopElement.price.minAmount))
        }

        // expects to get an item later
        if (shopElement.item.id.isArmorItem()) {
            autoShopInventoryManager.addPendingItems(mapOf(
                shopElement.item.id to shopElement.amountPerClick
            ))
        }

        // waits extra ticks
        waitConditional(NormalPurchaseMode.extraDelay.random()) { !isShopOpen() }
    }

    private suspend fun Sequence.buyAllItemsInCategory(remainingElements: List<ShopElement>) {
        val simulationResult = simulateNextPurchases(remainingElements, onlySameCategory = true)
        val slotsToClick = simulationResult.first
        val prevInventory = autoShopInventoryManager.getInventoryItems()
        val prevShopStacks = (mc.currentScreen as GenericContainerScreen).stacks()

        for(slot in slotsToClick) {
            if (slot == -1) {
                continue    // it looks as if it doesn't require to switch an item category anymore
            }

            delay(QuickPurchaseMode.delay.random().toLong())

            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                slot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )

            if (ModuleDebug.running) {
                recordedClicks.add(slot)
            }
        }

        val nextCategorySlot = slotsToClick.last()
        if (nextCategorySlot != -1) {
            prevCategorySlot = nextCategorySlot
        }

        // expects to get items later
        val newPendingItems = if (QuickPurchaseMode.waitForItems) {
            simulationResult.second.filter { it.key.isArmorItem() }
        } else { simulationResult.second }
        autoShopInventoryManager.addPendingItems(newPendingItems)

        // waits for an inventory update and for an item category update
        waitUntil { !isShopOpen() || (hasReceivedItems(prevInventory, simulationResult.second)
            && (nextCategorySlot == -1 || hasItemCategoryChanged(prevShopStacks))) }

        // waits extra ticks
        waitConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    private fun hasItemCategoryChanged(prevShopStacks: List<String>): Boolean {
        val currentShopStacks = (mc.currentScreen as GenericContainerScreen).stacks()

        val difference = currentShopStacks
            .filter { !prevShopStacks.contains(it) }
            .union(prevShopStacks.filter { !currentShopStacks.contains(it) })

        return difference.size > 1
    }

    /**
     * Checks if the player has received [expectedItems].
     *
     * If [expectedItems] contain only armor which can be received only after the shop is closed,
     * it will check whether the items required to buy it are taken.
     **/
    private fun hasReceivedItems(prevInventory: Map<String, Int>,
                                 expectedItems: Map<String, Int>): Boolean {
        val exceptedItemsToGet = expectedItems.filter { it.value > 0 }
        val exceptedItemsToLose = expectedItems.filter { it.value < 0 }
        val isArmorOnly = exceptedItemsToGet.all { it.key.isArmorItem() }

        val currentInventory = autoShopInventoryManager.getInventoryItems()
        val receivedNewItems = exceptedItemsToGet.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevInventory[item] ?: 0
            val newItemAmount = currentInventory[item] ?: 0

            newItemAmount - prevItemAmount >= expectedNewAmount
        }

        val lostPriceItems = isArmorOnly && exceptedItemsToLose.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevInventory[item] ?: 0
            val newItemAmount = currentInventory[item] ?: 0

            newItemAmount - prevItemAmount <= expectedNewAmount
        }

        return receivedNewItems || lostPriceItems
    }

    /**
     * Returns items expected to get and a list of clickable slots
     * within the same category as the current element.
     * The last item in this list is a slot pointing to the next category
     * (if a category switch is unnecessary, it will be -1).
     *
     * The function determines what items can be bought,
     * based on the player's resources and the purchase order specified in the configuration.
     */
    private fun simulateNextPurchases(
        remainingElements: List<ShopElement>,
        onlySameCategory: Boolean) : Pair<List<Int>, Map<String, Int>> {

        if (remainingElements.isEmpty()) {
            return Pair(emptyList(), emptyMap())
        }

        val initialCategorySlot = remainingElements.first().categorySlot
        var currentCategorySlot = initialCategorySlot
        val currentItems = autoShopInventoryManager.getInventoryItems().toMutableMap()
        val slots = mutableListOf<Int>()
        val expectedItems = mutableMapOf<String, Int>()
        var nextCategorySlot = -1

        @Suppress("LoopWithTooManyJumpStatements")
        for (element in remainingElements) {
            val requiredItems = checkElement(element, items = currentItems) ?: continue
            val clicks = getRequiredClicks(element, currentItems, requiredItems)
            if (clicks < 1) {
                continue    // we can't buy an item actually
            }

            // subtract the required items from the limited items we have
            currentItems.sumValues(requiredItems.mapValues { -it.value * clicks })
            currentItems.incrementOrSet(element.item.id, element.amountPerClick * clicks)


            if (!onlySameCategory) {
                if (element.categorySlot != currentCategorySlot) {
                    slots.add(element.categorySlot)
                    currentCategorySlot = element.categorySlot
                }
                repeat(clicks) { slots.add(element.itemSlot) }
                expectedItems.incrementOrSet(element.item.id, element.amountPerClick * clicks)
                expectedItems.incrementOrSet(element.price.id, -element.price.minAmount * clicks)
                continue
            }

            if (element.categorySlot == initialCategorySlot) {
                repeat(clicks) { slots.add(element.itemSlot) }
                // for example, [wool: 64, iron_ingot: -16]
                expectedItems.incrementOrSet(element.item.id, element.amountPerClick * clicks)
                expectedItems.incrementOrSet(element.price.id, -element.price.minAmount * clicks)
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
        remainingElements: List<ShopElement>? = null,
        items: Map<String, Int> = autoShopInventoryManager.getInventoryItems()) : Map<String, Int>? {

        // checks if the player already has the required item to be bought
        if ((items[shopElement.item.id] ?: 0) >= shopElement.item.minAmount) {
            return null
        }

        // checks the item's price
        if (!checkPrice(shopElement.price, items)) {
            return null
        }

        // checks if the player is capable of buying a better item so that this item is not actually needed
        if (shopElement.item.id.isItemWithTiers() && remainingElements != null) {
            val simulationResult = simulateNextPurchases(remainingElements, onlySameCategory = false)
            val hasBetterItem = hasBetterTierItem(shopElement.item.id, simulationResult.second)
            if (hasBetterItem) {
                return null
            }
        }

        // makes sure that other conditions are met
        if (!ConditionCalculator.items(items).process(
                shopElement.item.id, shopElement.purchaseConditions)) {
            return null
        }

        return mapOf(shopElement.price.id to shopElement.price.minAmount)
    }

    /**
     * Returns the amount of clicks which can be performed to buy an item
     * For example, it might need 4 clicks to buy wool blocks
     * but there might be enough resources only for 3 clicks
     */
    private fun getRequiredClicks(
        shopElement: ShopElement,
        items: Map<String, Int>,
        requiredLimitedItems: Map<String, Int>) : Int {

        val currentLimitedItems = items.filterKeys { it in LIMITED_ITEMS }
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
        val screen = mc.currentScreen as? GenericContainerScreen ?: return false

        val title = screen.title.string.stripMinecraftColorCodes()
        val isTitleValid = currentConfig.traderTitles.any {
            title.contains(it, ignoreCase = true)
        }

        return isTitleValid
    }

    private fun reset() {
        if (ModuleDebug.running && startMilliseconds != 0L && canAutoClose) {
            chat("[AutoShop] Time elapsed: ${System.currentTimeMillis() - startMilliseconds} ms")
            chat("[AutoShop] Clicked on the following slots: $recordedClicks")
            recordedClicks.clear()
            startMilliseconds = 0L
        }

        autoShopInventoryManager.clearPendingItems()
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
        canAutoClose = false
    }
}
