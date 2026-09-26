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

import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment

class EnchantmentValueEstimator(
    private vararg val weightedEnchantments: WeightedEnchantment,
) : Comparator<ItemStack> {

    fun estimateValue(itemStack: ItemStack): Float {
        var sum = 0.0f

        for (it in this.weightedEnchantments) {
            sum += itemStack.getEnchantment(it.enchantment) * it.factor
        }

        return sum
    }

    override fun compare(o1: ItemStack, o2: ItemStack): Int =
        this.estimateValue(o1).compareTo(this.estimateValue(o2))

    class WeightedEnchantment(val enchantment: ResourceKey<Enchantment>, val factor: Float)
}
