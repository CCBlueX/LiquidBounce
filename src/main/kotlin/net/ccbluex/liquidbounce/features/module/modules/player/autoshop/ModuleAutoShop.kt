/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.ints.IntList
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap
import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntMaps
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import net.ccbluex.fastutil.fastIterator
import net.ccbluex.fastutil.forEachInt
import net.ccbluex.fastutil.intListOf
import net.ccbluex.liquidbounce.event.tickConditional
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.AutoShopConfig.loadAutoShopConfig
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.purchasemode.NormalPurchaseMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.purchasemode.QuickPurchaseMode
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ItemInfo
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ShopConfig
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.ShopElement
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.conditions.ConditionCalculator
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.stripMinecraftColorCodes
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.ccbluex.liquidbounce.utils.kotlin.subList
import net.minecraft.client.gui.screens.inventory.ContainerScreen
import net.minecraft.world.inventory.ClickType
import kotlin.math.ceil
import kotlin.math.min

/**
 * AutoShop module
 *
 * Automatically buys specific items in a BedWars shop.
 */
@Suppress("TooManyFunctions")
object ModuleAutoShop : ClientModule("AutoShop", ModuleCategories.PLAYER) {

    private var shopConfig by enumChoice("Config", ShopConfigPreset.PIKA_NETWORK).onChanged {
        loadAutoShopConfig(it)
    }

    private val startDelay by intRange("StartDelay", 1..2, 0..10, "ticks")
    val purchaseMode = modes(this, "PurchaseMode", NormalPurchaseMode,
        arrayOf(NormalPurchaseMode, QuickPurchaseMode)
    )

    private val extraCategorySwitchDelay by intRange("ExtraCategorySwitchDelay", 3..4,
        0..10, "ticks")
    private val autoClose by boolean("AutoClose", true)

    private var waitedBeforeTheFirstClick = false
    private var canAutoClose = false    // allows closing the shop menu only after a purchase
    private var prevCategorySlot = -1
    var currentConfig = ShopConfig.Empty

    // Debug
    private val recordedClicks = IntArrayList()
    private var startMilliseconds = 0L

    init {
        // Update [currentConfig] on module initialization
        loadAutoShopConfig(shopConfig)
    }

    @Suppress("unused")
    private val repeatable = tickHandler(Dispatchers.Minecraft) {
        if (!isShopOpen()) {
            return@tickHandler
        }

        if (ModuleDebug.running) {
            startMilliseconds = System.currentTimeMillis()
        }

        // wait after opening a shop (before the first click)
        if (!waitedBeforeTheFirstClick) {
            tickConditional(startDelay.random()) { !isShopOpen() }
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
            player.closeContainer()
        }
        reset()
    }

    private suspend fun doClicks(remainingElements: List<ShopElement>) {
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
        if (purchaseMode.activeMode == NormalPurchaseMode) {
            buyItem(itemSlot, currentElement)
            return
        }

        // buys all items in a category and switch to the next one
        buyAllItemsInCategory(remainingElements)
    }

