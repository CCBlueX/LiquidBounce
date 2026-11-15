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
package net.ccbluex.liquidbounce.deeplearn.combat

import net.ccbluex.liquidbounce.deeplearn.model.NetworkSpec
import net.ccbluex.liquidbounce.features.addon.UnstableAddonApi
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.sqrt
import kotlin.random.Random

@UnstableAddonApi
class CombatDecision(val yaw: Float, val pitch: Float, val clamped: Boolean = false)

/**
 * Outputs per tick: turn mean and log scale for yaw and pitch, in [CombatFeatures.TURN_SCALE] units.
 */
@UnstableAddonApi
object CombatOutputs {
    const val HIDDEN = 64
    const val SIZE = 4

    val NETWORK = NetworkSpec(listOf(HIDDEN, HIDDEN), SIZE)

    fun decide(
        output: FloatArray, clamped: Boolean, randomness: Float, turnCap: Float, random: Random,
    ): CombatDecision {
        fun turn(mean: Int, logScale: Int): Float {
            val gaussian = sqrt(-2.0 * ln(random.nextDouble().coerceAtLeast(1e-12))) *
                cos(2.0 * Math.PI * random.nextDouble())
            val noise = gaussian * exp(output[logScale].coerceIn(-6f, 1f).toDouble()) * randomness
            return ((output[mean] + noise) * CombatFeatures.TURN_SCALE).toFloat()
        }
        val yaw = turn(0, 2)
        val pitch = turn(1, 3)
        val capped = max(abs(yaw), abs(pitch)) > turnCap
        return CombatDecision(yaw.coerceIn(-turnCap, turnCap), pitch.coerceIn(-turnCap, turnCap), clamped || capped)
    }
}
