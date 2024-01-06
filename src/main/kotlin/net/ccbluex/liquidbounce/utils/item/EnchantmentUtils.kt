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
package net.ccbluex.liquidbounce.utils.item

import net.minecraft.enchantment.Enchantment
import net.minecraft.enchantment.EnchantmentHelper
import net.minecraft.item.ItemStack
import net.minecraft.nbt.NbtCompound
import net.minecraft.nbt.NbtElement
import net.minecraft.nbt.NbtList

fun addEnchantment(item: ItemStack, enchantment: Enchantment, level: Int?) {
    val nbt = item.orCreateNbt
    if (nbt?.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt()) == false) {
        nbt.put(ItemStack.ENCHANTMENTS_KEY, NbtList())
    }
    val nbtList = nbt?.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
    nbtList?.add(
        EnchantmentHelper.createNbt(
            EnchantmentHelper.getEnchantmentId(enchantment),
            level ?: enchantment.maxLevel
        )
    )
}

fun removeEnchantment(item: ItemStack, enchantment: Enchantment) {
    val nbt = item.nbt ?: return
    if (!nbt.contains(ItemStack.ENCHANTMENTS_KEY, NbtElement.LIST_TYPE.toInt())) {
        return
    }
    val nbtList = nbt.getList(ItemStack.ENCHANTMENTS_KEY, NbtElement.COMPOUND_TYPE.toInt())
    nbtList.removeIf {
        (it as NbtCompound).getString("id") == EnchantmentHelper.getEnchantmentId(enchantment).toString()
    }
}

fun clearEnchantments(item: ItemStack) {
    val nbt = item.nbt ?: return
    nbt.remove(ItemStack.ENCHANTMENTS_KEY)
}
