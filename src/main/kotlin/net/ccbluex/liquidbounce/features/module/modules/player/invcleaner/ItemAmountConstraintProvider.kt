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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items.ItemFacet

interface ItemAmountConstraintProvider {
    fun getConstraints(item: ItemFacet): ArrayList<ItemConstraintInfo>

    /**
     * Returns the priority of the given item category.
     * Categories with values are processed first.
     *
     * This is useful when it comes to finding the minimal number of items required to fulfill the constraints.
     * For example, if the constraints were `egg -> 64, egg, snowball -> 32`, it would be important to process the eggs
     * first so that no snowballs are kept when having > 32 eggs.
     */
    fun getAllocationPriority(itemGroup: ItemCategory): Int

    /**
     * Filters out not applying default configurations.
     *
     * See [ItemConstraintInfo.default] for further information on that.
     */
    fun getApplyingConstraints(item: ItemFacet): ArrayList<ItemConstraintInfo> {
        val constraints = getConstraints(item)

        if (constraints.any { !it.default }) {
            constraints.removeIf { it.default }
        }

        return constraints
    }
}
