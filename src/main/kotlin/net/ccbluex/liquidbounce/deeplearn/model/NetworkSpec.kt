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
package net.ccbluex.liquidbounce.deeplearn.model

import ai.djl.nn.Activation
import ai.djl.nn.SequentialBlock
import ai.djl.nn.core.Linear
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi

/** A fully connected network: [hidden] layers of the given widths, each followed by [activation]. */
@UnstableAddonApi
class NetworkSpec(val hidden: List<Int>, val outputs: Int, val activation: String = TANH) {
    init {
        require(activation == TANH) { "Unsupported activation $activation" }
        require(outputs > 0 && hidden.all { it > 0 })
    }

    fun block(): SequentialBlock = SequentialBlock().apply {
        for (units in hidden) {
            add(Linear.builder().setUnits(units.toLong()).build())
            add(Activation.tanhBlock())
        }
        add(Linear.builder().setUnits(outputs.toLong()).build())
    }

    companion object {
        const val TANH = "tanh"
    }
}

/** Standardizes inputs with the training set's statistics, clamped to [limit] standard deviations. */
@UnstableAddonApi
class InputNormalization(val mean: FloatArray, val scale: FloatArray, val limit: Float = DEFAULT_LIMIT) {
    init {
        require(mean.size == scale.size)
    }

    /** Returns whether any input had to be clamped. */
    fun apply(input: FloatArray, out: FloatArray): Boolean {
        var clamped = false
        for (index in input.indices) {
            val value = (input[index] - mean[index]) / scale[index]
            out[index] = value.coerceIn(-limit, limit)
            if (value < -limit || value > limit) {
                clamped = true
            }
        }
        return clamped
    }

    companion object {
        const val DEFAULT_LIMIT = 8f
    }
}
