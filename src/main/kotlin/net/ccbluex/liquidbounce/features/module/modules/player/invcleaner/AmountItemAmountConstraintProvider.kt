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

/**
 * Keeps items based on the configured item groups.
 *
 * Groups configured by the user take precedence over the fallback limits, which are only applied for categories the
 * user did not configure.
 */
class AmountItemAmountConstraintProvider(
    /**
     * Fallback amount per function an item provides, e.g. how many hunger points of food to keep.
     */
    private val fallbackAmountPerFunction: Map<ItemFunction, Int> = emptyMap(),
    /**
     * Groups configured by the user, e.g. `[water bucket] -> 2`.
     */
    private val configuredItemsInSpecificCategories: List<Pair<List<ItemCategory>, Int>> = emptyList(),
    /**
     * Fallback limits for the categories that the user did not configure.
     */
    private val fallbackItemsInSpecificCategories: List<Pair<List<ItemCategory>, Int>> = emptyList(),
) : ItemAmountConstraintProvider {
    /**
     * Contains all specific item groups in which an item is.
     *
     * For these rules: `[egg, snowball] -> 32, [egg, carrot] -> 64`, this list would look like this:
     * - `egg` -> `[0, 1]`
     * - `snowball` -> `[0]`
     * - `carrot` -> `[1]`
     */
    private val itemSpecificGroupMap: Map<ItemCategory, List<SpecificItemGroup>> = run {
        val configuredCategories = configuredItemsInSpecificCategories
            .flatMapTo(HashSet()) { it.first }

        val assignments = ArrayList<Pair<ItemCategory, SpecificItemGroup>>()
        var nextId = 0

        fun register(items: List<ItemCategory>, desiredAmount: Int, default: Boolean) {
            val id = nextId++

            val group = SpecificItemGroup(
                id = id,
                desiredAmount = desiredAmount,
                priority = id,
                default = default,
            )

            items.forEach { assignments.add(it to group) }
        }

        configuredItemsInSpecificCategories.forEach { (items, desiredAmount) -> register(items, desiredAmount, false) }
        fallbackItemsInSpecificCategories
            .filterNot { (items, _) -> items.any { it in configuredCategories } }
            .forEach { (items, desiredAmount) -> register(items, desiredAmount, true) }

        assignments.groupBy({ it.first }, { it.second })
    }

    override fun getConstraints(facet: ItemFacet): List<ItemConstraintInfo> {
        val constraints = ArrayList<ItemConstraintInfo>()

        val specificGroups = this.itemSpecificGroupMap[facet.category]

        if (specificGroups != null) {
            for (group in specificGroups) {
                val info = ItemConstraintInfo(
                    group = SpecificItemGroupConstraintGroup(
                        acceptableRange = group.desiredAmount..Integer.MAX_VALUE,
                        priority = group.priority,
                        groupId = group.id
                    ),
                    amountAddedByItem = facet.itemStack.count,
                    default = group.default
                )

                constraints.add(info)
            }
        }

        for ((function, amountAdded) in facet.providedItemFunctions) {
            val info = ItemConstraintInfo(
                group = ItemFunctionCategoryConstraintGroup(
                    this.fallbackAmountPerFunction.getOrDefault(function, 1)..Integer.MAX_VALUE,
                    1000,
                    function
                ),
                amountAddedByItem = amountAdded,
                default = true
            )

            constraints.add(info)
        }

        if (specificGroups == null && facet.providedItemFunctions.isEmpty()
            && facet.category.type != GenericItemType.ANY_ITEM
        ) {
            val defaultDesiredAmount = if (facet.category.type.oneIsSufficient) 1 else Integer.MAX_VALUE

            val info = ItemConstraintInfo(
                group = ItemCategoryConstraintGroup(
                    defaultDesiredAmount..Integer.MAX_VALUE,
                    1000,
                    facet.category
                ),
                amountAddedByItem = facet.itemStack.count,
                default = true
            )

            constraints.add(info)
        }

        return constraints
    }

    override fun getAllocationPriority(itemGroup: ItemCategory): Int {
        // Categories without a rule are processed last, so the groups the user cares about are counted first.
        return this.itemSpecificGroupMap[itemGroup]?.minOf { it.priority } ?: Int.MAX_VALUE
    }

    private class SpecificItemGroup(
        val id: Int,
        val desiredAmount: Int,
        val priority: Int,
        val default: Boolean,
    )
}
