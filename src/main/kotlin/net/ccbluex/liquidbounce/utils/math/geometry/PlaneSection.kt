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
import net.ccbluex.liquidbounce.utils.math.isLikelyZero
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

class PlaneSection(
    val originPoint: Vec3,
    val dirVec1: Vec3,
    val dirVec2: Vec3,
) {

    inline fun castPointsOnUniformly(maxPoints: Int, consumer: (Vec3) -> Unit) {
        val (dz, dy) = getFairStepSide(maxPoints)

        (0.0..1.0 step dy).forEachDouble { y ->
            (0.0..1.0 step dz).forEachDouble { z ->
                val point = this.originPoint + this.dirVec1 * y + this.dirVec2 * z

                consumer(point)
            }
        }
    }

    fun getFairStepSide(nPoints: Int): DoubleDoublePair {
        val aspectRatio = this.dirVec2.length() / this.dirVec1.length()

        val vec1zero = this.dirVec1.isLikelyZero
        val vec2zero = this.dirVec2.isLikelyZero

        return when {
            !vec1zero && !vec2zero -> {
                val dz = sqrt(1 / (aspectRatio * nPoints))
                val dy = sqrt(aspectRatio / nPoints)

                DoubleDoublePair.of(dz, dy)
            }
            vec1zero && vec2zero -> DoubleDoublePair.of(1.0, 1.0)
            vec1zero -> DoubleDoublePair.of(1.0, 2.0 / nPoints)
            else -> DoubleDoublePair.of(2.0 / nPoints, 1.0)
        }
    }

}
