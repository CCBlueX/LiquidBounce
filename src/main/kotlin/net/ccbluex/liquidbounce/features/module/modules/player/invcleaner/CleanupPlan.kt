/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

import net.minecraft.component.ComponentMap
import net.minecraft.item.Item

data class InventorySwap(val from: ItemSlot, val to: ItemSlot)

data class ItemId(val item: Item, val nbt: ComponentMap)

class InventoryCleanupPlan(
    val usefulItems: MutableSet<ItemSlot>,
    val swaps: MutableList<InventorySwap>,
    val mergeableItems: HashMap<ItemId, MutableList<ItemSlot>>,
) {
    /**
     * Replaces the slot from key to value
     */
    fun remapSlots(slotMap: Map<ItemSlot, ItemSlot>) {
        val usefulItemsToAdd = mutableSetOf<ItemSlot>()
        val usefulItemsToRemove = mutableSetOf<ItemSlot>()

        for (entry in slotMap) {
            val from = entry.key
            val to = entry.value

            if (from in usefulItems) {
                usefulItemsToRemove.add(from)
                usefulItemsToAdd.add(to)
            }
        }

        this.usefulItems.removeAll(usefulItemsToRemove)
        this.usefulItems.addAll(usefulItemsToAdd)

        this.swaps.forEachIndexed { index, hotbarSwap ->
            val newSwap =
                InventorySwap(
                    slotMap[hotbarSwap.from] ?: hotbarSwap.from,
                    slotMap[hotbarSwap.to] ?: hotbarSwap.to,
                )

            this.swaps[index] = newSwap
        }

        mergeableItems.values.forEach { mergeableItems ->
            mergeableItems.forEachIndexed { index, itemSlot ->
                mergeableItems[index] = slotMap[itemSlot] ?: itemSlot
            }
        }
    }
}
