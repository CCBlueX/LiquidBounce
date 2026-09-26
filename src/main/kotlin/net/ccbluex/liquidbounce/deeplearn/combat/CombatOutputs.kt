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
class CombatDecision(val yaw: Float, val pitch: Float, val attack: Boolean, val clamped: Boolean = false)

/**
 * Outputs per tick: turn mean and log scale for yaw and pitch, an attack logit, logits for the forward
 * and strafe keys, a jump logit and a sprint logit. Turns are in [CombatFeatures.TURN_SCALE] units.
 */
@UnstableAddonApi
object CombatOutputs {
    const val HIDDEN = 64
    const val SIZE = 13
    const val JUMP = 11
    const val SPRINT = 12
    private const val FORWARD = 2

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
        val probability = 1f / (1f + exp(-output[4]))
        return CombatDecision(
            yaw.coerceIn(-turnCap, turnCap), pitch.coerceIn(-turnCap, turnCap),
            random.nextFloat() < probability, clamped || capped,
        )
    }

    fun movement(output: FloatArray): Pair<Int, Int> =
        (5..7).maxBy { output[it] } - 5 to (8..10).maxBy { output[it] } - 8

    /**
     * Keys drawn from their probabilities. The likeliest key is usually the one already held, so always
     * taking it never starts a strafe the way players do.
     */
    fun movement(output: FloatArray, random: Random) = key(output, 5, random) to key(output, 8, random)

    /** Per tick chance of jumping, sampled like attacks. */
    fun jump(output: FloatArray, random: Random) = random.nextFloat() < 1f / (1f + exp(-output[JUMP]))

    fun sprint(output: FloatArray) = output[SPRINT] > 0f

    /**
     * Sprinting was recorded as a state, which implies the forward key, so it is drawn given the drawn
     * forward key. Drawing both independently sprints only as often as both come up together.
     */
    fun sprint(output: FloatArray, forward: Int, random: Random): Boolean {
        if (forward != FORWARD) {
            return false
        }
        val weights = weights(output, 5)
        return random.nextFloat() * weights[FORWARD] / weights.sum() < 1f / (1f + exp(-output[SPRINT]))
    }

    private fun weights(output: FloatArray, from: Int): FloatArray {
        val largest = maxOf(output[from], output[from + 1], output[from + 2])
        return FloatArray(3) { exp(output[from + it] - largest) }
    }

    private fun key(output: FloatArray, from: Int, random: Random): Int {
        val weights = weights(output, from)
        var pick = random.nextFloat() * weights.sum()
        for (key in 0..1) {
            pick -= weights[key]
            if (pick < 0f) {
                return key
            }
        }
        return 2
    }
}
