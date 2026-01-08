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

package net.ccbluex.liquidbounce.event

import it.unimi.dsi.fastutil.objects.ObjectArrays
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update
import java.util.Collections.emptyIterator

/**
 * A special [java.util.concurrent.CopyOnWriteArrayList] that stores [EventHook]s.
 *
 * All elements are ordered descending by [EventHook.priority].
 *
 * Equality of [EventHook]s is defined by reference equality.
 */
class EventHookRegistry<E : Event> : Iterable<EventHook<E>> {

    // sorted descending by EventHook.priority
    private val array = atomic(ObjectArrays.EMPTY_ARRAY)

    fun addIfAbsent(eventHook: EventHook<E>) {
        array.update { curr ->
            if (curr === ObjectArrays.EMPTY_ARRAY) {
                return@update arrayOf(eventHook)
            }

            @Suppress("UNCHECKED_CAST")
            val comparatorAny = comparator as Comparator<Any>
            val index = ObjectArrays.binarySearch(curr, eventHook, comparatorAny)
            if (index >= 0) {
                // found some element with same priority -> scan the equal-range for exact match
                // find start of equal-range
                val endOfRange = curr.findIdEqual(mid = index, element = eventHook, comparator = comparatorAny) {
                    return@update curr
                }

                // insert at position `endOfRange` (after the equal-range)
                curr.insertAt(endOfRange, eventHook)
            } else {
                // not found -> insertion point
                curr.insertAt(index.inv(), eventHook)
            }
        }
    }

    fun remove(eventHook: EventHook<E>) {
        array.update { curr ->
            if (curr === ObjectArrays.EMPTY_ARRAY) return@update curr

            if (curr.size == 1) {
                return@update if (curr[0] === eventHook) ObjectArrays.EMPTY_ARRAY else curr
            }

            @Suppress("UNCHECKED_CAST")
            val comparatorAny = comparator as Comparator<Any>
            val index = ObjectArrays.binarySearch(curr, eventHook, comparatorAny)
            if (index < 0) return@update curr

            // search equal-priority range for exact match
            curr.findIdEqual(mid = index, element = eventHook, comparator = comparatorAny) {
                return@update curr.removeAt(it)
            }

            // no exact match found -> return original array
            curr
        }
    }

    fun remove(eventListener: EventListener) {
        array.update {
            var newArray = ObjectArrays.EMPTY_ARRAY
            var newSize = 0
            for (i in 0 until it.size) {
                @Suppress("UNCHECKED_CAST")
                if ((it[i] as EventHook<E>).handlerClass !== eventListener) {
                    if (newArray === ObjectArrays.EMPTY_ARRAY) {
                        newArray = arrayOfNulls<Any>(it.size - i)
                    }

                    newArray[newSize++] = it[i]
                }
            }

            if (newSize == newArray.size) {
                newArray
            } else {
                val result = arrayOfNulls<Any>(newSize)
                newArray.copyInto(result, endIndex = newSize)
                result
            }
        }
    }

    fun clear() {
        array.value = ObjectArrays.EMPTY_ARRAY
    }

    override fun iterator(): Iterator<EventHook<E>> {
        val snapshot = array.value
        @Suppress("UNCHECKED_CAST")
        return if (snapshot.isEmpty()) {
            emptyIterator()
        } else {
            snapshot.iterator() as Iterator<EventHook<E>>
        }
    }

    companion object {
        @JvmStatic
        private val comparator = Comparator<EventHook<*>> { a, b ->
            b.priority.compareTo(a.priority)
        }

        @JvmStatic
        private fun Array<Any?>.insertAt(index: Int, element: Any?): Array<Any?> {
            val newArray = arrayOfNulls<Any>(size + 1)
            copyInto(newArray, 0, 0, index)
            newArray[index] = element
            copyInto(newArray, index + 1, index, size)
            return newArray
        }

        @JvmStatic
        private fun Array<Any?>.removeAt(index: Int): Array<Any?> {
            val newArray = arrayOfNulls<Any>(size - 1)
            copyInto(newArray, 0, 0, index)
            copyInto(newArray, index, index + 1, size)
            return newArray
        }

        /**
         * Finds the index of the first element with the same priority as [element] starting from [mid].
         *
         * If [element] is found, [onFound] is invoked with the index of [element] and the index is returned.
         *
         * If [element] is not found, the index where [element] would be inserted is returned.
         */
        private inline fun Array<Any>.findIdEqual(
            mid: Int,
            element: Any,
            comparator: Comparator<Any>,
            onFound: (Int) -> Unit,
        ): Int {
            if (element === this[mid]) {
                onFound(mid)
                return mid
            }

            var i = mid - 1
            while (i >= 0 && comparator.compare(this[i], this[mid]) == 0) {
                if (this[i] === element) {
                    onFound(i)
                    return i
                }
                i--
            }

            i = mid + 1
            while (i < this.size && comparator.compare(this[i], this[mid]) == 0) {
                if (this[i] === element) {
                    onFound(i)
                    return i
                }
                i++
            }

            return i
        }
    }

}
