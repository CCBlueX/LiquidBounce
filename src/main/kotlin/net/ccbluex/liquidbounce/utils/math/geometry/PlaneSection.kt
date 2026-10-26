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

package net.ccbluex.liquidbounce.utils.math.geometry

import net.ccbluex.liquidbounce.utils.kotlin.IntIntValuePair
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.world.phys.Vec3
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

class PlaneSection(
    val originPoint: Vec3,
    val dirVec1: Vec3,
    val dirVec2: Vec3,
) {

    /**
     * Casts at most [maxPoints] points over this section, spread so that the area covered by each point is
     * equal regardless of how the section is proportioned.
     *
     * Points sit at cell centres, so they never land on the section border where a ray through them would
     * graze the edge.
     *
     * @see getFairPointCounts
     */
    inline fun castPointsOnUniformly(maxPoints: Int, consumer: (Vec3) -> Unit) {
        val (countDir1, countDir2) = getFairPointCounts(maxPoints)

        forEachGridCenter(countDir1, countDir2) { t1, t2 ->
            consumer(originPoint.fma(t1, dirVec1).fma(t2, dirVec2))
            true
        }
    }

    /**
     * Point counts per direction of a grid that holds no more than [nPoints] points while keeping the area
     * covered by each point equal, i.e. the counts follow the side lengths of the section.
     *
     * The budget wins when the two conflict: a section whose side ratio is more extreme than [nPoints] : 1 is
     * capped at [nPoints] points instead of following the ratio.
     *
     * @return `(countDir1, countDir2)`
     */
    fun getFairPointCounts(nPoints: Int): IntIntValuePair {
        require(nPoints > 0) { "nPoints must be positive, was $nPoints" }

        val vec1zero = dirVec1.isLikelyZero
        val vec2zero = dirVec2.isLikelyZero

        return when {
            !vec1zero && !vec2zero -> {
                // countDir1 / countDir2 must equal len1 / len2 for equal per-point area, and the product must
                // not exceed nPoints. Capping the first count at nPoints keeps the second one, which is derived
                // from it, at 1 or above, so the product stays inside the budget even for extreme side ratios.
                val aspectRatio = dirVec2.length() / dirVec1.length()
                val countDir1 = min(nPoints, max(1, floor(sqrt(nPoints / aspectRatio)).toInt()))
                val countDir2 = max(1, nPoints / countDir1)

                IntIntValuePair(countDir1, countDir2)
            }
            // A zero-length direction covers no area: all points go onto the remaining one.
            vec1zero && vec2zero -> IntIntValuePair(1, 1)
            vec1zero -> IntIntValuePair(1, nPoints)
            else -> IntIntValuePair(nPoints, 1)
        }
    }

}
