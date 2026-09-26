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
package net.ccbluex.liquidbounce.utils.sorting

class ComparatorChain<T>(private vararg val comparisonFunctions: Comparator<in T>) : Comparator<T> {

    override fun compare(o1: T, o2: T): Int {
        for (comparisonFunction in this.comparisonFunctions) {
            val comparisonResult = comparisonFunction.compare(o1, o2)

            if (comparisonResult != 0) {
                return comparisonResult
            }
        }

        return 0
    }

}

/**
 * false first
 */
@Deprecated(
    message = "Use standard compareValuesBy instead",
    replaceWith = ReplaceWith("compareValuesBy(a, b, cond)"),
)
inline fun <T> compareValueByCondition(a: T, b: T, cond: (T) -> Boolean): Int {
    return compareValuesBy(a, b, cond)
}

/**
 * false first
 */
@Deprecated(
    message = "Use standard compareBy instead",
    replaceWith = ReplaceWith("compareBy(cond)"),
)
inline fun <T> compareByCondition(crossinline cond: (T) -> Boolean): Comparator<T> {
    return compareBy(cond)
}
