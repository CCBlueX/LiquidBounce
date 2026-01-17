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
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(item: Item): T? =
    findClosestSlot { it.item === item }

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(vararg items: Item): T? =
    findClosestSlot { it.item in items }

fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(items: Collection<Item>): T? =
    findClosestSlot { it.item in items }

inline fun <T : HotbarItemSlot> SlotGroup<T>.findClosestSlot(predicate: (ItemStack) -> Boolean): T? {
    return this.filter { predicate(it.itemStack) }.minWithOrNull(HotbarItemSlot.PREFER_NEARBY)
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
        listOf(
            ArmorItemSlot(EquipmentSlot.FEET), // 0
            ArmorItemSlot(EquipmentSlot.LEGS), // 1
            ArmorItemSlot(EquipmentSlot.CHEST), // 2
            ArmorItemSlot(EquipmentSlot.HEAD), // 3
        )
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
    val stacks: Array<ItemStack>
        get() = slots.mapToArray { it.itemStack }

    val items: Array<Item>
        get() = slots.mapToArray { it.itemStack.item }

    fun findSlot(item: Item): T? {
        return findSlot { it.item === item }
    }

    inline fun findSlot(predicate: (ItemStack) -> Boolean): T? {
        return if (mc.player == null) null else find { predicate(it.itemStack) }
    }

    operator fun plus(other: SlotGroup<*>): SlotGroup<ItemSlot> {
        return SlotGroup(this.slots + other.slots)
    }

    operator fun plus(other: ItemSlot): SlotGroup<ItemSlot> {
        return SlotGroup(this.slots + other)
    }
}
