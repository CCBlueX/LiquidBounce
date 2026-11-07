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
package net.ccbluex.liquidbounce.utils.collection

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.utils.collection.ExpiringList.Companion.ExpiringList
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ExpiringListTest {

    private val owner = object : EventListener {
        override var running = true
    }
    private val list = owner.ExpiringList<String>()

    @AfterTest
    fun unregister() {
        EventManager.unregisterEventHandler(owner)
    }

    private fun tick(count: Int = 1) {
        repeat(count) { EventManager.callEvent(GameTickEvent) }
    }

    @Test
    fun `short lived entries expire behind a long lived entry`() {
        list.add("long", 10)
        list.add("short", 1)

        tick()

        assertEquals(listOf("long"), list.rawValues().toList())
        assertEquals(9, list.timeToDie(list.single()))
    }

    @Test
    fun `lifetime changes remove expired entries throughout the list`() {
        list.add("old lifetime", 10)
        tick(2)
        list.add("short lifetime", 1)
        list.add("medium lifetime", 3)

        tick()
        assertEquals(listOf("old lifetime", "medium lifetime"), list.rawValues().toList())
        tick(2)
        assertEquals(listOf("old lifetime"), list.rawValues().toList())
        tick(5)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `surviving values preserve insertion order`() {
        list.add("first", 5)
        list.add("expired", 1)
        list.add("second", 4)
        list.add("also expired", 1)
        list.add("third", 3)

        tick()

        assertEquals(listOf("first", "second", "third"), list.rawValues().toList())
    }

    @Test
    fun `equal deadlines expire together`() {
        list.add("first", 2)
        list.add("second", 2)

        tick()
        assertEquals(2, list.size)
        tick()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `raw value view reflects expiration and later additions`() {
        val values = list.rawValues()
        list.add("long", 5)
        list.add("short", 1)

        tick()
        list.add("new", 1)

        assertEquals(listOf("long", "new"), values.toList())
        tick()
        assertEquals(listOf("long"), values.toList())
    }

    @Test
    fun `clear removes entries and new lifetimes start at the current tick`() {
        list.add("old", 10)
        tick(3)

        list.clear()
        list.add("new", 2)

        assertEquals(2, list.timeToDie(list.single()))
        tick()
        assertEquals(listOf("new"), list.rawValues().toList())
        tick()
        assertTrue(list.isEmpty())
    }

    @Test
    fun `inactive owner pauses expiration`() {
        list.add("value", 2)
        owner.running = false

        tick(4)

        assertEquals(2, list.timeToDie(list.single()))
        owner.running = true
        tick(2)
        assertTrue(list.isEmpty())
    }

    @Test
    fun `nonpositive lifetimes are removed on the next tick even behind live entries`() {
        list.add("long", 3)
        list.add("zero", 0)
        list.add("negative", -1)

        tick()

        assertEquals(listOf("long"), list.rawValues().toList())
    }
}
