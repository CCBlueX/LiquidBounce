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
package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

fun <T : HotbarItemSlot> Iterable<T>.findClosestSlot(item: Item): T? =
    findClosestSlot { it.item === item }

fun <T : HotbarItemSlot> Iterable<T>.findClosestSlot(itemTag: TagKey<Item>): T? =
    findClosestSlot { it.`is`(itemTag) }

fun <T : HotbarItemSlot> Iterable<T>.findClosestSlot(vararg items: Item): T? =
    findClosestSlot { it.item in items }

fun <T : HotbarItemSlot> Iterable<T>.findClosestSlot(items: Collection<Item>): T? =
    findClosestSlot { it.item in items }

inline fun <T : HotbarItemSlot> Iterable<T>.findClosestSlot(predicate: (ItemStack) -> Boolean): T? {
    var candidate: T? = null
    for (slot in this) {
        if (!predicate(slot.itemStack)) continue
        candidate = if (candidate == null) {
            slot
        } else {
            minOf(candidate, slot, HotbarItemSlot.PREFER_NEARBY)
        }
    }
    return candidate
}

class Slots<T : ItemSlot>(private val slots: List<T>) : List<T> by slots {
    val stacks: Array<ItemStack>
        get() = slots.mapToArray { it.itemStack }

    val items: Array<Item>
        get() = slots.mapToArray { it.itemStack.item }

    fun findSlot(item: Item): T? = findSlot { it.item === item }

    inline fun findSlot(predicate: (ItemStack) -> Boolean): T? {
        return if (mc.player == null) null else find { predicate(it.itemStack) }
    }

    fun findSlot(predicate: Predicate<ItemStack>): T? = findSlot(predicate::test)

    operator fun plus(other: Slots<*>): Slots<ItemSlot> {
        return Slots(this.slots + other.slots)
    }

    operator fun plus(other: ItemSlot): Slots<ItemSlot> {
        return Slots(this.slots + other)
    }

    companion object {
        /**
         * Hotbar 0~8
         */
        @JvmField
        val Hotbar = Slots(HotbarItemSlot.mainHandSlots)

        /**
         * Inventory 0~26
         */
        @JvmField
        val Inventory = Slots(InventoryItemSlot.ALL)

        /**
         * Hotbar + Inventory
         */
        @JvmField
        val HotbarAndInventory = Hotbar + Inventory

        /**
         * Armor slots 0~3
         *
         * Boots/Leggings/Chestplate/Helmet
         */
        @JvmField
        val Armor = Slots(ArmorItemSlot.entries)

        /**
         * Offhand + Hotbar
         */
        @JvmField
        val OffhandWithHotbar = Slots(HotbarItemSlot.entries)

        /**
         * Hotbar + OffHand + Inventory + Armor
         */
        @JvmField
        val All = Hotbar + HotbarItemSlot.OFFHAND + Inventory + Armor
    }
}
