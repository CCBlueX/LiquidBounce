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
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
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
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.item.PotionItem
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

    val purchaseMode = choices(this,
        "PurchaseMode", NormalClickMode, arrayOf(
            NormalClickMode,
            QuickClickMode
        ))

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

    private object InventoryManager {
        private val prevInventoryItems = mutableMapOf<String, Int>()
        private val currentInventoryItems = mutableMapOf<String, Int>()
        private val pendingItems = mutableMapOf<String, Int>()

        fun getInventoryItems() : Map<String, Int> {
            synchronized(currentInventoryItems) {
                return currentInventoryItems.toMap()
            }
        }

        fun update(newItems: Map<String, Int>) {
            synchronized(currentInventoryItems) {
                prevInventoryItems.clear()
                prevInventoryItems.putAll(currentInventoryItems)

                currentInventoryItems.clear()
                currentInventoryItems.putAll(newItems)

                synchronized(pendingItems) {
                    val itemsToRemove = mutableSetOf<String>()
                    val itemsToUpdate = mutableMapOf<String, Int>()


                    pendingItems.forEach { (item, _) ->
                        val newAmount = currentInventoryItems[item] ?: 0
                        val prevAmount = prevInventoryItems[item] ?: 0
                        val newPendingAmount = (pendingItems[item] ?: 0) -
                            (newAmount.coerceAtLeast(prevAmount) - prevAmount)

                        if (newPendingAmount <= 0) {
                            itemsToRemove.add(item)
                        } else {
                            itemsToUpdate[item] = newPendingAmount
                        }
                    }

                    itemsToRemove.forEach { item ->
                        pendingItems.remove(item)
                    }
                    itemsToUpdate.forEach { (item, newPendingAmount) ->
                        pendingItems[item] = newPendingAmount
                    }
                }
            }
        }

        fun addPendingItems(items: Map<String, Int>) {
            synchronized(pendingItems) {
                pendingItems.sumValues(items)
            }
        }

        fun getPendingItems(): Map<String, Int> {
            synchronized(pendingItems) {
                return pendingItems.toMap()
            }
        }

        fun clearPendingItems() {
            synchronized(pendingItems) {
                pendingItems.clear()
            }
        }
    }

    private var waitedBeforeTheFirstClick = false
    private var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    var currentConfig = ShopConfig.emptyConfig()

    // Debug
    private val recordedClicks = mutableListOf<Int>()
    private var startMilliseconds = 0L

    @Suppress("unused")
    // update the items from the player's inventory every tick
    val onTick = handler<GameTickEvent> {
        if (!isShopOpen()) {
            return@handler
        }

        val player = mc.player ?: return@handler
        val inventoryItems = player.inventory.main.toMutableList().apply {
            addAll(player.inventory.armor)
            addAll(player.inventory.offHand)
        }

        val newItems = mutableMapOf<String, Int>()
        inventoryItems.filter { !it.isNothing() }.forEach { stack ->
            val id = Registries.ITEM.getId(stack.item).path
            newItems[id] = (newItems[id] ?: 0) + stack.count

            // collect all kinds of wool and terracotta blocks together
            // so that there is no dependency on color
            if (stack.item.isWool()) {
                newItems[WOOL_ID] = (newItems[WOOL_ID] ?: 0) + stack.count
            }
            if (stack.item.isTerracotta()) {
                newItems[TERRACOTTA_ID] = (newItems[TERRACOTTA_ID] ?: 0) + stack.count
            }

            // group potions by their effects
            if (stack.item is PotionItem) {
                stack.getPotionEffects().forEach { effect ->
                    val potionID = Registries.STATUS_EFFECT.getId(effect.effectType.value())?.path
                    val newID = POTION_PREFIX + potionID
                    if (potionID != null) {
                        newItems[newID] = (newItems[newID] ?: 0) + stack.count
                    }
                }
            }

            // group items by enchantments
            stack.enchantments.enchantmentEntries.forEach {
                // it doesn't work in MC 1.21 anymore
                //val enchantmentID = Registries.ENCHANTMENT.getId(it.key.value())?.path
                val enchantmentID = it.key.idAsString.replace("minecraft:", "")
                val level = it.intValue
                // example: chainmail_chestplate:protection:2
                val enchantedItemID = "$id:$enchantmentID:$level"
                newItems[enchantedItemID] = (newItems[enchantedItemID] ?: 0) + stack.count
            }
        }

        // experience level
        newItems[EXPERIENCE_ID] = player.experienceLevel
        InventoryManager.update(newItems)
    }

    @Suppress("unused")
    val repeatable = repeatable {
        if (!isShopOpen()) {
            return@repeatable
        }

        if (debug) {
            startMilliseconds = System.currentTimeMillis()
        }

        for (index in currentConfig.elements.indices) {
            val element = currentConfig.elements[index]

            var needToBuy = checkElement(element) != null
            if (!needToBuy) {
                continue
            }

            // wait after opening a shop (before the first click)
            if (!waitedBeforeTheFirstClick) {
                waitConditional(startDelay.random()) { !isShopOpen() }
                waitedBeforeTheFirstClick = true
            }

            if (!isShopOpen()) {
                return@repeatable
            }
            // check it again because it might be changed after "startDelay.random()" ticks
            needToBuy = checkElement(element) != null

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
        InventoryManager.addPendingItems(mapOf(shopElement.item.id to shopElement.amountPerClick))
        val currentInventory = InventoryManager.getInventoryItems()
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
        InventoryManager.addPendingItems(simulationResult.second.filterValues { it > 0 })
        val prevInventory = InventoryManager.getInventoryItems()
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
                val currentItems = InventoryManager.getInventoryItems()

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

            val currentInventory = InventoryManager.getInventoryItems()
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
        val currentItems = InventoryManager.getInventoryItems().toMutableMap()
        val limitedItems = currentItems.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
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
            for (key in limitedItems.keys) {
                limitedItems[key] = (limitedItems[key] ?: 0) - (requiredItems[key] ?: 0) * clicks
            }
            currentItems.putAll(limitedItems) // update the current items
            currentItems[element.item.id] = (currentItems[element.item.id] ?: 0) + element.amountPerClick * clicks

            if (element.categorySlot == currentCategorySlot) {
                repeat(clicks) {
                    slots.add(element.itemSlot)
                }

                // for example, [wool: 64, iron_ingot: -16]
                expectedItems[element.item.id] =
                    (expectedItems[element.item.id] ?: 0) + element.amountPerClick * clicks
                expectedItems[element.price.id] =
                    (expectedItems[element.price.id] ?: 0) - element.price.minAmount * clicks
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
        items: Map<String, Int> = InventoryManager.getInventoryItems()) : Map<String, Int>? {

        val totalItems = items.toMutableMap()
        totalItems.sumValues(InventoryManager.getPendingItems())

        // checks if the player already has the required item to be bought
        if ((totalItems[shopElement.item.id] ?: 0) >= shopElement.item.minAmount) {
            return null
        }

        // checks the item's price
        if (!checkPrice(shopElement.price, totalItems)) {
            return null
        }

        // makes sure that other conditions are met
        if (shopElement.purchaseConditions != null
                && !checkPurchaseConditions(shopElement.purchaseConditions, totalItems)) {
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
        val requiredItemAmount = items[itemInfo.id] ?: 0
        return requiredItemAmount >= itemInfo.minAmount
    }

    /**
     * Checks the whole purchaseConditions block
     */
    @Suppress("CognitiveComplexMethod", "NestedBlockDepth")
    private fun checkPurchaseConditions(root: ConditionNode, items: Map<String, Int>) : Boolean {
        val stack = mutableListOf(root to false)
        val results = mutableMapOf<ConditionNode, Boolean>()

        while (stack.isNotEmpty()) {
            val (currentNode, visited) = stack.removeLast()

            if (currentNode is ItemConditionNode) {
                val itemAmount = items[currentNode.id] ?: 0
                val currentResult = itemAmount <= currentNode.max &&
                    itemAmount >= currentNode.min.coerceAtMost(currentNode.max)

                results[currentNode] = currentResult
            }
            else if (currentNode is AllConditionNode) {
                if (!visited) {
                    stack.add(currentNode to true)
                    currentNode.all.asReversed().forEach { childNode ->
                        stack.add(childNode to false)
                    }
                } else {
                    results[currentNode] = currentNode.all.all { results[it] == true }
                }
            }
            else if (currentNode is AnyConditionNode) {
                if (!visited) {
                    stack.add(currentNode to true)
                    currentNode.any.asReversed().forEach { childNode ->
                        stack.add(childNode to false)
                    }
                } else {
                    results[currentNode] = currentNode.any.any { results[it] == true }
                }
            }
        }

        return results[root] == true
    }

    private fun isShopOpen(): Boolean {
        mc.player ?: return reset()
        val screen = mc.currentScreen as? GenericContainerScreen ?: return reset()

        val title = screen.title.string.stripMinecraftColorCodes()
        val isTitleValid = currentConfig.traderTitles.any {
            title.contains(it, ignoreCase = true)
        }
        if (!isTitleValid) {
            return reset()
        }

        return true
    }

    private fun reset() : Boolean {
        if (debug && startMilliseconds != 0L) {
            chat("[AutoShop] Time elapsed: ${System.currentTimeMillis() - startMilliseconds} ms")
            chat("[AutoShop] Clicked on the following slots: $recordedClicks")
            chat("[AutoShop] Pending items: ${InventoryManager.getPendingItems()}")
            recordedClicks.clear()
            startMilliseconds = 0L
        }

        InventoryManager.clearPendingItems()
        prevCategorySlot = currentConfig.initialCategorySlot
        waitedBeforeTheFirstClick = false
        return false
    }
}
