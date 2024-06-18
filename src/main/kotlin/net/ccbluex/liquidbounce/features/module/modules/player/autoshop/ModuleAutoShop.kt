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
    val clickDelay by intRange("ClickDelay", 2..4, 2..10, "ticks")
    val autoClose by boolean("AutoClose", true)
    val quickBuy by boolean("QuickBuy", false)
    val reload by boolean("Reload", false).onChange {
        if (it == true) {
            loadAutoShopConfig(configName)
        }
        false
    }

    var prevCategorySlot = -1   // prev category slot (used for optimizing clicks count)
    private var waitedBeforeTheFirstClick = false
    private var itemsFromInventory = mutableMapOf<String, Int>()
    var currentConfig = AutoShopConfig(
                    traderTitle = "",
                    initialCategorySlot = -1,
                    elements = emptyList())

    val onUpdate = handler<GameTickEvent> {
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

        // close the shop after buying the items
        if (waitedBeforeTheFirstClick && autoClose) {
            player.closeHandledScreen()
        }
    }

    private suspend fun Sequence<*>.doClicks(currentElements: List<ShopElement>, currentIndex: Int) {
        val categorySlot = currentElements[currentIndex].categorySlot
        val itemSlot = currentElements[currentIndex].itemSlot

        // we don't need to open, for example, "Blocks" category again if it's already opened,
        if (categorySlot != prevCategorySlot) {
            interaction.clickSlot(
                (mc.currentScreen as GenericContainerScreen).screenHandler.syncId,
                categorySlot,
                0,
                SlotActionType.PICKUP,
                mc.player
            )
            prevCategorySlot = categorySlot
            waitConditional(clickDelay.random()) { !isShopOpen() }
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
        }
        waitConditional(clickDelay.random()) { !isShopOpen() }
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
     * simply because waiting for a response from a server isn't the case
     */
    private fun simulateNextPurchases(currentElements: List<ShopElement>, currentIndex: Int) : List<Int> {
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
            val requiredItems = checkElement(element, currentItems)
                    ?.filterKeys { key -> key in LIMITED_ITEMS } ?: continue
            for (key in limitedItemsAmount.keys) {
                limitedItemsAmount[key] = (limitedItemsAmount[key] ?: 0) - (requiredItems[key] ?: 0) * clicks
            }
            currentItems.putAll(limitedItemsAmount) // update the current items
            currentItems[element.id] = (currentItems[element.id] ?: 0) + element.amountPerClick * clicks

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
     * Returns null if an item can't be bought
     */
    private fun checkElement(
        shopElement: ShopElement,
        items: Map<String, Int> = itemsFromInventory) : Map<String, Int>? {

        if (findItem(shopElement.id, items) >= shopElement.minAmount) {
            return null
        }

        // check an item's price
        if (!checkPrice(shopElement.price, items)) {
            return null
        }

        if (shopElement.purchaseConditions != null && !checkPurchaseConditions(shopElement.purchaseConditions, items)) {
            return null
        }

        return mapOf(shopElement.id to shopElement.minAmount)
    }

    /**
     * Returns the amount of clicks which can be performed to buy an item
     * For example, it might need 4 clicks to buy wool blocks
     * but there might be enough resources only for 3 clicks
     */
    private fun getRequiredClicks(shopElement: ShopElement, items: Map<String, Int>) : Int {
        checkElement(shopElement, items) ?: return 0

        val requiredLimitedItems = checkElement(shopElement, items)
                                ?.filterKeys { key -> key in LIMITED_ITEMS } ?: return 0
        val currentLimitedItems = items.filterKeys { key -> key in LIMITED_ITEMS }.toMutableMap()
        // after this requiredLimitedItems and currentLimitedItems will be something like that:
        // iron_ingots - 46
        // gold_ingots - 12
        // emeralds - 3


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
    private fun checkPrice(itemInfo: ItemInfo, items: Map<String, Int>) : Boolean {
        val requiredItemAmount = findItem(itemInfo.id, items)
        return requiredItemAmount >= itemInfo.min && requiredItemAmount <= itemInfo.max
    }

    /**
     * Checks the whole purchaseConditions block
     */
    @Suppress("CognitiveComplexMethod")
    private fun checkPurchaseConditions(root: ConditionNode, items: Map<String, Int>) : Boolean {
        if (root is ItemInfo) {
            val itemAmount = findItem(root.id, items)
            return itemAmount >= root.min && itemAmount <= root.max
        }

        if (root is AllConditionNode) {
            var result = true

            for (node in root.all) {
                result = result && checkPurchaseConditions(node, items)
                if (!result) {
                    return false
                }
            }
        } else if (root is AnyConditionNode) {
            var result = false

            for (node in root.any) {
                result = result || checkPurchaseConditions(node, items)
                if (result) {
                    return true
                }
            }
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
     * Checks if there are enough [requiredItems] in [items]
     */
    @Suppress("UnusedPrivateMember")
    private fun findItems(requiredItems: Map<String, Int>, items: Map<String, Int>) : Boolean {
        val currentItems : MutableMap<String, Int>
        synchronized(items) {
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
    private fun getItemsFromInventory() : Map<String, Int> {
        val items = mutableMapOf<String, Int>()

        for (slot in 0 until 36) {
            val stack = mc.player!!.inventory.getStack(slot)
            if (stack.isNothing()) {
                continue
            }
            // substitute all wool blocks with a single one (for instance, blue_wool)
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
    }
}

// The items which are usually used to buy the other items in BedWars
// A server will take them from the player if the latter wants to buy something
private val LIMITED_ITEMS = arrayOf(
    "brick", "iron_ingot", "gold_ingot", "diamond", "emerald"
)
