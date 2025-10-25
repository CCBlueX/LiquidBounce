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

@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import java.util.Collections
import java.util.EnumMap
import kotlin.experimental.ExperimentalTypeInference

fun <T> Array<out T>?.unmodifiable(): List<T> =
    when {
        isNullOrEmpty() -> emptyList()
        size == 1 -> Collections.singletonList(this[0])
        else -> ObjectImmutableList(this)
    }

inline fun <T> Comparator<in T>.max(a: T, b: T): T = if (compare(a, b) > 0) a else b
inline fun <T> Comparator<in T>.min(a: T, b: T): T = if (compare(a, b) < 0) a else b

inline fun <reified K : Enum<K>, V> enumMapOf(): EnumMap<K, V> = EnumMap(K::class.java)

@JvmName("enumMapOfBuilder")
@OptIn(ExperimentalTypeInference::class)
inline fun <reified K : Enum<K>, V> enumMapOf(
    @BuilderInference block: EnumMap<K, V>.() -> Unit
): EnumMap<K, V> = enumMapOf<K, V>().apply(block)

@JvmName("enumMapOfMapper")
inline fun <reified K : Enum<K>, V> enumMapOf(
    mapper: (K) -> V
): EnumMap<K, V> = enumMapOf<K, V> {
    K::class.java.enumConstants.forEach {
        put(it, mapper(it))
    }
}

@JvmName("enumMapOfPairs")
inline fun <reified K : Enum<K>, V> enumMapOf(vararg pairs: Pair<K, V>): EnumMap<K, V> =
    enumMapOf<K, V>().apply { putAll(pairs) }
