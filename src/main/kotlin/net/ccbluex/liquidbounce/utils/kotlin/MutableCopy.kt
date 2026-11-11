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
package net.ccbluex.liquidbounce.utils.kotlin

import net.ccbluex.liquidbounce.utils.client.Chronometer
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.lang.reflect.Array as JavaArray

private val lookup = MethodHandles.publicLookup()

private val copiers = object : ClassValue<MethodHandle?>() {
    override fun computeValue(type: Class<*>) = copierOf(type)
}

/**
 * A copy of [value] if it is changed in place: collections, maps, arrays, atomics and [Chronometer]s. Anything else
 * is returned as it is, which is safe for immutable values.
 */
internal fun copyIfMutable(value: Any?): Any? = when (value) {
    null -> null
    is Chronometer -> value.copy()
    is AtomicBoolean -> AtomicBoolean(value.get())
    is AtomicInteger -> AtomicInteger(value.get())
    is AtomicLong -> AtomicLong(value.get())
    is AtomicReference<*> -> AtomicReference(value.get())
    else -> if (value.javaClass.isArray) copyArray(value) else copiers.get(value.javaClass)?.invoke(value) ?: value
}

private fun copyArray(array: Any): Any {
    val length = JavaArray.getLength(array)
    val copy = JavaArray.newInstance(array.javaClass.componentType, length)
    System.arraycopy(array, 0, copy, 0, length)
    return copy
}

/**
 * Prefers `clone()`, which keeps the exact class with its settings (capacity, comparator, access order).
 * Collections that cannot be cloned are copied with their copy constructor.
 */
private fun copierOf(type: Class<*>): MethodHandle? {
    val container = when {
        Collection::class.java.isAssignableFrom(type) -> Collection::class.java
        Map::class.java.isAssignableFrom(type) -> Map::class.java
        else -> return null
    }

    val asCopier = MethodType.methodType(Any::class.java, Any::class.java)
    if (Cloneable::class.java.isAssignableFrom(type)) {
        runCatching {
            return lookup.findVirtual(type, "clone", MethodType.methodType(Any::class.java)).asType(asCopier)
        }
    }

    return runCatching {
        lookup.findConstructor(type, MethodType.methodType(Void.TYPE, container)).asType(asCopier)
    }.getOrNull()
}
