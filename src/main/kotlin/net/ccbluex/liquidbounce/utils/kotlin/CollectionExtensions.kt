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

@file:Suppress("NOTHING_TO_INLINE")
package net.ccbluex.liquidbounce.utils.kotlin

import it.unimi.dsi.fastutil.objects.ObjectArraySet
import it.unimi.dsi.fastutil.objects.ObjectImmutableList
import it.unimi.dsi.fastutil.objects.ObjectLinkedOpenHashSet
import java.util.Collections
import java.util.SequencedCollection
import java.util.function.Predicate

fun <T> Array<out T>?.unmodifiable(): List<T> =
    when {
        isNullOrEmpty() -> emptyList()
        size == 1 -> Collections.singletonList(this[0])
        else -> ObjectImmutableList(this)
    }

fun <E> Collection<E>.toOrderedSet(): Set<E> {
    return when (this.size) {
        0 -> emptySet()
        1 -> Collections.singleton(if (this is SequencedCollection) this.first else this.iterator().next())
        in 2..4 -> ObjectArraySet(this)
        else -> ObjectLinkedOpenHashSet(this)
    }
}

fun <T> Iterable<Predicate<T>>.matchesAny(t: T): Boolean =
    any { it.test(t) }

fun <T> Iterable<Predicate<T>>.matchesAll(t: T): Boolean =
    all { it.test(t) }
