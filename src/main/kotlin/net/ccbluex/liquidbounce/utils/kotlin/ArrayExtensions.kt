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
@file:Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.doubles.DoubleIterable
import it.unimi.dsi.fastutil.ints.IntLinkedOpenHashSet
import it.unimi.dsi.fastutil.ints.IntSet
import net.ccbluex.fastutil.mapToIntArray
import java.util.*
import java.util.stream.Stream

inline infix operator fun IntRange.contains(range: IntRange): Boolean {
    return this.first <= range.first && this.last >= range.last
}

fun ClosedFloatingPointRange<Float>.valueAtProportion(proportion: Float): Float {
    return when {
        proportion >= 1f -> endInclusive
        proportion <= 0f -> start
        else -> start + (endInclusive - start) * proportion
    }
}

fun ClosedFloatingPointRange<Float>.proportionOfValue(value: Float): Float {
    return when {
        value >= endInclusive -> 1f
        value <= start -> 0f
        else -> (value - start) / (endInclusive - start)
    }
}

inline fun range(iterable: DoubleIterable, operation: (Double) -> Unit) {
    iterable.doubleIterator().apply {
        while (hasNext()) {
            operation(nextDouble())
        }
    }
}

inline fun range(iterable: IntProgression, operation: (Int) -> Unit) {
    iterable.iterator().apply {
        while (hasNext()) {
            operation(nextInt())
        }
    }
}

inline fun range(iterable1: DoubleIterable, iterable2: DoubleIterable, operation: (Double, Double) -> Unit) {
    range(iterable1) { d1 ->
        range(iterable2) { d2 ->
            operation(d1, d2)
        }
    }
}

inline fun range(
    iterable1: IntProgression,
    iterable2: IntProgression,
    iterable3: IntProgression,
    operation: (Int, Int, Int) -> Unit
) {
    range(iterable1) { d1 ->
        range(iterable2) { d2 ->
            range(iterable3) { d3 ->
                operation(d1, d2, d3)
            }
        }
    }
}

fun ClosedFloatingPointRange<Float>.random(): Float {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return (start + (endInclusive - start) * Math.random()).toFloat()
}

fun ClosedFloatingPointRange<Double>.random(): Double {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return start + (endInclusive - start) * Math.random()
}

fun ClosedFloatingPointRange<Float>.toDouble(): ClosedFloatingPointRange<Double> {
    require(start.isFinite())
    require(endInclusive.isFinite())
    return start.toDouble()..endInclusive.toDouble()
}

fun <T> List<T>.subList(fromIndex: Int): List<T> {
    return this.subList(fromIndex, this.size)
}

fun <T> MutableList<T>.removeRange(fromInclusive: Int = 0, endExclusive: Int = this.size) {
    this.subList(fromInclusive, endExclusive).clear()
}

/**
 * A JavaScript-styled forEach
 */
inline fun <T, C : Collection<T>> C.forEachWithSelf(action: (T, index: Int, self: C) -> Unit) {
    forEachIndexed { i, item ->
        action(item, i, this)
    }
}

inline fun <reified T : Enum<T>> Array<out T>.toEnumSet(): EnumSet<T> =
    toCollection(emptyEnumSet())

inline fun <reified T : Enum<T>> Iterable<T>.toEnumSet(): EnumSet<T> =
    toCollection(emptyEnumSet())

inline fun <reified T : Enum<T>> emptyEnumSet(): EnumSet<T> =
    EnumSet.noneOf(T::class.java)

inline fun <T> Collection<T>.mapIntSet(transform: (T) -> Int): IntSet {
    return IntLinkedOpenHashSet(mapToIntArray(transform))
}

/**
 * Inserts a new element into a sorted list while maintaining the order.
 */
inline fun <T, K : Comparable<K>> MutableList<T>.sortedInsert(item: T, crossinline selector: (T) -> K?) {
    val insertIndex = binarySearchBy(selector(item), selector = selector).let {
        if (it >= 0) it else it.inv()
    }

    add(insertIndex, item)
}

/**
 * Transform a String to another String with same length by given [transform]
 */
inline fun String.mapString(transform: (Char) -> Char) = String(CharArray(length) {
    transform(this[it])
})

/**
 * Transform a Collection to a String with by given [transform]
 */
inline fun <T> Collection<T>.mapString(transform: (T) -> Char) = with(iterator()) {
    String(CharArray(size) {
        transform(next())
    })
}

inline fun <reified T> Stream<T>.toTypedArray(): Array<T> = toArray(::arrayOfNulls)
