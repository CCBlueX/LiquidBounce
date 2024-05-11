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

import net.minecraft.component.DataComponentTypes
import net.minecraft.component.type.ItemEnchantmentsComponent
import net.minecraft.enchantment.Enchantment
import net.minecraft.item.ItemStack

fun addEnchantment(item: ItemStack, enchantment: Enchantment, level: Int) {
    item.addEnchantment(enchantment, level)
}

fun removeEnchantment(item: ItemStack, enchantment: Enchantment) {
    val enchantmentComponent = item.get(DataComponentTypes.ENCHANTMENTS) ?: return

    val builder = ItemEnchantmentsComponent.Builder(enchantmentComponent)

    builder.remove { it.value() == enchantment }

    item.set(DataComponentTypes.ENCHANTMENTS, builder.build())
}

fun clearEnchantments(item: ItemStack) {
    item.set(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT)
}
