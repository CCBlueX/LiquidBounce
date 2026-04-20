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

package net.ccbluex.liquidbounce.features.module.modules.world.automobheal

import net.minecraft.core.component.DataComponents
import net.minecraft.tags.TagKey
import net.minecraft.util.ToFloatFunction
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import java.util.function.Predicate

@JvmRecord
data class MobFoodOption(
    val test: Predicate<ItemStack>,
    val healAmount: ToFloatFunction<ItemStack>,
    val isBucket: Boolean = false,
) {

    constructor(
        tag: TagKey<Item>,
        healAmount: ToFloatFunction<ItemStack>,
        isBucket: Boolean = false,
    ) : this({ it.`is`(tag) }, healAmount, isBucket)

    constructor(
        item: Item,
        healAmount: ToFloatFunction<ItemStack>,
        isBucket: Boolean = false,
    ) : this({ it.item === item }, healAmount, isBucket)

    constructor(
        item: Item,
        healAmount: Float,
        isBucket: Boolean = false,
    ) : this(item, { healAmount }, isBucket)

    companion object {
        @JvmStatic
        fun foodNutritionHeal(stack: ItemStack, multiplier: Float): Float {
            val foodProperties = stack.get(DataComponents.FOOD)
            return (foodProperties?.nutrition() ?: 1) * multiplier
        }

        @JvmStatic
        fun ofBucket(item: Item): MobFoodOption {
            return MobFoodOption(
                item = item,
                healAmount = 1f,
                isBucket = true,
            )
        }
    }

}
