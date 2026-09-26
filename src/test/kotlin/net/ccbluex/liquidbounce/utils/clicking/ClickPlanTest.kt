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
package net.ccbluex.liquidbounce.utils.clicking

import java.util.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClickPlanTest {

    private fun humanPlan(cps: IntRange, maxPerTick: Int = 2, seed: Long = 1337L) =
        ClickPlan(HumanClickTiming(), Random(seed)).apply {
            this.cps = cps
            this.maxPerTick = maxPerTick
        }

    /**
     * Ticks [ticks] times, 50 ms apart, using every press, and returns the presses consumed per tick.
     */
    private fun ClickPlan.run(ticks: Int, onTick: (Int) -> Unit = {}): IntArray {
        val consumed = IntArray(ticks)
        repeat(ticks) { tick ->
            tick(tick * ClickPlan.TICK_MS)
            consumed[tick] = clicksAt(0)
            onTick(tick)
            consume({ true }) { true }
        }
        return consumed
    }

    @Test
    fun `presses are batched into the tick that follows them`() {
        val plan = ClickPlan({ _, _, _, _ -> 30L }).apply { maxPerTick = 2 }

        // Presses at 0, 30, 60, 90, 120, 150, 180, 210, 240 against ticks at 0, 50, 100, ...
        val consumed = plan.run(6)

        assertEquals(listOf(1, 1, 2, 2, 1, 2), consumed.toList())
    }

    @Test
    fun `lookahead equals consumption`() {
        val plan = humanPlan(8..20)
        val ticks = 2000
        val ahead = 10
        val predicted = Array(ticks + ahead) { mutableListOf<Int>() }

        val consumed = plan.run(ticks) { tick ->
            for (n in 1..ahead) {
                predicted[tick + n] += plan.clicksAt(n)
            }
        }

        for (tick in 0 until ticks) {
            for (prediction in predicted[tick]) {
                assertEquals(consumed[tick], prediction, "tick $tick")
            }
        }
    }

    @Test
    fun `mean cps stays inside the range`() {
        for (range in listOf(8..10, 11..14, 16..20)) {
            val ticks = 20 * 60 * 10
            val cps = humanPlan(range).run(ticks).sum() / (ticks / 20.0)

            assertTrue(cps in range.first.toDouble()..range.last.toDouble(), "$cps outside $range")
        }
    }

    @Test
    fun `settings changed mid combo apply right away`() {
        val plan = humanPlan(11..14, maxPerTick = 1)
        fun cps(from: Int, until: Int): Double {
            var clicks = 0
            for (tick in from until until) {
                plan.tick(tick * ClickPlan.TICK_MS)
                clicks += plan.consume({ true }) { true }
            }
            return clicks / ((until - from) / 20.0)
        }

        cps(0, 200)
        plan.cps = 4..5
        val slow = cps(200, 400)
        assertTrue(slow <= 5.5, "$slow cps after lowering the range")

        plan.cps = 25..30
        cps(400, 420)
        val capped = cps(420, 620)
        assertTrue(capped <= 20.0, "$capped cps with one press per tick")

        plan.maxPerTick = 3
        cps(620, 640)
        val fast = cps(640, 840)
        assertTrue(fast >= 24.0, "$fast cps after raising the cap")
    }

    @Test
    fun `constant clicks at the top of the range, evenly`() {
        val plan = ClickPlan(ConstantClickTiming, Random(1L)).apply { cps = 10..20 }
        val consumed = plan.run(20 * 60)

        assertTrue(consumed.drop(1).all { it == 1 }, consumed.joinToString(""))
    }

    @Test
    fun `presses per tick never exceed the cap`() {
        for (cap in 1..3) {
            val plan = humanPlan(25..30, maxPerTick = cap)
            var now = 0L

            repeat(5000) { tick ->
                // A late tick collapses several windows into one
                now += if (tick % 97 == 0) 3 * ClickPlan.TICK_MS else ClickPlan.TICK_MS
                plan.tick(now)
                for (n in 0..20) {
                    assertTrue(plan.clicksAt(n) <= cap, "tick $tick + $n")
                }
                plan.consume({ true }) { true }
            }
        }
    }

    @Test
    fun `presses during miss time are dropped`() {
        val plan = humanPlan(11..14)
        var missTime = 0
        var executed = 0
        val ticks = 2000

        val consumed = plan.run(ticks) {
            // An air click sets missTime like Minecraft.startAttack on a MISS
            val clicks = plan.consume({ missTime <= 0 }) {
                missTime = 10
                true
            }
            assertTrue(clicks <= 1)
            executed += clicks

            if (missTime > 0) {
                missTime--
            }
        }

        assertTrue(executed <= ticks / 10 + 1, "$executed clicks through miss time")
        assertTrue(consumed.sum() / (ticks / 20.0) >= 11.0, "dropped presses must not be delayed")
    }

    @Test
    fun `unused presses end the combo`() {
        val plan = humanPlan(11..14)
        plan.run(100)
        assertTrue(plan.comboMs > 4000)

        var now = 100 * ClickPlan.TICK_MS
        repeat(10) {
            plan.tick(now)
            now += ClickPlan.TICK_MS
        }

        assertTrue(plan.comboMs < 250, "combo is ${plan.comboMs} ms")
    }

}
