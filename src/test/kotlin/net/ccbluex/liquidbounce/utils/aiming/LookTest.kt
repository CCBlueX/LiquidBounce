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
package net.ccbluex.liquidbounce.utils.aiming

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.GuardInterruption
import net.ccbluex.liquidbounce.event.Guards
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.utils.aiming.data.Rotation
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Drives [Looks] the way the rotation update does each tick, with [going] as the rotation that goes out.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class LookTest {

    private val looks = Looks()
    private val module = object : EventListener {}
    private val submitted = mutableListOf<RotationTarget>()

    private fun tick(going: Rotation, accept: (RotationTarget) -> Boolean = { true }) {
        submitted.clear()
        looks.request { target, _, provider ->
            assertSame(module, provider)
            submitted += target
        }
        looks.offer(going, submitted.filter(accept))
        looks.settle()
    }

    private fun yawTarget(yaw: Float) = RotationTarget(yaw, null)

    private fun RotationTarget.near(going: Rotation) = going.takeIf { kotlin.math.abs(it.yaw - yaw!!) < 1f }

    @Test
    fun `the code after the look runs within the update of the tick the rotation meets the target`() =
        runTest(UnconfinedTestDispatcher()) {
            var acted: Rotation? = null
            launch(looks) {
                acted = module.lookAt(Priority.NORMAL, { yawTarget(90f) }) { going -> near(going) }
            }

            tick(Rotation(30f, 0f))
            assertEquals(null, acted)
            tick(Rotation(89.5f, 0f))

            assertEquals(Rotation(89.5f, 0f), acted)
            assertEquals(0, looks.size)
        }

    @Test
    fun `the target is planned again every tick`() = runTest(UnconfinedTestDispatcher()) {
        var plans = 0
        launch(looks) {
            module.lookAt<Rotation>(Priority.NORMAL, { yawTarget(10f * ++plans) }) { going -> near(going) }
        }

        tick(Rotation(0f, 0f))
        tick(Rotation(0f, 0f))
        assertEquals(2, plans)
        assertEquals(yawTarget(20f).yaw, submitted.single().yaw)

        tick(Rotation(30f, 0f))
        assertEquals(0, looks.size)
    }

    @Test
    fun `a rotation that is not this look's request is not offered to it`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(looks) {
            module.lookAt(Priority.NORMAL, { yawTarget(0f) }) { going -> near(going) }
        }

        tick(Rotation(0f, 0f)) { false }
        assertFalse(result.isCompleted)

        tick(Rotation(0f, 0f))
        assertEquals(Rotation(0f, 0f), result.await())
    }

    @Test
    fun `a failing guard stops the request before the target is planned`() = runTest(UnconfinedTestDispatcher()) {
        var alive = true
        var plans = 0
        var acted = false
        var ended: Throwable? = null
        launch(looks + Guards()) {
            requires { alive }
            module.lookAt(Priority.NORMAL, { plans++; yawTarget(90f) }) { going -> near(going) }
            acted = true
        }.invokeOnCompletion { ended = it }

        tick(Rotation(0f, 0f))
        alive = false
        tick(Rotation(90f, 0f))

        assertTrue(ended is GuardInterruption, "The run ended with $ended")
        assertEquals(1, plans)
        assertTrue(submitted.isEmpty())
        assertFalse(acted)
        assertEquals(0, looks.size)
    }

    @Test
    fun `a guard that fails on the tick the target is met stops before acting`() = runTest(UnconfinedTestDispatcher()) {
        var alive = true
        var acted = false
        var ended: Throwable? = null
        launch(looks + Guards()) {
            requires { alive }
            module.lookAt(Priority.NORMAL, { yawTarget(90f) }) { going ->
                // The target dies while the update runs, after the guard was checked for this tick
                alive = false
                near(going)
            }
            acted = true
        }.invokeOnCompletion { ended = it }

        tick(Rotation(90f, 0f))

        assertTrue(ended is GuardInterruption, "The run ended with $ended")
        assertFalse(acted)
    }

    @Test
    fun `a cancelled look is dropped without asking for its rotation again`() = runTest(UnconfinedTestDispatcher()) {
        var plans = 0
        val job = launch(looks) {
            module.lookAt<Rotation>(Priority.NORMAL, { plans++; yawTarget(90f) }) { going -> near(going) }
        }

        tick(Rotation(0f, 0f))
        job.cancel()
        tick(Rotation(0f, 0f))

        assertEquals(1, plans)
        assertEquals(0, looks.size)
    }

    @Test
    fun `exceptions from planning reach the waiting code`() = runTest(UnconfinedTestDispatcher()) {
        val result = async(looks) {
            runCatching { module.lookAt(Priority.NORMAL, { error("no plan") }) { going -> near(going) } }
        }

        tick(Rotation(0f, 0f))

        assertEquals("no plan", result.await().exceptionOrNull()?.message)
    }

    @Test
    fun `a look started by resumed code waits for the next update`() = runTest(UnconfinedTestDispatcher()) {
        val acted = mutableListOf<Rotation>()
        launch(looks) {
            repeat(2) {
                acted += module.lookAt(Priority.NORMAL, { yawTarget(0f) }) { going -> near(going) }
            }
        }

        tick(Rotation(0f, 0f))
        assertEquals(1, acted.size)
        assertEquals(1, looks.size)

        tick(Rotation(0.5f, 0f))
        assertEquals(listOf(Rotation(0f, 0f), Rotation(0.5f, 0f)), acted)
    }

}
