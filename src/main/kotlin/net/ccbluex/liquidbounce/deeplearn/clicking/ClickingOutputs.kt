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

import net.ccbluex.liquidbounce.deeplearn.model.NetworkSpec
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import kotlin.math.exp
import kotlin.math.ln
import kotlin.random.Random

/**
 * One logit per bin of the gap to the next press, bins evenly spaced in log ms. Longer gaps are pauses, which
 * belong to whoever decides to stop clicking.
 */
@UnstableAddonApi
object ClickingOutputs {
    const val BINS = 48
    const val MIN_INTERVAL = 20f
    const val MAX_INTERVAL = 400f

    val NETWORK = NetworkSpec(listOf(32, 32), BINS)

    private val logMin = ln(MIN_INTERVAL)
    private val width = (ln(MAX_INTERVAL) - logMin) / BINS

    fun bin(interval: Float) =
        ((ln(interval.coerceIn(MIN_INTERVAL, MAX_INTERVAL)) - logMin) / width).toInt().coerceIn(0, BINS - 1)

    /** A gap drawn from the bins' probabilities, spread evenly in log ms inside the drawn bin. */
    fun sample(output: FloatArray, random: Random): Float {
        val largest = output.max()
        val weights = FloatArray(BINS) { exp(output[it] - largest) }
        var pick = random.nextFloat() * weights.sum()
        var bin = BINS - 1
        for (index in 0 until BINS) {
            pick -= weights[index]
            if (pick < 0f) {
                bin = index
                break
            }
        }
        return exp(logMin + (bin + random.nextFloat()) * width)
    }
}
