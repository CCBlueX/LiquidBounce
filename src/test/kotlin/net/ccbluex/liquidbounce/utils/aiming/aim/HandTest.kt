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
package net.ccbluex.liquidbounce.utils.aiming.aim

import net.ccbluex.liquidbounce.utils.aiming.data.RotationDelta
import java.util.Random
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HandTest {

    private val step = 0.15f

    /**
     * Runs a hand from [start] against a goal that moves as [goal] says. A box [width] wide counts as reached when the
     * view is inside it; width 0 is a bare rotation, reached within half a step on each axis.
     */
    private fun run(
        seed: Long,
        ticks: Int,
        width: Float,
        start: Pair<Float, Float> = 0f to 0f,
        goal: (Int) -> Pair<Float, Float>,
    ): List<Pair<RotationDelta, Boolean>> {
        val hand = Hand(Random(seed)).apply { reset() }
        var (yaw, pitch) = start
        val margin = if (width > 0f) width / 2 else step / 2
        return List(ticks) { tick ->
            val (goalYaw, goalPitch) = goal(tick)
            val reached = abs(goalYaw - yaw) < margin && abs(goalPitch - pitch) < margin
            val delta = hand.step(yaw, pitch, goalYaw, goalPitch, width, width, reached, step)
            yaw += delta.deltaYaw
            pitch += delta.deltaPitch
            delta to (abs(goalYaw - yaw) < margin && abs(goalPitch - pitch) < margin)
        }
    }

    @Test
    fun `the same seed turns the same way`() {
        val strafing = { tick: Int -> 30f + tick * 2f to 5f * sin(tick / 9f) }
        assertEquals(run(7, 300, 8f, goal = strafing), run(7, 300, 8f, goal = strafing))
    }

    @Test
    fun `every turn is whole mouse steps`() {
        for ((delta, _) in run(3, 300, 8f) { tick -> 40f + tick * 3.3f to 12f * sin(tick / 7f) }) {
            for (degrees in floatArrayOf(delta.deltaYaw, delta.deltaPitch)) {
                val counts = degrees / step
                assertTrue(abs(counts - counts.roundToInt()) < 1e-3f, "$degrees is not a whole number of steps")
            }
        }
    }

    @Test
    fun `a flick lands on the target within its time and then holds still`() {
        repeat(20) { seed ->
            val hand = Hand(Random(0))
            val limit = hand.reaction.last + hand.ticksFor(90.55f, 8f)
            val stream = run(seed.toLong(), 60, 8f) { 90f to 10f }
            val landed = stream.indexOfFirst { (_, onTarget) -> onTarget }
            // A flick that falls short lands with its correction, a tick or two after Fitts' time
            assertTrue(landed in 0..limit + 2, "seed $seed landed after $landed ticks, allowed $limit")
            assertTrue(
                stream.drop(landed + 12).all { (delta, _) -> delta.deltaYaw == 0f && delta.deltaPitch == 0f },
                "seed $seed kept moving on a target that stands still",
            )
        }
    }

    @Test
    fun `a bare rotation is reached to half a step and held`() {
        repeat(20) { seed ->
            val stream = run(seed.toLong(), 80, 0f) { -135f to -40f }
            assertTrue(stream.takeLast(15).all { (_, onTarget) -> onTarget }, "seed $seed did not arrive")
            assertTrue(
                stream.takeLast(15).all { (delta, _) -> delta.deltaYaw == 0f && delta.deltaPitch == 0f },
                "seed $seed dithered on the goal",
            )
        }
    }

    @Test
    fun `a bare rotation less than a step away is still reached`() {
        repeat(20) { seed ->
            // Between half a step and a step off on both axes: one count each is needed
            val stream = run(seed.toLong(), 30, 0f, start = 0.11f to -0.12f) { 0f to 0f }
            assertTrue(stream.takeLast(10).all { (_, onTarget) -> onTarget }, "seed $seed stopped short of the goal")
        }
    }

    @Test
    fun `a strafing target is kept in sight`() {
        repeat(10) { seed ->
            val stream = run(seed.toLong(), 400, 9f) { tick ->
                // Turns around every 20 ticks, as a strafing player does
                val phase = tick % 40
                (if (phase < 20) phase * 2.5f else (40 - phase) * 2.5f) to 4f * sin(tick / 11f)
            }.drop(40)
            val inSight = stream.count { (_, onTarget) -> onTarget }.toFloat() / stream.size
            assertTrue(inSight > 0.5f, "seed $seed kept the target in sight only ${inSight * 100}% of the time")
        }
    }

    @Test
    fun `a moving target is followed in bursts, not at a steady speed`() {
        repeat(10) { seed ->
            val stream = run(seed.toLong(), 400, 9f) { tick -> tick * 3.5f to 0f }.drop(40)
            // An anticheat calls ten ticks of turns within 2 degrees of each other above 2.5 degrees robotic
            stream.map { (delta, _) -> abs(delta.deltaYaw) }.windowed(10).forEach { window ->
                val steady = window.count { yaw -> yaw > 2.5f && abs(yaw - window.first()) < 2f }
                assertTrue(steady <= 8, "seed $seed turned steadily: $window")
            }
            // A person tracking stops now and then between corrections
            val slow = stream.count { (delta, _) -> abs(delta.deltaYaw) < 1f }.toFloat() / stream.size
            assertTrue(slow > 0.1f, "seed $seed never slowed down: ${slow * 100}% slow ticks")
        }
    }

}
