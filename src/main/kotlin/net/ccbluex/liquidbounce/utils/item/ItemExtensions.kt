/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils.item

import com.mojang.brigadier.StringReader
import net.minecraft.client.MinecraftClient
import net.minecraft.command.argument.ItemStackArgument
import net.minecraft.command.argument.ItemStringReader
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.*
import net.minecraft.nbt.NbtCompound
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

/**
 * Create item with NBT tags
 *
 * @docs https://minecraft.gamepedia.com/Commands/give
 */
fun createItem(stack: String, amount: Int = 1): ItemStack = ItemStringReader(StringReader(stack), true).consume().let {
    ItemStackArgument(it.item, it.nbt).createStack(amount, false)
}

fun findHotbarSlot(item: Item): Int? = findHotbarSlot { it.item == item }

fun findHotbarSlot(predicate: (ItemStack) -> Boolean): Int? {
    val player = MinecraftClient.getInstance().player ?: return null

    return (0..8).firstOrNull { predicate(player.inventory.getStack(it)) }
}

fun findInventorySlot(item: Item): Int? = findInventorySlot { it.item == item }

fun findInventorySlot(predicate: (ItemStack) -> Boolean): Int? {
    val player = MinecraftClient.getInstance().player ?: return null

    return (0..40).firstOrNull { predicate(player.inventory.getStack(it)) }
}

/**
 * Check if a stack is nothing (means empty slot)
 */
fun ItemStack?.isNothing() = this?.isEmpty == true

fun ItemStack?.getEnchantmentCount(): Int {
    val enchantments = this?.enchantments ?: return 0

    var c = 0

    for (enchantment in enchantments) {
        if (enchantment !is NbtCompound) {
            continue
        }

        if (enchantment.contains("ench") || enchantment.contains("id")) {
            c++
        }
    }

    return c
}

fun ItemStack?.getEnchantment(enchantment: Enchantment): Int {
    val enchantments = this?.enchantments ?: return 0
    val enchId = Registry.ENCHANTMENT.getId(enchantment)

    for (enchantmentEntry in enchantments) {
        if (enchantmentEntry !is NbtCompound) {
            continue
        }

        if (enchantmentEntry.contains("id") && Identifier.tryParse(enchantmentEntry.getString("id")) == enchId) {
            return enchantmentEntry.getShort("lvl").toInt()
        }
    }

    return 0
}

fun isInHotbar(slot: Int) = slot == 40 || slot in 0..8

val ToolItem.type: Int
    get() = when (this) {
        is AxeItem -> 0
        is PickaxeItem -> 1
        is ShovelItem -> 2
        is HoeItem -> 3
        else -> throw IllegalStateException()
    }

val Item.attackDamage: Float
    get() =
        when (this) {
            is SwordItem -> this.attackDamage
            is ToolItem -> this.material.attackDamage
            else -> throw IllegalArgumentException()
        }
