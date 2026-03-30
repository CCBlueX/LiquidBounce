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

import it.unimi.dsi.fastutil.doubles.DoubleDoublePair
import net.ccbluex.fastutil.component1
import net.ccbluex.fastutil.component2
import net.ccbluex.fastutil.forEachDouble
import net.ccbluex.fastutil.step
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class PlaneSection(
    val originPoint: Vec3,
    val dirVec1: Vec3,
    val dirVec2: Vec3,
) {

    inline fun castPointsOnUniformly(maxPoints: Int, consumer: (Vec3) -> Unit) {
        val (stepDir2, stepDir1) = getFairStepSide(maxPoints)

        (0.0..1.0 step stepDir1).forEachDouble { y ->
            (0.0..1.0 step stepDir2).forEachDouble { z ->
                consumer(originPoint.fma(y, dirVec1).fma(z, dirVec2))
            }
        }
    }

    fun getFairStepSide(nPoints: Int): DoubleDoublePair {
        val vec1zero = dirVec1.isLikelyZero
        val vec2zero = dirVec2.isLikelyZero

        return when {
            !vec1zero && !vec2zero -> {
                val aspectRatio = dirVec2.length() / dirVec1.length()
                val stepDir2 = sqrt(1.0 / (aspectRatio * nPoints))
                val stepDir1 = sqrt(aspectRatio / nPoints)

                DoubleDoublePair.of(stepDir2, stepDir1)
            }
            vec1zero && vec2zero -> DoubleDoublePair.of(1.0, 1.0)
            vec1zero -> DoubleDoublePair.of(1.0, 2.0 / nPoints)
            else -> DoubleDoublePair.of(2.0 / nPoints, 1.0)
        }
    }

}
