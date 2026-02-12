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
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.item.enchantment.EnchantmentHelper
import net.minecraft.world.item.enchantment.ItemEnchantments

private val ItemStack.componentTypeForEnchantment
    inline get() = EnchantmentHelper.getComponentType(this)

fun ItemStack.removeEnchantment(enchantment: Holder<Enchantment>) {
    EnchantmentHelper.updateEnchantments(this) { it.set(enchantment, 0) }
}

fun ItemStack.clearEnchantments() =
    EnchantmentHelper.setEnchantments(this, ItemEnchantments.EMPTY)

fun ItemStack?.getEnchantmentCount(): Int =
    this?.get(componentTypeForEnchantment)?.size() ?: 0

fun ItemStack?.getEnchantment(enchantment: ResourceKey<Enchantment>): Int {
    if (this == null) return 0
    val enchantmentEntry = enchantment.toRegistryEntryOrNull() ?: return 0
    return EnchantmentHelper.getItemEnchantmentLevel(enchantmentEntry, this)
}
