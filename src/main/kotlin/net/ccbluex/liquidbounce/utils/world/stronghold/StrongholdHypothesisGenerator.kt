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
package net.ccbluex.liquidbounce.utils.world.stronghold

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.minecraft.util.Mth.TWO_PI
import java.util.SplittableRandom
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val STRONGHOLD_COUNT = 128
private const val DISTANCE = 32
private const val SPREAD = 3
private const val SNAPPING_OFFSET = 7
private const val HYPOTHESIS_SEED = 0x51F15EEDL

data class StrongholdHypothesis(
    val chunkX: IntArray,
    val chunkZ: IntArray,
) {
    init {
        require(chunkX.size == STRONGHOLD_COUNT && chunkZ.size == STRONGHOLD_COUNT) {
            "Stronghold hypothesis needs exactly $STRONGHOLD_COUNT entries"
        }
    }
}

object StrongholdHypothesisGenerator {

    @JvmStatic
    fun generate(hypothesisCount: Int, seed: Long = HYPOTHESIS_SEED): List<StrongholdHypothesis> {
        require(hypothesisCount >= 1) { "Hypothesis count must be >= 1" }
        return List(hypothesisCount) { index -> generateSingle(seed + index) }
    }

    @JvmStatic
    fun ringDistribution(
        spread: Int = SPREAD,
        count: Int = STRONGHOLD_COUNT,
    ): IntArray {
        require(spread >= 1) { "Spread must be >= 1" }
        require(count >= 1) { "Count must be >= 1" }

        var currentSpread = spread
        var ring = 0
        var inCurrentRing = 0
        val ringSizes = IntArrayList()

        for (index in 0 until count) {
            inCurrentRing++
            if (inCurrentRing == currentSpread) {
                ringSizes.add(inCurrentRing)
                ring++
                inCurrentRing = 0
                currentSpread += 2 * currentSpread / (ring + 1)
                currentSpread = min(currentSpread, count - index)
            }
        }

        if (inCurrentRing > 0) {
            ringSizes.add(inCurrentRing)
        }

        return ringSizes.toIntArray()
    }

    private fun generateSingle(seed: Long): StrongholdHypothesis {
        val random = SplittableRandom(seed)
        val xs = IntArray(STRONGHOLD_COUNT)
        val zs = IntArray(STRONGHOLD_COUNT)

        var angle = random.nextDouble() * TWO_PI
        var ring = 0
        var currentSpread = SPREAD
        var inCurrentRing = 0

        for (index in 0 until STRONGHOLD_COUNT) {
            val ringRadius = 4 * DISTANCE + DISTANCE * ring * 6 + (random.nextDouble() - 0.5) * (DISTANCE * 2.5)
            var chunkX = (cos(angle) * ringRadius).roundToInt()
            var chunkZ = (sin(angle) * ringRadius).roundToInt()

            chunkX += random.nextInt(-SNAPPING_OFFSET, SNAPPING_OFFSET + 1)
            chunkZ += random.nextInt(-SNAPPING_OFFSET, SNAPPING_OFFSET + 1)

            xs[index] = chunkX
            zs[index] = chunkZ

            angle += TWO_PI / currentSpread
            inCurrentRing++

            if (inCurrentRing == currentSpread) {
                ring++
                inCurrentRing = 0
                currentSpread += 2 * currentSpread / (ring + 1)
                currentSpread = min(currentSpread, STRONGHOLD_COUNT - index)
                angle += random.nextDouble() * TWO_PI
            }
        }

        return StrongholdHypothesis(xs, zs)
    }
}
