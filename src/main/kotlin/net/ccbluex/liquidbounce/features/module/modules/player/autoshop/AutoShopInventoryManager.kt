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

import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.EXPERIENCE_ID
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.POTION_PREFIX
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.TERRACOTTA_ID
import net.ccbluex.liquidbounce.features.module.modules.player.autoshop.serializable.WOOL_ID
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.isTerracotta
import net.ccbluex.liquidbounce.utils.item.isWool
import net.ccbluex.liquidbounce.utils.kotlin.incrementOrSet
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
import net.minecraft.item.PotionItem
import net.minecraft.registry.Registries

object AutoShopInventoryManager : Listenable {
    private val prevInventoryItems = mutableMapOf<String, Int>()
    private val currentInventoryItems = mutableMapOf<String, Int>()
    private val pendingItems = mutableMapOf<String, Int>()

    @Suppress("unused")
    // update the items from the player's inventory every tick
    val onTick = handler<GameTickEvent> {
        val player = mc.player ?: return@handler
        if (!ModuleAutoShop.handleEvents()) {
            return@handler
        }

        val inventoryItems = player.inventory.main.toMutableList().apply {
            addAll(player.inventory.armor)
            addAll(player.inventory.offHand)
        }

        val newItems = mutableMapOf<String, Int>()
        inventoryItems.filter { !it.isNothing() }.forEach { stack ->
            val id = Registries.ITEM.getId(stack.item).path
            newItems.incrementOrSet(id, stack.count)

            // collect all kinds of wool and terracotta blocks together
            // so that there is no dependency on color
            if (stack.item.isWool()) {
                newItems.incrementOrSet(WOOL_ID, stack.count)
            }
            else if (stack.item.isTerracotta()) {
                newItems.incrementOrSet(TERRACOTTA_ID, stack.count)
            }
            else if (stack.item is PotionItem) {
                // group potions by their effects
                stack.getPotionEffects().forEach { effect ->
                    val potionID = Registries.STATUS_EFFECT.getId(effect.effectType.value())?.path
                    val newID = POTION_PREFIX + potionID
                    if (potionID != null) {
                        newItems.incrementOrSet(newID, stack.count)
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
                newItems.incrementOrSet(enchantedItemID, stack.count)
            }
        }

        // experience level
        newItems[EXPERIENCE_ID] = player.experienceLevel
        this.update(newItems)
    }

    fun getInventoryItems() : Map<String, Int> {
        synchronized(currentInventoryItems) {
            synchronized(pendingItems) {
                return currentInventoryItems.toMutableMap().sumValues(pendingItems)
            }
        }
    }

    @Suppress("CognitiveComplexMethod")
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
                    val currentPendingAmount = pendingItems[item] ?: 0

                    if (currentPendingAmount > 0 && newAmount > prevAmount) {
                        val newPendingAmount = currentPendingAmount - (newAmount - prevAmount)
                        if (newPendingAmount <= 0) {
                            itemsToRemove.add(item)
                        } else {
                            itemsToUpdate[item] = newPendingAmount
                        }
                    }
                    else if (currentPendingAmount < 0 && newAmount < prevAmount) {
                        val newPendingAmount = currentPendingAmount + (prevAmount - newAmount)
                        if (newPendingAmount >= 0) {
                            itemsToRemove.add(item)
                        } else {
                            itemsToUpdate[item] = newPendingAmount
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
