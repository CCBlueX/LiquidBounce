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

package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner

/**
 * Defines an item constraint group.
 *
 * For example if we had two constraints:
 * - `BLOCKS` -> `128..`
 * - `TNT` -> `..64`
 *
 * Imagine a situation where the player has 125 TNT:
 * - If the TNT was processed first it would be thrown out since the TNT limit says that we have too much TNT.
 * - If the BLOCKS constraint was processed first, the TNT would be kept since the BLOCKS constraint is not yet
 * satisfied.
 */
abstract class ItemNumberConstraintGroup(
    /**
     * The range of desired item amounts (which might be raw item counts, food saturation, etc.):
     * - The lower limit defines the desired amount of items (=> any more items *might* be thrown out)
     * - The upper limit defines the maximum amount of items (=> any more items *will* be thrown out)
     */
    val acceptableRange: IntRange,
    /**
     * The priority of this constraint group. Lower values are processed first.
     * It Affects the order in which items are processed.
     */
    val priority: Int,
) {
    abstract override fun hashCode(): Int
    abstract override fun equals(other: Any?): Boolean
}

class ItemCategoryConstraintGroup(
    acceptableRange: IntRange,
    priority: Int,
    val category: ItemCategory,
) : ItemNumberConstraintGroup(acceptableRange, priority) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemCategoryConstraintGroup

        return category == other.category
    }

    override fun hashCode(): Int {
        return category.hashCode()
    }
}

class ItemFunctionCategoryConstraintGroup(
    acceptableRange: IntRange,
    priority: Int,
    val function: ItemFunction,
) : ItemNumberConstraintGroup(acceptableRange, priority) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ItemFunctionCategoryConstraintGroup

        return function == other.function
    }

    override fun hashCode(): Int {
        return function.hashCode()
    }
}

class ItemConstraintInfo(
    val group: ItemNumberConstraintGroup,
    val amountAddedByItem: Int
)