    private suspend fun switchCategory(nextCategorySlot: Int) {
        // we don't need to open, for example, "Blocks" category again if it's already open
        if (prevCategorySlot == nextCategorySlot) {
            return
        }

        val prevShopStacks = (mc.screen as ContainerScreen).stacks()
        interaction.handleInventoryMouseClick(
            (mc.screen as ContainerScreen).menu.containerId,
            nextCategorySlot,
            0,
            ClickType.PICKUP,
            mc.player!!
        )

        if (ModuleDebug.running) {
            recordedClicks.add(nextCategorySlot)
        }

        prevCategorySlot = nextCategorySlot
        tickUntil { !isShopOpen() || hasItemCategoryChanged(prevShopStacks) }
        tickConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    private suspend fun buyItem(itemSlot: Int, shopElement: ShopElement) {
        val currentInventory = AutoShopInventoryManager.getInventoryItems()

        interaction.handleInventoryMouseClick(
            (mc.screen as ContainerScreen).menu.containerId,
            itemSlot,
            0,
            ClickType.PICKUP,
            mc.player!!
        )

        if (ModuleDebug.running) {
            recordedClicks.add(itemSlot)
        }

        // waits to receive items from a server after clicking before performing the next click
        tickUntil {
            !isShopOpen() || hasReceivedItems(
                prevInventory = currentInventory,
                expectedItems = Object2IntArrayMap(
                    arrayOf(shopElement.item.id, shopElement.price.id),
                    intArrayOf(shopElement.amountPerClick, -shopElement.price.minAmount),
                )
            )
        }

        // expects to get an item later
        if (shopElement.item.id.isArmorItem()) {
            AutoShopInventoryManager.addPendingItems(
                Object2IntMaps.singleton(shopElement.item.id, shopElement.amountPerClick)
            )
        }

        // waits extra ticks
        tickConditional(NormalPurchaseMode.extraDelay.random()) { !isShopOpen() }
    }

    private suspend fun buyAllItemsInCategory(remainingElements: List<ShopElement>) {
        val simulationResult = simulateNextPurchases(remainingElements, onlySameCategory = true)
        val slotsToClick = simulationResult.first
        val prevInventory = AutoShopInventoryManager.getInventoryItems()
        val prevShopStacks = (mc.screen as ContainerScreen).stacks()

        slotsToClick.forEachInt { slot ->
            if (slot == -1) {
                return@forEachInt // it looks as if it doesn't require to switch an item category anymore
            }

            delay(QuickPurchaseMode.delay.random().toLong())

            interaction.handleInventoryMouseClick(
                (mc.screen as ContainerScreen).menu.containerId,
                slot,
                0,
                ClickType.PICKUP,
                mc.player!!
            )

            if (ModuleDebug.running) {
                recordedClicks.add(slot)
            }
        }

        val nextCategorySlot = slotsToClick.getInt(slotsToClick.lastIndex)
        if (nextCategorySlot != -1) {
            prevCategorySlot = nextCategorySlot
        }

        // expects to get items later
        val newPendingItems = if (QuickPurchaseMode.waitForItems) {
            val map = Object2IntOpenHashMap<String>()
            simulationResult.second.fastIterator().forEach {
                if (it.key.isArmorItem()) map.put(it.key, it.intValue)
            }
            map
        } else {
            simulationResult.second
        }
        AutoShopInventoryManager.addPendingItems(newPendingItems)

        // waits for an inventory update and for an item category update
        tickUntil {
            !isShopOpen() || (hasReceivedItems(prevInventory, simulationResult.second)
                && (nextCategorySlot == -1 || hasItemCategoryChanged(prevShopStacks)))
        }

        // waits extra ticks
        tickConditional(extraCategorySwitchDelay.random()) { !isShopOpen() }
    }

    private fun hasItemCategoryChanged(prevShopStacks: List<String>): Boolean {
        val currentShopStacks = (mc.screen as ContainerScreen).stacks()

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
    private fun hasReceivedItems(
        prevInventory: Object2IntMap<String>,
        expectedItems: Object2IntMap<String>,
    ): Boolean {
        val exceptedItemsToGet = expectedItems.filter { it.value > 0 }
        val exceptedItemsToLose = expectedItems.filter { it.value < 0 }
        val isArmorOnly = exceptedItemsToGet.all { it.key.isArmorItem() }

        val currentInventory = AutoShopInventoryManager.getInventoryItems()
        val receivedNewItems = exceptedItemsToGet.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevInventory.getOrDefault(item, 0)
            val newItemAmount = currentInventory.getOrDefault(item, 0)

            newItemAmount - prevItemAmount >= expectedNewAmount
        }

        val lostPriceItems = isArmorOnly && exceptedItemsToLose.all { (item, expectedNewAmount) ->
            val prevItemAmount = prevInventory.getOrDefault(item, 0)
            val newItemAmount = currentInventory.getOrDefault(item, 0)

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
        onlySameCategory: Boolean,
    ): Pair<IntList, Object2IntMap<String>> {

        if (remainingElements.isEmpty()) {
            return Pair(intListOf(), Object2IntMaps.emptyMap())
        }

        val initialCategorySlot = remainingElements.first().categorySlot
        var currentCategorySlot = initialCategorySlot
        val currentItems = Object2IntOpenHashMap(AutoShopInventoryManager.getInventoryItems())
        val slots = IntArrayList()
        val expectedItems = Object2IntOpenHashMap<String>()
        var nextCategorySlot = -1

        @Suppress("LoopWithTooManyJumpStatements")
        for (element in remainingElements) {
            val requiredItems = checkElement(element, items = currentItems) ?: continue
            val clicks = getRequiredClicks(element, currentItems, requiredItems)
            if (clicks < 1) {
                continue    // we can't buy an item actually
            }

            // subtract the required items from the limited items we have
            requiredItems.fastIterator().forEach {
                currentItems.addTo(key, -it.intValue * clicks)
            }
            currentItems.addTo(element.item.id, element.amountPerClick * clicks)

            if (!onlySameCategory) {
                if (element.categorySlot != currentCategorySlot) {
                    slots.add(element.categorySlot)
                    currentCategorySlot = element.categorySlot
                }
                repeat(clicks) { slots.add(element.itemSlot) }
                expectedItems.addTo(element.item.id, element.amountPerClick * clicks)
                expectedItems.addTo(element.price.id, -element.price.minAmount * clicks)
                continue
            }

            if (element.categorySlot == initialCategorySlot) {
                repeat(clicks) { slots.add(element.itemSlot) }
                // for example, [wool: 64, iron_ingot: -16]
                expectedItems.addTo(element.item.id, element.amountPerClick * clicks)
                expectedItems.addTo(element.price.id, -element.price.minAmount * clicks)
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
        items: Object2IntMap<String> = AutoShopInventoryManager.getInventoryItems(),
    ): Object2IntMap<String>? {

        // checks if the player already has the required item to be bought
        if (items.getOrDefault(shopElement.item.id, 0) >= shopElement.item.minAmount) {
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
        if (!ConditionCalculator(items).process(
                shopElement.item.id, shopElement.purchaseConditions)) {
            return null
        }

        return Object2IntMaps.singleton(shopElement.price.id, shopElement.price.minAmount)
    }

    /**
     * Returns the amount of clicks which can be performed to buy an item
     * For example, it might need 4 clicks to buy wool blocks
     * but there might be enough resources only for 3 clicks
     */
    private fun getRequiredClicks(
        shopElement: ShopElement,
        items: Object2IntMap<String>,
        requiredLimitedItems: Object2IntMap<String>,
    ): Int {
        val currentLimitedItems = Object2IntOpenHashMap<String>()
        items.fastIterator().forEach {
            if (it.key in LIMITED_ITEMS) currentLimitedItems.put(it.key, it.intValue)
        }
        val currentItemAmount = min(items.getOrDefault(shopElement.item.id, 0), shopElement.item.minAmount)
        val maxBuyClicks = ceil(
            1f * (shopElement.item.minAmount - currentItemAmount) / shopElement.amountPerClick).toInt()

        return requiredLimitedItems.keys.minOf { key ->
            val requiredItemsAmount = requiredLimitedItems.getOrDefault(key, 0)
            val currentItemsAmount = currentLimitedItems.getOrDefault(key, 0)
            min(maxBuyClicks, currentItemsAmount / requiredItemsAmount)
        }
    }

    /**
     * Checks the whole price block
     */
    private fun checkPrice(price: ItemInfo, items: Object2IntMap<String>): Boolean {
        val requiredItemAmount = items.getOrDefault(price.id, 0)
        return requiredItemAmount >= price.minAmount
    }

    private fun isShopOpen(): Boolean {
        val screen = mc.screen as? ContainerScreen ?: return false

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

        AutoShopInventoryManager.clearPendingItems()
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
        canAutoClose = false
    }
}
