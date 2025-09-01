/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import kotlin.collections.filter
import kotlin.math.abs

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(item: Item): T? =
    findClosestSlot { it.item === item }

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(vararg items: Item): T? =
    findClosestSlot { it.item in items }

/**
 * Distance order:
 * current hand -> offhand -> other slots
 */
inline fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(predicate: (ItemStack) -> Boolean): T? {
    return mc.player?.let { player ->
        val selected = player.inventory.selectedSlot
        this.filter { predicate(it.itemStack) }.minByOrNull {
            when {
                it is OffHandSlot -> Int.MIN_VALUE + 1
                it.hotbarSlotForServer == selected -> Int.MIN_VALUE
                else -> abs(selected - it.hotbarSlotForServer)
            }
        }
    }
}

fun SlotGroup<*>.hasItem(item: Item): Boolean = any { it.itemStack.item === item }

object Slots {

    /**
     * Hotbar 0~8
     */
    @JvmField
    val Hotbar = SlotGroup(
        List(9) { HotbarItemSlot(it) }
    )

    /**
     * Inventory 0~26
     */
    @JvmField
    val Inventory = SlotGroup(
        List(27) { InventoryItemSlot(it) }
    )

    /**
     * Offhand (singleton list)
     */
    @JvmField
    val OffHand = SlotGroup(
        listOf(OffHandSlot)
    )

    /**
     * Armor slots 0~3
     *
     * Boots/Leggings/Chestplate/Helmet
     */
    @JvmField
    val Armor = SlotGroup(
        List(4) { ArmorItemSlot(it) }
    )

    /**
     * Offhand + Hotbar
     */
    @Suppress("UNCHECKED_CAST")
    @JvmField
    val OffhandWithHotbar = (OffHand + Hotbar) as SlotGroup<HotbarItemSlot>

    /**
     * Hotbar + OffHand + Inventory + Armor
     */
    @JvmField
    val All = Hotbar + OffHand + Inventory + Armor
}

class SlotGroup<T : ItemSlot>(val slots: List<T>) : List<T> by slots {
    val items: List<Item>
        get() = slots.map { it.itemStack.item }

    fun findSlot(item: Item): T? {
        return findSlot { it.item === item }
    }

    inline fun findSlot(predicate: (ItemStack) -> Boolean): T? {
        return if (mc.player == null) null else find { predicate(it.itemStack) }
    }

    operator fun plus(other: SlotGroup<*>): SlotGroup<ItemSlot> {
        val newList = ArrayList<ItemSlot>(this.size + other.size)
        newList.addAll(this)
        newList.addAll(other)
        return SlotGroup(newList)
    }

    operator fun plus(other: ItemSlot): SlotGroup<ItemSlot> {
        val newList = ArrayList<ItemSlot>(this.size + 1)
        newList.addAll(this)
        newList.add(other)
        return SlotGroup(newList)
    }
}
