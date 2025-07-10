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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.TickedValue
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.FIRST_PRIORITY

class ExpiringList<E> private constructor(
    val owner: EventListener,
    private val list: ArrayDeque<TickedValue<E>>,
) : Collection<TickedValue<E>> by list {

    class TickedValue<T>(val value: T, val expiration: Int)

    companion object {
        @JvmStatic
        fun <E> EventListener.ExpiringList(): ExpiringList<E> {
            return ExpiringList(this, ArrayDeque())
        }
    }

    private var tick = 0

    private val tickHandler = owner.handler<GameTickEvent>(priority = FIRST_PRIORITY) {
        tick++

        while (list.isNotEmpty() && list.first().expiration <= tick) {
            list.removeFirst()
        }
    }

    fun clear() = list.clear()

    fun timeToDie(item: TickedValue<E>): Int {
        return item.expiration - tick
    }

    fun add(element: E, ttl: Int) {
        list.add(TickedValue(element, tick + ttl))
    }

    private var rawValues: Collection<E>? = null

    private inner class RawValueView : Collection<E> {

        override val size: Int get() = list.size

        override fun isEmpty(): Boolean = list.isEmpty()

        override fun contains(element: E): Boolean = list.any { it.value == element }

        override fun iterator(): Iterator<E> = object : Iterator<E> {
            private val iterator = list.iterator()
            override fun hasNext(): Boolean = iterator.hasNext()
            override fun next(): E {
                if (!hasNext()) throw NoSuchElementException()
                return iterator.next().value
            }
        }

        override fun containsAll(elements: Collection<E>): Boolean =
            elements.all { contains(it) }
    }

    fun rawValues(): Collection<E> {
        return rawValues ?: RawValueView().also { rawValues = it }
    }

}
