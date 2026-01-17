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
@file:Suppress("NOTHING_TO_INLINE", "detekt:TooManyFunctions")

package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.doubles.DoubleIterable
import net.ccbluex.fastutil.forEachDouble
import java.util.concurrent.ThreadLocalRandom
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

inline fun range(iterable1: DoubleIterable, iterable2: DoubleIterable, operation: (Double, Double) -> Unit) {
    iterable1.forEachDouble { d1 ->
        iterable2.forEachDouble { d2 ->
            operation(d1, d2)
        }
    }
}

inline fun range(
    iterable1: DoubleIterable,
    iterable2: DoubleIterable,
    iterable3: DoubleIterable,
    operation: (Double, Double, Double) -> Unit,
) {
    iterable1.forEachDouble { d1 ->
        iterable2.forEachDouble { d2 ->
            iterable3.forEachDouble { d3 ->
                operation(d1, d2, d3)
            }
        }
    }
}

inline fun range(
    iterable1: IntProgression,
    iterable2: IntProgression,
    iterable3: IntProgression,
    operation: (Int, Int, Int) -> Unit
) {
    iterable1.forEach { d1 ->
        iterable2.forEach { d2 ->
            iterable3.forEach { d3 ->
                operation(d1, d2, d3)
            }
        }
    }
}

fun ClosedFloatingPointRange<Float>.random(): Float {
    return if (start >= endInclusive) start else ThreadLocalRandom.current().nextFloat(start, endInclusive)
}

inline operator fun ClosedFloatingPointRange<Float>.unaryMinus(): ClosedFloatingPointRange<Float> {
    return -endInclusive..-start
}

fun ClosedFloatingPointRange<Double>.random(): Double {
    return if (start >= endInclusive) start else ThreadLocalRandom.current().nextDouble(start, endInclusive)
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
