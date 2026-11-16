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
package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.world.item.ItemStack

/**
 * Lets the farm walk to crops, soil and items out of reach; without it, the farm works only where the player stands.
 */
object AutoFarmAutoWalk : ToggleableValueGroup(ModuleAutoFarm, "AutoWalk", true) {

    // Makes the player move to farmland blocks where there is a need for crop replacement
    val toPlant by boolean("ToPlant", true, aliases = listOf("ToPlace"))

    object ToItems : ToggleableValueGroup(this, "ToItems", true) {
        val range by float("Range", 20f, 8f..64f)

        private val items by items("Items", itemSortedSetOf())
        private val filter by enumChoice("Filter", Filter.BLACKLIST)

        fun shouldPickUp(stack: ItemStack): Boolean = filter(stack.item, items)
    }

    init {
        tree(ToItems)
    }

}
