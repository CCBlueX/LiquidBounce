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

import net.minecraft.util.Mth
import net.minecraft.util.Mth.wrapDegrees
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.Random
import kotlin.math.atan2

class StrongholdBayesianEstimatorTest {

    @Test
    fun `angle wrap behaves around -180 and 180 degrees`() {
        assertEquals(-2.0f, wrapDegrees(179f - (-179f)), 1e-6f)
        assertEquals(2.0f, wrapDegrees(-179f - 179f), 1e-6f)
        assertEquals(0.0f, wrapDegrees(360f), 1e-6f)
    }

    @Test
    fun `synthetic known hypothesis recovery finds true chunk`() {
        val trueHypothesis = StrongholdHypothesisGenerator.generate(1, seed = 11L).first()
        val targetIndex = 7

        val hypotheses = buildList {
            add(trueHypothesis)
            addAll(StrongholdHypothesisGenerator.generate(2000, seed = 99L))
        }

        val measurements = syntheticMeasurements(
            hypothesis = trueHypothesis,
            targetIndex = targetIndex,
            amount = 6,
            noiseSigmaDeg = 0.02,
            seed = 5L
        )

        val posterior = StrongholdBayesianEstimator.estimate(
            measurements = measurements,
            hypotheses = hypotheses,
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 5
        )

        assertNotNull(posterior)
        val best = posterior!!.candidates.first()
        assertEquals(trueHypothesis.chunkX[targetIndex], best.chunkX)
        assertEquals(trueHypothesis.chunkZ[targetIndex], best.chunkZ)
    }

    @Test
    fun `confidence increases with more consistent samples`() {
        val trueHypothesis = StrongholdHypothesisGenerator.generate(1, seed = 17L).first()
        val hypotheses = buildList {
            add(trueHypothesis)
            addAll(StrongholdHypothesisGenerator.generate(1000, seed = 123L))
        }

        val oneMeasurement = syntheticMeasurements(trueHypothesis, 2, amount = 1, noiseSigmaDeg = 0.0, seed = 1L)
        val manyMeasurements = List(6) { tick -> oneMeasurement.first().copy(tick = tick) }

        val posteriorOne = StrongholdBayesianEstimator.estimate(
            oneMeasurement,
            hypotheses,
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 3
        )
        val posteriorMany = StrongholdBayesianEstimator.estimate(
            manyMeasurements,
            hypotheses,
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 3
        )

        assertNotNull(posteriorOne)
        assertNotNull(posteriorMany)
        assertTrue(posteriorMany!!.confidence >= posteriorOne!!.confidence)
    }

    @Test
    fun `higher sigma yields flatter posterior`() {
        val trueHypothesis = StrongholdHypothesisGenerator.generate(1, seed = 21L).first()
        val hypotheses = buildList {
            add(trueHypothesis)
            addAll(StrongholdHypothesisGenerator.generate(1000, seed = 444L))
        }

        val measurements = syntheticMeasurements(trueHypothesis, 4, amount = 5, noiseSigmaDeg = 0.015, seed = 9L)

        val lowSigmaPosterior = StrongholdBayesianEstimator.estimate(
            measurements,
            hypotheses,
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 3
        )
        val highSigmaPosterior = StrongholdBayesianEstimator.estimate(
            measurements,
            hypotheses,
            sigmaDeg = 0.2,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 3
        )

        assertNotNull(lowSigmaPosterior)
        assertNotNull(highSigmaPosterior)
        assertTrue(lowSigmaPosterior!!.confidence > highSigmaPosterior!!.confidence)
    }

    @Test
    fun `nearest stronghold consistency gate can reject inconsistent throws`() {
        val hypothesis = StrongholdHypothesis(
            chunkX = IntArray(128) { 100000 },
            chunkZ = IntArray(128) { 100000 }
        ).also {
            it.chunkX[0] = 0
            it.chunkZ[0] = 0
            it.chunkX[1] = 200
            it.chunkZ[1] = 0
        }

        val measurementA = EyeMeasurement(
            throwX = 8.0,
            throwY = 64.0,
            throwZ = 8.0,
            angleDeg = yawToChunkCenter(8.0, 8.0, hypothesis.chunkX[0], hypothesis.chunkZ[0]),
            tick = 1
        )
        val measurementB = EyeMeasurement(
            throwX = 200 * 16.0 + 8.0,
            throwY = 64.0,
            throwZ = 8.0,
            angleDeg = yawToChunkCenter(200 * 16.0 + 8.0, 8.0, hypothesis.chunkX[1], hypothesis.chunkZ[1]),
            tick = 2
        )

        val rejected = StrongholdBayesianEstimator.estimate(
            measurements = listOf(measurementA, measurementB),
            hypotheses = listOf(hypothesis),
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = true,
            topCandidates = 3
        )
        val allowed = StrongholdBayesianEstimator.estimate(
            measurements = listOf(measurementA, measurementB),
            hypotheses = listOf(hypothesis),
            sigmaDeg = 0.03,
            requireSameStrongholdAcrossThrows = false,
            topCandidates = 3
        )

        assertNull(rejected)
        assertNotNull(allowed)
    }

    private fun syntheticMeasurements(
        hypothesis: StrongholdHypothesis,
        targetIndex: Int,
        amount: Int,
        noiseSigmaDeg: Double,
        seed: Long,
    ): List<EyeMeasurement> {
        val random = Random(seed)
        val targetX = hypothesis.chunkX[targetIndex] * 16.0 + 8.0
        val targetZ = hypothesis.chunkZ[targetIndex] * 16.0 + 8.0

        return buildList {
            repeat(amount) { index ->
                val throwX = targetX - 1500.0 + index * 20.0
                val throwZ = targetZ - 1200.0 + index * 13.0
                val trueYaw = yawTo(throwX, throwZ, targetX, targetZ)
                val noisyYaw = Mth.wrapDegrees((trueYaw + random.nextGaussian() * noiseSigmaDeg).toFloat())

                add(
                    EyeMeasurement(
                        throwX = throwX,
                        throwY = 70.0,
                        throwZ = throwZ,
                        angleDeg = noisyYaw,
                        tick = index
                    )
                )
            }
        }
    }

    private fun yawToChunkCenter(fromX: Double, fromZ: Double, chunkX: Int, chunkZ: Int): Float {
        val targetX = chunkX * 16.0 + 8.0
        val targetZ = chunkZ * 16.0 + 8.0
        return yawTo(fromX, fromZ, targetX, targetZ)
    }

    private fun yawTo(fromX: Double, fromZ: Double, toX: Double, toZ: Double): Float {
        return Mth.wrapDegrees(Math.toDegrees(atan2(toZ - fromZ, toX - fromX)).toFloat() - 90f)
    }
}
