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
package net.ccbluex.liquidbounce.deeplearn.clicking

import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import kotlin.math.ln
import kotlin.math.min
import kotlin.random.Random

/**
 * The model input: the last [HISTORY] gaps between presses as log milliseconds, how much of that history exists
 * yet, and how long the burst has been going, which is where fatigue shows.
 */
@UnstableAddonApi
object ClickingFeatures {
    const val VERSION = 1
    const val HISTORY = 8
    const val SIZE = HISTORY + 2
    private const val UNKNOWN_INTERVAL = 100f

    /** [intervals] in ms, oldest first; only the last [HISTORY] are used. */
    fun write(intervals: List<Float>, burstMs: Float, into: FloatArray) {
        for (i in 0 until HISTORY) {
            val index = intervals.size - HISTORY + i
            into[i] = ln(if (index >= 0) intervals[index] else UNKNOWN_INTERVAL)
        }
        into[HISTORY] = min(intervals.size, HISTORY) / HISTORY.toFloat()
        into[HISTORY + 1] = burstMs / 1000f
    }
}

/** One burst of presses in the model's own tempo; [next] draws the gap to the following press in ms. */
@UnstableAddonApi
class ClickingRhythm(private val predict: (FloatArray) -> FloatArray?) {
    private val intervals = ArrayDeque<Float>()
    private val input = FloatArray(ClickingFeatures.SIZE)

    var burstMs = 0f
        private set

    fun next(random: Random): Float? {
        ClickingFeatures.write(intervals, burstMs, input)
        val interval = ClickingOutputs.sample(predict(input) ?: return null, random)
        intervals.addLast(interval)
        if (intervals.size > ClickingFeatures.HISTORY) {
            intervals.removeFirst()
        }
        burstMs += interval
        return interval
    }

    fun reset() {
        intervals.clear()
        burstMs = 0f
    }
}
