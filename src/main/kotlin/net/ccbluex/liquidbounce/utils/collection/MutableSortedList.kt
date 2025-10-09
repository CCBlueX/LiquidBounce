/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

package net.ccbluex.liquidbounce.utils.collection

import net.ccbluex.fastutil.objectMutableListOf

private const val TOO_SMALL = -1
private const val TOO_BIG = 1
private const val LEGAL = 0

@Suppress("TooManyFunctions")
class MutableSortedList<E, K : Comparable<K>>(
    private val list : MutableList<E> = objectMutableListOf(),
    // Includes
    private val lowerBound: K? = null,
    // Excludes
    private val upperBound: K? = null,
    private val keySelector: (E) -> K,
) : MutableList<E> by list {

    private val comparator = compareBy(keySelector)

    init {
        list.sortWith(comparator)

        if (list.isNotEmpty()) {
            val first = list.first()
            val last = list.last()

            lowerBound?.let {
                require(keySelector(first) < it) {
                    "First element is smaller than lower bound"
                }
            }
            upperBound?.let {
                require(keySelector(last) >= it) {
                    "Last element is larger than upper bound"
                }
            }
        }
    }

    private fun checkBounds(element: E): Int {
        lowerBound?.let {
            if (keySelector(element) < it) return TOO_SMALL
        }
        upperBound?.let {
            if (keySelector(element) >= it) return TOO_BIG
        }
        return LEGAL
    }

    private fun insertionIndex(element: E): Int {
        val index = list.binarySearch(element, comparator)
        return if (index >= 0) index else index.inv()
    }

    override fun add(element: E): Boolean {
        return when (checkBounds(element)) {
            TOO_SMALL -> false
            TOO_BIG -> false
            else -> {
                val idx = insertionIndex(element)
                list.add(idx, element)
                true
            }
        }
    }

    override fun addAll(elements: Collection<E>): Boolean {
        var changed = false
        for (e in elements) changed = add(e) || changed
        return changed
    }

    override fun add(index: Int, element: E) {
        throw UnsupportedOperationException("Cannot insert at arbitrary index in sorted list")
    }

    override fun addAll(index: Int, elements: Collection<E>): Boolean {
        throw UnsupportedOperationException("Cannot insert at arbitrary index in sorted list")
    }

    override fun set(index: Int, element: E): E {
        throw UnsupportedOperationException("Cannot replace element in sorted list")
    }

    override fun sort(c: Comparator<in E>?) {
        throw UnsupportedOperationException("Sorting is managed automatically")
    }

    override fun iterator(): MutableIterator<E> = listIterator()

    override fun listIterator(): MutableListIterator<E> = listIterator(0)

    override fun listIterator(index: Int): MutableListIterator<E> =
        object : MutableListIterator<E> by list.listIterator(index) {
            override fun set(element: E) {
                throw UnsupportedOperationException("Cannot replace element in sorted list")
            }

            override fun add(element: E) {
                throw UnsupportedOperationException("Cannot add from iterator in sorted list")
            }
        }

    override fun subList(fromIndex: Int, toIndex: Int): MutableList<E> {
        return MutableSortedList(
            list.subList(fromIndex, toIndex),
            lowerBound?.let { maxOf(it, keySelector(list[fromIndex])) } ?: keySelector(list[fromIndex]),
            upperBound?.let { minOf(it, keySelector(list[toIndex - 1])) } ?: keySelector(list[toIndex - 1]),
            keySelector,
        )
    }

    override fun toString() = list.toString()

}
