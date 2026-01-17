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

import it.unimi.dsi.fastutil.objects.Object2IntMap
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.entity.armorItems
import net.ccbluex.liquidbounce.utils.item.getPotionEffects
import net.ccbluex.liquidbounce.utils.kotlin.sumValues
import net.minecraft.core.registries.BuiltInRegistries
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.PotionItem

object AutoShopInventoryManager : EventListener {

    private val prevInventoryItems = Object2IntOpenHashMap<String>()
    private val currentInventoryItems = Object2IntOpenHashMap<String>()
    private val pendingItems = Object2IntOpenHashMap<String>()

    private val inventoryItems = ArrayList<ItemStack>(36 + 4 + 1)

    @Suppress("unused")
    // update the items from the player's inventory every tick
    private val onTick = handler<GameTickEvent> {
        inventoryItems += player.inventory.nonEquipmentItems
        inventoryItems += player.armorItems
        inventoryItems += player.offhandItem

        val newItems = Object2IntOpenHashMap<String>()
        inventoryItems.filter { !it.isEmpty }.forEach { stack ->
            val id = BuiltInRegistries.ITEM.getKey(stack.item).path
            newItems.addTo(id, stack.count)

            // collects all kinds of colorful blocks together
            // so that there is no dependency on color
            when {
                stack.item.isWool() -> newItems.addTo(WOOL_ID, stack.count)
                stack.item.isTerracotta() -> newItems.addTo(TERRACOTTA_ID, stack.count)
                stack.item.isStainedGlass() -> newItems.addTo(STAINED_GLASS_ID, stack.count)
                stack.item.isConcrete() -> newItems.addTo(CONCRETE_ID, stack.count)
            }

            // groups potions by their effects
            if (stack.item is PotionItem) {
                stack.getPotionEffects().forEach { effect ->
                    val potionID = BuiltInRegistries.MOB_EFFECT.getKey(effect.effect.value())?.path
                    val newID = "$POTION_PREFIX$potionID"
                    if (potionID != null) {
                        newItems.addTo(newID, stack.count)
                    }
                }
            }

            // groups items by enchantments
            // example: [chainmail_chestplate:protection:2 = 1, iron_sword:sharpness:3 = 1]
            stack.enchantments.entrySet().forEach {
                val enchantmentID = it.key.registeredName.replace("minecraft:", "")
                val level = it.intValue
                val enchantedItemID = "$id:$enchantmentID:$level"
                newItems.addTo(enchantedItemID, stack.count)
            }

            // adds data about tiered items
            // example: [sword:tier:1 = 1, bow:tier:2 = 1]
            ModuleAutoShop.currentConfig.itemsWithTiers?.forEach {
                it.value.forEachIndexed { index, id ->
                    val tieredItemID = it.key + TIER_ID + (index + 1)
                    val tieredItemAmount = newItems.getOrDefault(id, 0)
                    if (tieredItemAmount > 0) {
                        newItems.addTo(tieredItemID, tieredItemAmount)
                    }
                }
            }
        }

        // experience level
        newItems.put(EXPERIENCE_ID, player.experienceLevel)
        this.update(newItems)
        inventoryItems.clear()
    }

    private fun update(newItems: Map<String, Int>) {
        prevInventoryItems.clear()
        prevInventoryItems.putAll(currentInventoryItems)

        currentInventoryItems.clear()
        currentInventoryItems.putAll(newItems)

        // updates pending items on the inventory update
        updatePendingItems()
    }

    private fun updatePendingItems() {
        val itemsToRemove = ObjectOpenHashSet<String>()
        val itemsToUpdate = Object2IntOpenHashMap<String>()

        pendingItems.forEach { (item, _) ->
            val newAmount = currentInventoryItems.getOrDefault(item, 0)
            val prevAmount = prevInventoryItems.getOrDefault(item, 0)
            val currentPendingAmount = pendingItems.getOrDefault(item, 0)

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
            } else if (lostNegativeItems) {
                val newPendingAmount = currentPendingAmount + (prevAmount - newAmount)
                when {
                    newPendingAmount >= 0 -> itemsToRemove.add(item)
                    else -> itemsToUpdate[item] = newPendingAmount
                }
            }
        }

        pendingItems.keys.removeAll(itemsToRemove)
        pendingItems.putAll(itemsToUpdate)
    }

    fun getInventoryItems(): Object2IntMap<String> {
        return Object2IntOpenHashMap(currentInventoryItems).sumValues(pendingItems)
    }

    fun addPendingItems(items: Object2IntMap<String>) {
        pendingItems.sumValues(items)
    }

    fun clearPendingItems() {
        pendingItems.clear()
    }

    override fun parent() = ModuleAutoShop

}
