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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.annotations.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@OptIn(ExperimentalCoroutinesApi::class)
class GuardScopeTest {

    private val ticker = ManualTicker()

    private class Flag(var value: Boolean = true) : Condition {
        var checks = 0

        override fun holds(): Boolean {
            checks++
            return value
        }
    }

    @Tag("GuardScopeTestEvent")
    private class ProbeEvent(val id: Int) : Event()

    @Test
    fun `a condition failing mid-wait stops before the code after the wait`() = runTest(UnconfinedTestDispatcher()) {
        val alive = Flag()
        var resumed = false
        val result = async(ticker) {
            guarded(alive) {
                waitTicks(5)
                resumed = true
            }
        }

        ticker.tick(2)
        alive.value = false
        ticker.tick()

        assertEquals(Guarded.Interrupted(alive), result.await())
        assertFalse(resumed)
        assertEquals(0, ticker.taskCount)
    }

    @Test
    fun `a condition failing on the resume tick stops`() = runTest(UnconfinedTestDispatcher()) {
        val alive = Flag()
        var resumed = false
        val result = async(ticker) {
            guarded(alive) {
                waitTicks(3)
                resumed = true
            }
        }

        ticker.tick(2)
        alive.value = false
        ticker.tick()

        assertEquals(Guarded.Interrupted(alive), result.await())
        assertFalse(resumed)
    }

    @Test
    fun `a condition failing between the last tick and the resumption stops`() = runTest {
        val alive = Flag()
        var resumed = false
        val result = async(ticker) {
            guarded(alive) {
                waitTicks(1)
                resumed = true
            }
        }
        runCurrent()

        ticker.tick()
        alive.value = false
        runCurrent()

        assertEquals(Guarded.Interrupted(alive), result.await())
        assertFalse(resumed)
    }

    @Test
    fun `a wait predicate never sees a broken condition`() = runTest(UnconfinedTestDispatcher()) {
        var target: String? = "target"
        val result = async(ticker) {
            guarded({ target != null }) {
                tickUntil { target!!.isEmpty() }
            }
        }

        ticker.tick()
        target = null
        ticker.tick()

        assertTrue(result.await().isInterrupted())
    }

    @Test
    fun `an inner condition ends only the inner scope`() = runTest(UnconfinedTestDispatcher()) {
        val outer = Flag()
        val inner = Flag()
        var afterInnerScope = false
        val result = async(ticker) {
            guarded(outer) {
                guarded(inner) {
                    waitTicks(5)
                }.also { afterInnerScope = true }
            }
        }

        inner.value = false
        ticker.tick()

        assertEquals(Guarded.Completed(Guarded.Interrupted(inner)), result.await())
        assertTrue(afterInnerScope)
    }

    @Test
    fun `an outer condition still applies inside an inner scope`() = runTest(UnconfinedTestDispatcher()) {
        val outer = Flag()
        val inner = Flag()
        var afterInnerScope = false
        val result = async(ticker) {
            guarded(outer) {
                guarded(inner) {
                    waitTicks(5)
                }
                afterInnerScope = true
            }
        }

        outer.value = false
        ticker.tick()

        assertEquals(Guarded.Interrupted(outer), result.await())
        assertFalse(afterInnerScope)
    }

    @Test
    fun `conditions added inside the scope apply from then on`() = runTest(UnconfinedTestDispatcher()) {
        val late = Flag()
        val result = async(ticker) {
            guarded {
                waitTicks(1)
                guard(late)
                waitTicks(5)
            }
        }

        ticker.tick()
        assertEquals(1, late.checks)
        late.value = false
        ticker.tick()

        assertEquals(Guarded.Interrupted(late), result.await())
    }

    @Test
    fun `a condition that does not hold at the start skips the block`() = runTest(UnconfinedTestDispatcher()) {
        val alive = Flag(false)
        var ran = false

        val result = guarded(alive) { ran = true }

        assertEquals(Guarded.Interrupted(alive), result)
        assertFalse(ran)
    }

    @Test
    fun `a completed scope returns the value of its block`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(ticker) {
            guarded(Flag()) {
                waitTicks(2)
                42
            }
        }

        ticker.tick(2)

