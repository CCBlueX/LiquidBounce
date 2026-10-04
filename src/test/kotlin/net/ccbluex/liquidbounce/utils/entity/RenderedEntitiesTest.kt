/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License, either version 3 of the License, or
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
package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RenderedEntitiesTest {

    // Equal listeners still own separate subscriptions and callbacks.
    private data class TestListener(val name: String) : EventListener {
        override val running = true
    }

    private val first = TestListener("subscriber")
    private val second = TestListener("subscriber")

    @AfterTest
    fun unsubscribe() {
        RenderedEntities.unsubscribe(first)
        RenderedEntities.unsubscribe(second)
    }

    @Test
    fun `resubscribing before an update does not resurrect the previous callback`() {
        var oldCalls = 0
        var newCalls = 0
        var otherCalls = 0
        RenderedEntities.subscribe(second)
        with(second) { RenderedEntities.onUpdated { otherCalls++ } }
        RenderedEntities.subscribe(first)
        with(first) { RenderedEntities.onUpdated { oldCalls++ } }

        RenderedEntities.unsubscribe(first)
        RenderedEntities.subscribe(first)
        with(first) { RenderedEntities.onUpdated { newCalls++ } }
        EventManager.callEvent(WorldChangeEvent(null))

        assertEquals(0, oldCalls)
        assertEquals(1, newCalls)
        assertEquals(1, otherCalls)
    }

    @Test
    fun `repeated toggles between updates keep only the current callbacks`() {
        RenderedEntities.subscribe(second)
        var calls = 0

        repeat(4) {
            RenderedEntities.subscribe(first)
            with(first) { RenderedEntities.onUpdated { calls++ } }
            RenderedEntities.unsubscribe(first)
        }
        RenderedEntities.subscribe(first)
        with(first) { RenderedEntities.onUpdated { calls++ } }
        EventManager.callEvent(WorldChangeEvent(null))

        assertEquals(1, calls)
    }

    @Test
    fun `unsubscription removes every callback owned by that listener`() {
        RenderedEntities.subscribe(second)
        RenderedEntities.subscribe(first)
        var staleCalls = 0
        with(first) {
            RenderedEntities.onUpdated { staleCalls++ }
            RenderedEntities.onUpdated { staleCalls++ }
        }

        RenderedEntities.unsubscribe(first)
        RenderedEntities.subscribe(first)
        EventManager.callEvent(WorldChangeEvent(null))

        assertEquals(0, staleCalls)
    }

    @Test
    fun `unsubscription preserves equal listeners and does not call their callbacks early`() {
        RenderedEntities.subscribe(first)
        RenderedEntities.subscribe(second)
        var firstCalls = 0
        var secondCalls = 0
        with(first) { RenderedEntities.onUpdated { firstCalls++ } }
        with(second) { RenderedEntities.onUpdated { secondCalls++ } }

        RenderedEntities.unsubscribe(first)

        assertTrue(RenderedEntities.running)
        assertEquals(0, secondCalls)
        EventManager.callEvent(WorldChangeEvent(null))
        assertEquals(0, firstCalls)
        assertEquals(1, secondCalls)
    }

    @Test
    fun `the last unsubscription stops tracking and discards its callbacks`() {
        var calls = 0
        RenderedEntities.subscribe(first)
        with(first) { RenderedEntities.onUpdated { calls++ } }

        RenderedEntities.unsubscribe(first)

        assertFalse(RenderedEntities.running)
        assertTrue(RenderedEntities.isEmpty())
        RenderedEntities.subscribe(first)
        EventManager.callEvent(WorldChangeEvent(null))
        assertEquals(0, calls)
    }

}
