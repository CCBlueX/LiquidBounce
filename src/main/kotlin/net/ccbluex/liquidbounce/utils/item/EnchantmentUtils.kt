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
package net.ccbluex.liquidbounce.utils.item

import net.minecraft.core.Holder
import net.minecraft.core.component.DataComponents
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.ItemEnchantments

fun ItemStack.removeEnchantment(enchantment: Holder<Enchantment>) {
    val enchantmentComponent = get(DataComponents.ENCHANTMENTS) ?: return

    val builder = ItemEnchantments.Mutable(enchantmentComponent)

    builder.removeIf { it == enchantment }

    set(DataComponents.ENCHANTMENTS, builder.toImmutable())
}

fun ItemStack.clearEnchantments() {
    set(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)
}