        val outcome = result.await()
        if (outcome.isInterrupted()) fail("Interrupted by ${outcome.condition}")
        assertEquals(42, outcome.value)
        assertNull(Guarded.Interrupted(Flag()).getOrNull())
    }

    @Test
    fun `tracked values follow the game and end the scope once gone`() = runTest(UnconfinedTestDispatcher()) {
        var current: String? = "first"
        val seen = mutableListOf<String>()
        lateinit var targetRef: Tracked<String>
        val result = async(ticker) {
            guarded {
                targetRef = tracked { current }
                val target by targetRef
                while (true) {
                    seen += target
                    waitTicks(1)
                }
            }
        }

        current = "second"
        ticker.tick()
        current = null
        ticker.tick()

        assertEquals(Guarded.Interrupted(targetRef), result.await())
        assertEquals(listOf("first", "second"), seen)
    }

    @Test
    fun `same holds only while the instance stays the same`() = runTest(UnconfinedTestDispatcher()) {
        var world = Any()
        lateinit var sameWorld: Condition
        val result = async(ticker) {
            guarded {
                sameWorld = same { world }
                waitTicks(5)
            }
        }

        ticker.tick()
        world = Any()
        ticker.tick()

        assertEquals(Guarded.Interrupted(sameWorld), result.await())
    }

    @Test
    fun `disabling the listener cancels the work instead of reporting an interruption`() = runTest {
        val listener = object : EventListener {
            override var running = true
        }
        var resumed = false
        var afterScope = false
        var cleanedUp = false
        val job = launch(ticker + listener.wrapContinuationInterceptor()) {
            guarded(Flag()) {
                try {
                    waitTicks(2)
                    resumed = true
                } finally {
                    cleanedUp = true
                }
            }
            afterScope = true
        }

        ticker.tick()
        listener.running = false
        ticker.tick()
        job.join()

        assertTrue(job.isCancelled)
        assertFalse(resumed)
        assertFalse(afterScope)
        assertTrue(cleanedUp)
    }

    @Test
    fun `exceptions from the block pass through`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(ticker) {
            runCatching {
                guarded(Flag()) {
                    waitTicks(1)
                    error("boom")
                }
            }
        }

        ticker.tick()

        val failure = result.await().exceptionOrNull()
        assertTrue(failure is IllegalStateException && failure.message == "boom", "Got $failure")
    }

    @Test
    fun `exceptions from a condition reach the waiting code`() = runTest(UnconfinedTestDispatcher()) {
        var fail = false
        val result = async(ticker) {
            runCatching {
                guarded({ check(!fail) { "condition failed" }; true }) {
                    waitTicks(5)
                }
            }
        }

        fail = true
        ticker.tick()

        val failure = result.await().exceptionOrNull()
        assertTrue(failure is IllegalStateException && failure.message == "condition failed", "Got $failure")
    }

    @Test
    fun `an event wait stops on the tick its condition fails`() = runTest(UnconfinedTestDispatcher()) {
        val listener = object : EventListener {}
        val alive = Flag()
        val result = async(ticker) {
            guarded(alive) {
                listener.waitMatches<ProbeEvent> { it.id == 1 }.id
            }
        }

        alive.value = false
        ticker.tick()

        assertEquals(Guarded.Interrupted(alive), result.await())
        assertEquals(0, ticker.taskCount)
        EventManager.callEvent(ProbeEvent(1))
    }

    @Test
    fun `an event matching after its condition failed does not resume the code`() =
        runTest(UnconfinedTestDispatcher()) {
            val listener = object : EventListener {}
            val alive = Flag()
            val result = async(ticker) {
                guarded(alive) {
                    listener.waitMatches<ProbeEvent> { it.id == 2 }.id
                }
            }

            alive.value = false
            EventManager.callEvent(ProbeEvent(2))

            assertEquals(Guarded.Interrupted(alive), result.await())
        }

    @Test
    fun `an event wait returns the event while its condition holds`() = runTest(UnconfinedTestDispatcher()) {
        val listener = object : EventListener {}
        val result = async(ticker) {
            guarded(Flag()) {
                listener.waitMatches<ProbeEvent> { it.id == 3 }.id
            }
        }

        ticker.tick()
        EventManager.callEvent(ProbeEvent(3))
        ticker.tick()

        assertEquals(Guarded.Completed(3), result.await())
        assertEquals(0, ticker.taskCount)
    }

    @Test
    fun `waits outside a guarded scope behave as before`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(ticker) { tickUntil { it >= 2 } }

        ticker.tick(2)

        assertEquals(2, result.await())
    }

}
