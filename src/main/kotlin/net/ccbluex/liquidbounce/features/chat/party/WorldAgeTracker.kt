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


package net.ccbluex.liquidbounce.features.chat.party

import kotlin.math.abs

private const val MILLIS_PER_TICK = 50L
private const val MIN_TPS = 5.0
private const val SLOW_SAMPLES_UNTIL_FROZEN = 2

/**
 * The chat server compares world ages to tell whether two players share a world, so an age is only
 * offered while it advances.
 */
class WorldAgeTracker {

    private var lastAge = -1L
    private var lastAt = 0L
    private var slowSamples = 0

    var isAdvancing = false
        private set

    fun reset() {
        lastAge = -1L
        slowSamples = 0
        isAdvancing = false
    }

    fun update(age: Long, now: Long) {
        if (lastAge >= 0) {
            val elapsed = now - lastAt
            val tps = if (elapsed > 0) (age - lastAge) * 1000.0 / elapsed else 0.0

            if (age > lastAge && tps >= MIN_TPS) {
                slowSamples = 0
                isAdvancing = true
            } else if (++slowSamples >= SLOW_SAMPLES_UNTIL_FROZEN) {
                isAdvancing = false
            }
        }

        lastAge = age
        lastAt = now
    }

    /**
     * Assumes 20 ticks per second since the last update; `null` while the age does not advance.
     */
    fun current(now: Long): Long? = if (isAdvancing) lastAge + (now - lastAt) / MILLIS_PER_TICK else null

    /**
     * Ticks between the actual age and what the server extrapolates from an age reported at [reportedAt].
     */
    fun drift(reportedAge: Long, reportedAt: Long, now: Long): Long? {
        val actual = current(now) ?: return null
        return abs(actual - (reportedAge + (now - reportedAt) / MILLIS_PER_TICK))
    }

}
