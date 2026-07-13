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

import it.unimi.dsi.fastutil.longs.LongDoubleImmutablePair
import net.ccbluex.fastutil.asObjectList
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.longDoubleHashMapOf
import net.ccbluex.fastutil.mapToArray
import net.ccbluex.liquidbounce.utils.math.yaw
import net.minecraft.util.Mth.wrapDegrees
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.ChunkPos.getX
import net.minecraft.world.level.ChunkPos.getZ
import net.minecraft.world.phys.Vec3
import kotlin.math.exp

private const val CHUNK_CENTER_OFFSET = 8
private const val CHUNK_SIZE = 16

@JvmRecord
data class EyeMeasurement(
    val throwPos: Vec3,
    val angleDeg: Float,
    val tick: Int,
) {
    constructor(
        throwX: Double,
        throwY: Double,
        throwZ: Double,
        angleDeg: Float,
        tick: Int,
    ) : this(Vec3(throwX, throwY, throwZ), angleDeg, tick)
}

@JvmRecord
data class PosteriorCandidate(
    val chunkPos: ChunkPos,
    val probability: Double,
)

@JvmRecord
data class PosteriorSnapshot(
    val candidates: List<PosteriorCandidate>,
    val confidence: Double,
    val sampleCount: Int,
)

object StrongholdBayesianEstimator {

    @Suppress("CognitiveComplexMethod", "LongMethod")
    @JvmStatic
    fun estimate(
        measurements: List<EyeMeasurement>,
        hypotheses: List<StrongholdHypothesis>,
        sigmaDeg: Double,
        requireSameStrongholdAcrossThrows: Boolean,
        topCandidates: Int,
    ): PosteriorSnapshot? {
        if (measurements.isEmpty() || hypotheses.isEmpty() || sigmaDeg <= 0.0 || topCandidates <= 0) {
            return null
        }

        val sigmaSquared = sigmaDeg * sigmaDeg
        val scoredHypotheses = ArrayList<LongDoubleImmutablePair>(hypotheses.size)

        for (hypothesis in hypotheses) {
            var valid = true
            var logWeight = 0.0
            var firstNearestIndex = -1

            val nearestCounts = IntArray(hypothesis.chunks.size)
            for (measurement in measurements) {
                val nearestIndex = nearestStrongholdIndex(hypothesis, measurement.throwPos.x, measurement.throwPos.z)
                if (nearestIndex == -1) {
                    valid = false
                    break
                }

                if (firstNearestIndex == -1) {
                    firstNearestIndex = nearestIndex
                }

                if (requireSameStrongholdAcrossThrows && nearestIndex != firstNearestIndex) {
                    valid = false
                    break
                }

                nearestCounts[nearestIndex]++

                val targetX = chunkCenter(getX(hypothesis.chunks[nearestIndex]))
                val targetZ = chunkCenter(getZ(hypothesis.chunks[nearestIndex]))
                val predictedYaw = angleToYaw(measurement.throwPos.x, measurement.throwPos.z, targetX, targetZ)
                val delta = wrapDegrees(measurement.angleDeg - predictedYaw).toDouble()
                logWeight -= (delta * delta) / (2.0 * sigmaSquared)
            }

            if (!valid) {
                continue
            }

            val chosenIndex = if (requireSameStrongholdAcrossThrows) {
                firstNearestIndex
            } else {
                nearestCounts.indices.maxByOrNull { nearestCounts[it] } ?: -1
            }

            if (chosenIndex == -1) {
                continue
            }

            scoredHypotheses += LongDoubleImmutablePair(hypothesis.chunks[chosenIndex], logWeight)
        }

        if (scoredHypotheses.isEmpty()) {
            return null
        }

        val maxLogWeight = scoredHypotheses.maxOf { it.secondDouble() }
        var weightSum = 0.0
        val chunkWeights = longDoubleHashMapOf()

        for ((key, logWeight) in scoredHypotheses) {
            val linearWeight = exp(logWeight - maxLogWeight)
            chunkWeights.addTo(key, linearWeight)
            weightSum += linearWeight
        }

        if (weightSum <= 0.0) {
            return null
        }

        val candidates = chunkWeights.long2DoubleEntrySet()
            .mapToArray {
                PosteriorCandidate(
                    ChunkPos.unpack(it.longKey),
                    it.doubleValue / weightSum,
                )
            }.ifEmpty { return null }

        candidates.sortByDescending { it.probability }

        return PosteriorSnapshot(
            candidates = candidates.asObjectList(length = minOf(candidates.size, topCandidates)),
            confidence = candidates.first().probability,
            sampleCount = measurements.size,
        )
    }

    private fun nearestStrongholdIndex(hypothesis: StrongholdHypothesis, throwX: Double, throwZ: Double): Int {
        var nearestIndex = -1
        var nearestDistance = Double.POSITIVE_INFINITY

        for ((index, chunkPos) in hypothesis.chunks.withIndex()) {
            val targetX = chunkCenter(getX(chunkPos))
            val targetZ = chunkCenter(getZ(chunkPos))
            val dx = targetX - throwX
            val dz = targetZ - throwZ
            val distanceSquared = dx * dx + dz * dz

            if (distanceSquared < nearestDistance) {
                nearestDistance = distanceSquared
                nearestIndex = index
            }
        }

        return nearestIndex
    }

    private fun angleToYaw(fromX: Double, fromZ: Double, toX: Double, toZ: Double): Float {
        val dx = toX - fromX
        val dz = toZ - fromZ
        return Vec3(dx, 0.0, dz).yaw
    }

    private fun chunkCenter(coordinate: Int): Double = coordinate * CHUNK_SIZE.toDouble() + CHUNK_CENTER_OFFSET
}
