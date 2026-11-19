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
package net.ccbluex.liquidbounce.features.module.modules.combat.crystalaura

import kotlin.test.Test
import kotlin.test.assertEquals

class AdaptiveBreakTimingTest {

    private fun advance(
        state: AdaptiveBreakTimingState = AdaptiveBreakTimingState(),
        latencyMs: Long,
        fixedTicks: Int = 1,
        minTicks: Int = 0,
        maxTicks: Int = 4,
    ) = advanceAdaptiveBreakTiming(state, latencyMs, fixedTicks, minTicks, maxTicks)

    @Test
    fun `initial state uses fixed timing inside the configured bounds`() {
        val state = advance(latencyMs = 100L, fixedTicks = 2)

        assertEquals(100.0, state.smoothedLatencyMs)
        assertEquals(2, state.ticks)
    }

    @Test
    fun `each confirmation changes timing by at most one tick`() {
        var state = AdaptiveBreakTimingState()
        val ticks = mutableListOf<Int?>()

        repeat(5) {
            state = advance(state, latencyMs = 200L, fixedTicks = 0)
            ticks += state.ticks
        }

        assertEquals(listOf<Int?>(1, 2, 3, 4, 4), ticks)
        assertEquals(200.0, state.smoothedLatencyMs)
    }

    @Test
    fun `upward adjustment requires crossing the dead band`() {
        val below = advance(
            AdaptiveBreakTimingState(smoothedLatencyMs = 89.0, ticks = 1),
            latencyMs = 89L,
        )
        val boundary = advance(
            AdaptiveBreakTimingState(smoothedLatencyMs = 90.0, ticks = 1),
            latencyMs = 90L,
        )

        assertEquals(1, below.ticks)
        assertEquals(2, boundary.ticks)
    }

    @Test
    fun `downward adjustment requires crossing the dead band`() {
        val above = advance(
            AdaptiveBreakTimingState(smoothedLatencyMs = 61.0, ticks = 2),
            latencyMs = 61L,
        )
        val boundary = advance(
            AdaptiveBreakTimingState(smoothedLatencyMs = 60.0, ticks = 2),
            latencyMs = 60L,
        )

        assertEquals(2, above.ticks)
        assertEquals(1, boundary.ticks)
    }

    @Test
    fun `latency smoothing delays a downward timing change`() {
        val initial = AdaptiveBreakTimingState(smoothedLatencyMs = 100.0, ticks = 2)

        val first = advance(initial, latencyMs = 0L, fixedTicks = 2)
        val second = advance(first, latencyMs = 0L, fixedTicks = 2)

        assertEquals(75.0, first.smoothedLatencyMs)
        assertEquals(2, first.ticks)
        assertEquals(56.25, second.smoothedLatencyMs)
        assertEquals(1, second.ticks)
    }

    @Test
    fun `latency samples are clamped before smoothing`() {
        val negative = advance(latencyMs = -100L)
        val excessive = advance(latencyMs = Long.MAX_VALUE, fixedTicks = 0, maxTicks = 20)

        assertEquals(0.0, negative.smoothedLatencyMs)
        assertEquals(0, negative.ticks)
        assertEquals(5000.0, excessive.smoothedLatencyMs)
        assertEquals(1, excessive.ticks)
    }

    @Test
    fun `resolved timing clamps reversed bounds and out-of-range fixed values`() {
        val timing = AdaptiveBreakTiming()

        assertEquals(4, timing.resolve(fixedTicks = 99, minTicks = 4, maxTicks = 1))
        assertEquals(1, timing.resolve(fixedTicks = -5, minTicks = 4, maxTicks = 1))
    }

    @Test
    fun `recorded timing remains inside reversed bounds`() {
        val state = advance(
            latencyMs = 200L,
            fixedTicks = 0,
            minTicks = 4,
            maxTicks = 1,
        )

        assertEquals(2, state.ticks)
    }

    @Test
    fun `lower and upper bounds survive timing transitions`() {
        val lower = advance(latencyMs = 0L, fixedTicks = 0, minTicks = 2, maxTicks = 4)
        val upper = advance(latencyMs = 5000L, fixedTicks = 9, minTicks = 0, maxTicks = 3)

        assertEquals(2, lower.ticks)
        assertEquals(3, upper.ticks)
    }

    @Test
    fun `reset discards samples and restores fixed timing`() {
        val timing = AdaptiveBreakTiming()
        timing.record(latencyMs = 200L, fixedTicks = 0, minTicks = 0, maxTicks = 4)
        assertEquals(1, timing.resolve(fixedTicks = 0, minTicks = 0, maxTicks = 4))

        timing.reset()

        assertEquals(3, timing.resolve(fixedTicks = 3, minTicks = 0, maxTicks = 4))
        timing.record(latencyMs = 150L, fixedTicks = 3, minTicks = 0, maxTicks = 4)
        assertEquals(3, timing.resolve(fixedTicks = 3, minTicks = 0, maxTicks = 4))
    }
}
