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

import it.unimi.dsi.fastutil.longs.LongList
import java.util.Random
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.roundToLong

/**
 * Log-normal intervals around a CPS drawn once per combo, shifted by [fatigue] over the combo time in seconds.
 */
class HumanClickTiming(private val fatigue: (Float) -> Float) : ClickTiming {

    companion object {
        private const val SIGMA = 0.45
        private const val MIN_INTERVAL_MS = 10L
        private const val MAX_INTERVAL_MS = 1000L
    }

    private var comboCps = 0.0

    override fun nextInterval(recent: LongList, comboMs: Long, cps: IntRange, random: Random): Long {
        if (comboMs == 0L || comboCps == 0.0) {
            comboCps = cps.first + random.nextDouble() * (cps.last - cps.first)
        }

        val targetCps = (comboCps + fatigue(comboMs / 1000f)).coerceAtLeast(1.0)
        // mu shifted by -sigma^2/2 so the mean interval, not the median, lands on the target CPS
        val mu = ln(1000.0 / targetCps) - SIGMA * SIGMA / 2
        return exp(mu + SIGMA * random.nextGaussian()).roundToLong().coerceIn(MIN_INTERVAL_MS, MAX_INTERVAL_MS)
    }

}
