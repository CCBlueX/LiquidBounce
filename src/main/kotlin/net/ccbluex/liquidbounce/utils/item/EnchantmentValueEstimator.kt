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

import net.ccbluex.liquidbounce.annotations.ValueClassCandidate
import net.minecraft.core.component.DataComponents
import net.minecraft.core.registries.Registries
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import kotlin.jvm.optionals.getOrNull

class EnchantmentValueEstimator(
    private vararg val weightedEnchantments: WeightedEnchantment,
) : Comparator<ItemStack> {

    /**
     * @see net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel
     */
    fun estimateValue(itemStack: ItemStack): Float {
        val enchantments = itemStack[DataComponents.ENCHANTMENTS]
        if (enchantments == null || enchantments.isEmpty) return 0f

        val registry = Registries.ENCHANTMENT.getOrNull() ?: return 0f

        var sum = 0.0f

        for (it in this.weightedEnchantments) {
            sum += enchantments.getLevel(registry[it.enchantment].getOrNull() ?: continue) * it.factor
        }

        return sum
    }

    override fun compare(o1: ItemStack, o2: ItemStack): Int =
        this.estimateValue(o1).compareTo(this.estimateValue(o2))

    @ValueClassCandidate
    class WeightedEnchantment(@JvmField val enchantment: ResourceKey<Enchantment>, @JvmField val factor: Float)
}
