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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.incrementOrSet
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
import net.minecraft.item.PotionItem
import net.minecraft.registry.Registries

class AutoShopInventoryManager : EventListener {

    private val prevInventoryItems = mutableMapOf<String, Int>()
    private val currentInventoryItems = mutableMapOf<String, Int>()
    private val pendingItems = mutableMapOf<String, Int>()

    @Suppress("unused")
    // update the items from the player's inventory every tick
    private val onTick = handler<GameTickEvent> {
        val inventoryItems = player.inventory.main.toMutableList().apply {
            addAll(player.inventory.armor)
            addAll(player.inventory.offHand)
        }

        val newItems = mutableMapOf<String, Int>()
        inventoryItems.filter { !it.isNothing() }.forEach { stack ->
            val id = Registries.ITEM.getId(stack.item).path
            newItems.incrementOrSet(id, stack.count)

            // collects all kinds of colorful blocks together
            // so that there is no dependency on color
            when {
                stack.item.isWool() ->          newItems.incrementOrSet(WOOL_ID, stack.count)
                stack.item.isTerracotta() ->    newItems.incrementOrSet(TERRACOTTA_ID, stack.count)
                stack.item.isStainedGlass() ->  newItems.incrementOrSet(STAINED_GLASS_ID, stack.count)
                stack.item.isConcrete() ->      newItems.incrementOrSet(CONCRETE_ID, stack.count)
            }

            // groups potions by their effects
            if (stack.item is PotionItem) {
                stack.getPotionEffects().forEach { effect ->
                    val potionID = Registries.STATUS_EFFECT.getId(effect.effectType.value())?.path
                    val newID = "$POTION_PREFIX$potionID"
                    if (potionID != null) {
                        newItems.incrementOrSet(newID, stack.count)
                    }
                }
            }

            // groups items by enchantments
            // example: [chainmail_chestplate:protection:2 = 1, iron_sword:sharpness:3 = 1]
            stack.enchantments.enchantmentEntries.forEach {
                val enchantmentID = it.key.idAsString.replace("minecraft:", "")
                val level = it.intValue
                val enchantedItemID = "$id:$enchantmentID:$level"
                newItems.incrementOrSet(enchantedItemID, stack.count)
            }

            // adds data about tiered items
            // example: [sword:tier:1 = 1, bow:tier:2 = 1]
            ModuleAutoShop.currentConfig.itemsWithTiers?.forEach {
                it.value.forEachIndexed { index, id ->
                    val tieredItemID = it.key + TIER_ID + (index + 1)
                    val tieredItemAmount = newItems[id] ?: 0
                    if (tieredItemAmount > 0) {
                        newItems.incrementOrSet(tieredItemID, tieredItemAmount)
                    }
                }
            }
        }

        // experience level
        newItems[EXPERIENCE_ID] = player.experienceLevel
        this.update(newItems)
    }

    private fun update(newItems: Map<String, Int>) {
        synchronized(currentInventoryItems) {
            prevInventoryItems.clear()
            prevInventoryItems.putAll(currentInventoryItems)

            currentInventoryItems.clear()
            currentInventoryItems.putAll(newItems)

            // updates pending items on the inventory update
            updatePendingItems()
        }
    }

    private fun updatePendingItems() {
        val itemsToRemove = mutableSetOf<String>()
        val itemsToUpdate = mutableMapOf<String, Int>()

        synchronized(pendingItems) {
            pendingItems.forEach { (item, _) ->
                val newAmount = currentInventoryItems[item] ?: 0
                val prevAmount = prevInventoryItems[item] ?: 0
                val currentPendingAmount = pendingItems[item] ?: 0

                // doesn't increase the pending items amount
                // if the player loses those items somehow and vise versa
                val receivedPositiveItems = currentPendingAmount > 0 && newAmount > prevAmount
                val lostNegativeItems = currentPendingAmount < 0 && newAmount < prevAmount

                if (receivedPositiveItems) {
                    val newPendingAmount = currentPendingAmount - (newAmount - prevAmount)
                    when {
                        newPendingAmount <= 0 -> itemsToRemove.add(item)
                        else -> itemsToUpdate[item] = newPendingAmount
                    }
                }
                else if (lostNegativeItems) {
                    val newPendingAmount = currentPendingAmount + (prevAmount - newAmount)
                    when {
                        newPendingAmount >= 0 -> itemsToRemove.add(item)
                        else -> itemsToUpdate[item] = newPendingAmount
                    }
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

    fun getInventoryItems() : Map<String, Int> {
        synchronized(currentInventoryItems) {
            synchronized(pendingItems) {
                return currentInventoryItems.toMutableMap().sumValues(pendingItems)
            }
        }
    }

    fun addPendingItems(items: Map<String, Int>) {
        synchronized(pendingItems) {
            pendingItems.sumValues(items)
        }
    }

    fun clearPendingItems() {
        synchronized(pendingItems) {
            pendingItems.clear()
        }
    }

    override fun parent() = ModuleAutoShop

}
