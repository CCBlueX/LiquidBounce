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

import kotlin.collections.AbstractList

/**
 * Creates a virtual [List] represents the lists get from receiver with [transform].
 * It assumes all transformed list are immutable. Modify them leads to undefined behavior.
 */
@Suppress("CognitiveComplexMethod")
inline fun <T, R> List<T>.flatMapVirtual(transform: (T) -> List<R>): List<R> {
    val prefixSums = IntArray(this.size + 1)

    val flatten = Array(this.size) { i ->
        val list = transform(this[i])
        prefixSums[i + 1] = prefixSums[i] + list.size
        list
    }

    val totalSize = prefixSums[this.size]

    return object : AbstractList<R>() {
        private fun findOuterForIndex(index: Int): Int {
            // index is assumed to be in 0..totalSize-1 when called from get()
            var pos = prefixSums.binarySearch(index)
            if (pos >= 0) {
                // move to the rightmost equal so we map to the last prefix <= index
                while (pos + 1 < prefixSums.size && prefixSums[pos + 1] == index) pos++
                return pos
            } else {
                return -pos - 2
            }
        }

        override val size: Int
            get() = totalSize

        override fun get(index: Int): R {
            if (index !in 0 until totalSize) {
                throw IndexOutOfBoundsException("$index not in 0 until $totalSize")
            }
            val outer = findOuterForIndex(index)
            val offset = index - prefixSums[outer]
            return flatten[outer][offset]
        }

        override fun iterator() = listIterator(0)

        override fun listIterator(index: Int): ListIterator<R> {
            if (index !in 0..totalSize) {
                throw IndexOutOfBoundsException("$index not in 0..$totalSize")
            }

            // Special-case empty result
            if (totalSize == 0) {
                return emptyList<R>().listIterator()
            }

            val (startOuter, startInner) = if (index == totalSize) {
                // Position at the end: find outer containing last element
                val outer = findOuterForIndex(totalSize - 1)
                val inner = prefixSums[outer + 1] - prefixSums[outer] // = flatten[outer].size
                outer to inner
            } else {
                val outer = findOuterForIndex(index)
                val inner = index - prefixSums[outer]
                outer to inner
            }

            return object : ListIterator<R> {
                private var outerIndex = startOuter
                private var innerIndex = startInner
                private var globalIndex = index

                override fun hasNext(): Boolean = globalIndex < size
                override fun hasPrevious(): Boolean = globalIndex > 0

                override fun next(): R {
                    if (!hasNext()) throw NoSuchElementException()
                    // innerIndex must be valid because hasNext true
                    val v = flatten[outerIndex][innerIndex]
                    stepForward()
                    return v
                }

                override fun previous(): R {
                    if (!hasPrevious()) throw NoSuchElementException()
                    stepBackward()
                    return flatten[outerIndex][innerIndex]
                }

                override fun nextIndex(): Int = globalIndex
                override fun previousIndex(): Int = globalIndex - 1

                private fun stepForward() {
                    globalIndex++
                    innerIndex++
                    // advance outerIndex over empty lists if needed
                    while (outerIndex < flatten.size && innerIndex >= flatten[outerIndex].size) {
                        outerIndex++
                        innerIndex = 0
                    }
                }

                private fun stepBackward() {
                    globalIndex--
                    innerIndex--
                    while (outerIndex >= 0 && innerIndex < 0) {
                        outerIndex--
                        // if outerIndex >= 0 then we position innerIndex to last element index
                        innerIndex = if (outerIndex >= 0) flatten[outerIndex].size - 1 else -1
                    }
                }
            }
        }
    }
}
