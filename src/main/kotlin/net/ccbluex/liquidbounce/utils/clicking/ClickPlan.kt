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

import it.unimi.dsi.fastutil.longs.LongArrayList
import java.util.Random
import kotlin.math.abs

/**
 * Press timestamps planned ahead in milliseconds and batched into ticks the way
 * [net.minecraft.client.Minecraft.handleKeybinds] drains every click queued since the previous tick.
 *
 * Tick `n` consumes the presses in `(tickTime + (n - 1) * 50, tickTime + n * 50]`,
 * so [clicksAt] is what that tick will consume.
 */
class ClickPlan(
    private val timing: ClickTiming,
    private val random: Random = Random(),
) {

    companion object {
        const val TICK_MS = 50L
        private const val HORIZON_MS = 1000L
        private const val IDLE_MS = 250L
        private const val HISTORY = 32
        private const val NONE = Long.MIN_VALUE
    }

    var cps = 11..14
    var maxPerTick = 2
    var breakCombo = 0..0
    var comboLengthMs = 10_000L

    /**
     * Adds a guaranteed click to tick `n` when nothing is planned for it.
     */
    var enforced: (Int) -> Boolean = { false }

    var tickTime = 0L
        private set

    private val times = LongArrayList()
    private val intervals = LongArrayList()
    private var next = 0
    private var comboStart = 0L
    private var due = 0
    private var consumed = 0
    private var idleSince = NONE

    val comboMs get() = tickTime - comboStart

    /**
     * Starts a new combo with a press at [now].
     */
    fun reset(now: Long) {
        times.clear()
        intervals.clear()
        next = 0
        due = 0
        consumed = 0
        idleSince = NONE
        comboStart = now
        tickTime = now - TICK_MS
        times.add(now)
    }

    /**
     * Advances to the next tick and consumes the presses due at [now].
     * Presses nobody used for a while, or a gap in ticks, end the combo.
     */
    fun tick(now: Long) {
        if (consumed > 0) {
            idleSince = NONE
        } else if (due > 0 && idleSince == NONE) {
            idleSince = tickTime
        }

        if (times.isEmpty || now - tickTime > IDLE_MS || idleSince != NONE && now - idleSince > IDLE_MS) {
            reset(now)
        }

        // Stay on the 50 ms grid so lookahead matches consumption, unless the real tick drifted off it
        val expected = tickTime + TICK_MS
        tickTime = if (abs(now - expected) > TICK_MS) now else expected

        planUntil(tickTime + HORIZON_MS)
        var count = 0
        while (next < times.size && times.getLong(next) <= tickTime) {
            next++
            count++
        }
        due = count.coerceAtMost(maxPerTick)
        consumed = 0
        trim()
    }

    fun clicksAt(tick: Int): Int {
        val planned = if (tick <= 0) due else plannedAt(tick)
        return if (planned == 0 && enforced(tick.coerceAtLeast(0))) 1 else planned
    }

    private fun plannedAt(tick: Int): Int {
        val from = tickTime + (tick - 1) * TICK_MS
        val to = from + TICK_MS
        planUntil(to)
        var count = 0
        for (i in next until times.size) {
            val time = times.getLong(i)
            if (time > to) {
                break
            }
            if (time > from) {
                count++
            }
        }
        return count.coerceAtMost(maxPerTick)
    }

    /**
     * Uses up the presses of the current tick. A press failing [gate] is dropped,
     * as vanilla drops presses while `missTime` is set. Returns the amount of successful [block] calls.
     */
    inline fun consume(gate: () -> Boolean, block: () -> Boolean): Int {
        var clicks = 0
        while (take()) {
            if (gate() && block()) {
                clicks++
            }
        }
        return clicks
    }

    @PublishedApi
    internal fun take(): Boolean {
        if (consumed >= clicksAt(0)) {
            return false
        }
        consumed++
        return true
    }

    private fun planUntil(time: Long) {
        while (times.getLong(times.size - 1) <= time) {
            planNext()
        }
    }

    private fun planNext() {
        val last = times.getLong(times.size - 1)
        var time = last + timing.nextInterval(intervals, last - comboStart, cps, random)

        val breaks = breakCombo.last > 0 && time - comboStart > comboLengthMs
        if (breaks) {
            time += breakCombo.randomIn(random) * TICK_MS
        }

        if (times.size >= maxPerTick) {
            time = maxOf(time, times.getLong(times.size - maxPerTick) + TICK_MS)
        }

        if (breaks) {
            comboStart = time
            intervals.clear()
        } else {
            intervals.add(time - last)
            if (intervals.size > HISTORY) {
                intervals.removeLong(0)
            }
        }
        times.add(time)
    }

    private fun trim() {
        val excess = next - HISTORY
        if (excess > 0) {
            times.removeElements(0, excess)
            next -= excess
        }
    }

    private fun IntRange.randomIn(random: Random) = first + random.nextInt(last - first + 1)

}
