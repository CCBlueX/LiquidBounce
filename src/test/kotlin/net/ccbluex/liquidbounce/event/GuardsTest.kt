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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.annotations.Tag
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class GuardsTest {

    private val ticker = ManualTicker()

    private class Flag(var value: Boolean = true) {
        var checks = 0

        fun holds(): Boolean {
            checks++
            return value
        }
    }

    @Tag("GuardsTestEvent")
    private class ProbeEvent(val id: Int) : Event()

    /** How the last run started with [run] ended: null when it finished. */
    private var ended: Throwable? = null

    private fun TestScope.run(block: suspend CoroutineScope.() -> Unit): Job =
        launch(ticker + Guards(), block = block).also { job -> job.invokeOnCompletion { ended = it } }

    private fun assertEndedByGuard() = assertTrue(ended is GuardInterruption, "The run ended with $ended")

    @Test
    fun `a requirement failing mid-wait ends the run before the code after the wait`() =
        runTest(UnconfinedTestDispatcher()) {
            val alive = Flag()
            var resumed = false
            run {
                requires { alive.holds() }
                waitTicks(5)
                resumed = true
            }

            ticker.tick(2)
            alive.value = false
            ticker.tick()

            assertEndedByGuard()
            assertFalse(resumed)
            assertEquals(0, ticker.taskCount)
        }

    @Test
    fun `a required value gone mid-wait ends the run`() = runTest(UnconfinedTestDispatcher()) {
        var current: String? = "first"
        val seen = mutableListOf<String>()
        run {
            val target by requires { current }
            while (true) {
                seen += target
                waitTicks(1)
            }
        }

        current = "second"
        ticker.tick()
        current = null
        ticker.tick()

        assertEndedByGuard()
        assertEquals(listOf("first", "second"), seen)
    }

    @Test
    fun `a requirement failing on the resume tick ends the run`() = runTest(UnconfinedTestDispatcher()) {
        val alive = Flag()
        var resumed = false
        run {
            requires { alive.holds() }
            waitTicks(3)
            resumed = true
        }

        ticker.tick(2)
        alive.value = false
        ticker.tick()

        assertEndedByGuard()
        assertFalse(resumed)
    }

    @Test
    fun `a requirement failing between the last tick and the resumption ends the run`() = runTest {
        val alive = Flag()
        var resumed = false
        run {
            requires { alive.holds() }
            waitTicks(1)
            resumed = true
        }
        runCurrent()

        ticker.tick()
        alive.value = false
        runCurrent()

        assertEndedByGuard()
        assertFalse(resumed)
    }

    @Test
    fun `a wait predicate never sees a failed requirement`() = runTest(UnconfinedTestDispatcher()) {
        var target: String? = "target"
        run {
            requires { target != null }
            tickUntil { target!!.isEmpty() }
        }

        ticker.tick()
        target = null
        ticker.tick()

        assertEndedByGuard()
    }

    @Test
    fun `a requirement added after a suspension only applies from then on`() = runTest(UnconfinedTestDispatcher()) {
        val late = Flag()
        run {
            waitTicks(1)
            requires { late.holds() }
            waitTicks(5)
        }

        ticker.tick()
        assertEquals(1, late.checks)
        late.value = false
        ticker.tick()

        assertEndedByGuard()
    }

    @Test
    fun `a requirement that is false when added ends the run right there`() = runTest(UnconfinedTestDispatcher()) {
        var ran = false
        run {
            requires { false }
            ran = true
        }

        assertEndedByGuard()
        assertFalse(ran)
    }

    @Test
    fun `unchanged holds only while the instance stays the same`() = runTest(UnconfinedTestDispatcher()) {
        var world = Any()
        run {
            unchanged { world }
            waitTicks(5)
        }

        ticker.tick()
        world = Any()
        ticker.tick()

        assertEndedByGuard()
    }

    @Test
    fun `a run started in a world ends once that world is left`() = runTest(UnconfinedTestDispatcher()) {
        Worlds.changed(inWorld = true)
        try {
            var resumed = false
            run {
                beginRun()
                waitTicks(5)
                resumed = true
            }

            ticker.tick()
            Worlds.changed(inWorld = true)
            ticker.tick()

            assertEndedByGuard()
            assertFalse(resumed)
        } finally {
            Worlds.changed(inWorld = false)
        }
    }

    @Test
    fun `a run started outside of a world keeps going when one is joined`() = runTest(UnconfinedTestDispatcher()) {
        Worlds.changed(inWorld = false)
        var resumed = false
        try {
            run {
                beginRun()
                waitTicks(2)
                resumed = true
            }

            ticker.tick()
            Worlds.changed(inWorld = true)
            ticker.tick()

            assertNull(ended)
            assertTrue(resumed)
        } finally {
            Worlds.changed(inWorld = false)
        }
    }

    @Test
    fun `a coroutine that is no handler run outlives a world change`() = runTest(UnconfinedTestDispatcher()) {
        Worlds.changed(inWorld = true)
        var resumed = false
        try {
            run {
                waitTicks(2)
                resumed = true
            }

            ticker.tick()
            Worlds.changed(inWorld = true)
            ticker.tick()

            assertNull(ended)
            assertTrue(resumed)
        } finally {
            Worlds.changed(inWorld = false)
        }
    }

    @Test
    fun `cleanup in finally runs when a requirement ends the run`() = runTest(UnconfinedTestDispatcher()) {
        val alive = Flag()
        var cleanedUp = false
        run {
            requires { alive.holds() }
            try {
                waitTicks(5)
            } finally {
                cleanedUp = true
            }
        }

        alive.value = false
        ticker.tick()

        assertTrue(cleanedUp)
    }

    @Test
    fun `withContext stays in the run, a launched coroutine is a run of its own`() =
        runTest(UnconfinedTestDispatcher()) {
            val inner = Flag()
            val child = Flag()
            var childEnded: Throwable? = null
            var resumed = false
            run {
                withContext(ticker) { requires { inner.holds() } }
                launch {
                    requires { child.holds() }
                    waitTicks(5)
                }.invokeOnCompletion { childEnded = it }
                waitTicks(2)
                resumed = true
            }

            child.value = false
            ticker.tick()
            assertTrue(childEnded is GuardInterruption, "The child ended with $childEnded")

            ticker.tick()
            assertNull(ended)
            assertTrue(resumed)
            assertTrue(inner.checks > 1, "The requirement added in withContext was not checked by the run")
        }

    @Test
    fun `exceptions from the run pass through`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(ticker + Guards()) {
            runCatching {
                requires { true }
                waitTicks(1)
                error("boom")
            }
        }

        ticker.tick()

        val failure = result.await().exceptionOrNull()
        assertTrue(failure is IllegalStateException && failure.message == "boom", "Got $failure")
    }

    @Test
    fun `exceptions from a requirement reach the waiting code`() = runTest(UnconfinedTestDispatcher()) {
        var fail = false
        val result = async(ticker + Guards()) {
            runCatching {
                requires { check(!fail) { "requirement failed" }; true }
                waitTicks(5)
            }
        }

        fail = true
        ticker.tick()

        val failure = result.await().exceptionOrNull()
        assertTrue(failure is IllegalStateException && failure.message == "requirement failed", "Got $failure")
    }

    @Test
    fun `an event wait ends the run on the tick its requirement fails`() = runTest(UnconfinedTestDispatcher()) {
        val listener = object : EventListener {}
        val alive = Flag()
        run {
            requires { alive.holds() }
            listener.waitMatches<ProbeEvent> { it.id == 1 }
        }

        alive.value = false
        ticker.tick()

        assertEndedByGuard()
        assertEquals(0, ticker.taskCount)
        EventManager.callEvent(ProbeEvent(1))
    }

    @Test
    fun `an event matching after its requirement failed does not resume the code`() =
        runTest(UnconfinedTestDispatcher()) {
            val listener = object : EventListener {}
            val alive = Flag()
            var resumed = false
            run {
                requires { alive.holds() }
                listener.waitMatches<ProbeEvent> { it.id == 2 }
                resumed = true
            }

            alive.value = false
            EventManager.callEvent(ProbeEvent(2))

            assertEndedByGuard()
            assertFalse(resumed)
        }

    @Test
    fun `an event wait returns the event while its requirement holds`() = runTest(UnconfinedTestDispatcher()) {
        val listener = object : EventListener {}
        val result = async(ticker + Guards()) {
            requires { true }
            listener.waitMatches<ProbeEvent> { it.id == 3 }.id
        }

        ticker.tick()
        EventManager.callEvent(ProbeEvent(3))
        ticker.tick()

        assertEquals(3, result.await())
        assertEquals(0, ticker.taskCount)
    }

    @Test
    fun `waits that require nothing behave as before`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(ticker) { tickUntil { it >= 2 } }

        ticker.tick(2)

        assertEquals(2, result.await())
    }

    @Test
    fun `a failing requirement ends only that run of that handler`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val guarding = object : EventListener {}
        val other = object : EventListener {}
        val alive = Flag()
        val finished = mutableListOf<String>()

        with(SuspendHandlerBehavior.DiscardLatest.Default) {
            guarding.createEventHook(ProbeEvent::class.java, ticker + dispatcher, 0) { event ->
                if (event.id == 1) {
                    requires { alive.holds() }
                }
                waitTicks(2)
                finished += "guarding ${event.id}"
            }
            other.createEventHook(ProbeEvent::class.java, ticker + dispatcher, 0) { event ->
                waitTicks(2)
                finished += "other ${event.id}"
            }
        }

        try {
            EventManager.callEvent(ProbeEvent(1))
            alive.value = false
            ticker.tick(2)
            assertEquals(listOf("other 1"), finished)

            // Its next run starts without the requirement of the one before, though that one still fails
            EventManager.callEvent(ProbeEvent(2))
            ticker.tick(2)
            assertEquals(listOf("other 1", "guarding 2", "other 2"), finished)
        } finally {
            for (listener in arrayOf(guarding, other)) {
                EventManager.unregisterEventHandler(listener)
                listener.removeEventListenerScope()
            }
        }
    }

    @Test
    fun `runs instead of the handler cancel its run and go one after the other`() = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        val listener = object : EventListener {}
        val log = mutableListOf<String>()
        val handler = ExclusiveHandler(listener, ProbeEvent::class.java, ticker + dispatcher, 0, null) { event ->
            log += "start ${event.id}"
            waitTicks(5)
            log += "end ${event.id}"
        }

        try {
            EventManager.callEvent(ProbeEvent(1))
            handler.runInstead {
                waitTicks(2)
                log += "first"
            }
            handler.runInstead {
                waitTicks(2)
                log += "second"
            }
            EventManager.callEvent(ProbeEvent(2))
            ticker.tick(6)

            EventManager.callEvent(ProbeEvent(3))
            ticker.tick(5)
            assertEquals(listOf("start 1", "first", "second", "start 3", "end 3"), log)
        } finally {
            EventManager.unregisterEventHandler(listener)
            listener.removeEventListenerScope()
        }
    }

    @Test
    fun `disabling the listener cancels the run instead of ending it by a requirement`() = runTest {
        val listener = object : EventListener {
            override var running = true
        }
        var resumed = false
        var cleanedUp = false
        val job = launch(ticker + Guards() + listener.wrapContinuationInterceptor()) {
            requires { true }
            try {
                waitTicks(2)
                resumed = true
            } finally {
                cleanedUp = true
            }
        }
        job.invokeOnCompletion { ended = it }

        ticker.tick()
        listener.running = false
        ticker.tick()
        job.join()

        assertTrue(ended is EventListenerNotListeningException, "The run ended with $ended")
        assertFalse(resumed)
        assertTrue(cleanedUp)
    }

}
