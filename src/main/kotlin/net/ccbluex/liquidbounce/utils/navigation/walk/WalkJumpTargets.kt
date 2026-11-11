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

package net.ccbluex.liquidbounce.utils.navigation.walk

import net.ccbluex.liquidbounce.utils.math.ifNaN
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.terrain.hasRoom
import net.ccbluex.liquidbounce.utils.navigation.terrain.isOpen
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/** Spots worth a jump from a feet block: within reach across a gap or dip, where walking does not get as fast. */
internal class WalkJumpTargets(
    val terrain: NavigationTerrain,
    traits: MotionTraits = MotionTraits.DEFAULT,
) {

    private val extraHeight = traits.jumpHeight - MotionTraits.DEFAULT.jumpHeight
    private val maxRise = MAX_RISE + extraHeight
    private val maxUp = ceil(maxRise).toInt()
    private val reachScale = maxOf(1.0, traits.walkSpeed.toDouble() / MotionTraits.DEFAULT.walkSpeed)

    fun interface Sink {
        /** Takes the feet block [x] [y] [z] standing at [top]; false to stop. */
        fun accept(x: Int, y: Int, z: Int, top: Double): Boolean
    }

    /** Passes the spots a jump from [x] [y] [z], standing at [height], may land on or catch, nearest first. */
    fun forEach(x: Int, y: Int, z: Int, height: Double, maxDrop: Int, sink: Sink) {
        if (!isEdge(x, y, z, height)) {
            return
        }

        for (offset in OFFSETS) {
            val tx = x + offset.x
            val tz = z + offset.z
            for (ty in y + maxUp downTo y - maxDrop) {
                // Landings in water are not simulated; swimming gets there.
                if (terrain.isWater(tx, ty, tz)) {
                    continue
                }
                val top = terrain.standHeight(tx, ty, tz).ifNaN { catchHeight(tx, ty, tz) }
                val rise = top - height
                if (top.isNaN() || rise > maxRise || offset.distance > maxReach(rise) ||
                    walkReaches(x, y, z, offset, ty - y) || !leavesOverGap(x, y, z, height, offset, ty)
                ) {
                    continue
                }

                if (!sink.accept(tx, ty, tz, top)) {
                    return
                }
            }
        }
    }

    private fun catchHeight(x: Int, y: Int, z: Int) =
        if (terrain.isClimbable(x, y, z) && terrain.hasRoom(x, y, z)) y.toDouble() else Double.NaN

    private fun isEdge(x: Int, y: Int, z: Int, height: Double): Boolean {
        for (i in DX.indices) {
            if (isDip(x + DX[i], y, z + DZ[i], height)) {
                return true
            }
        }
        return false
    }

    /** Whether the body fits into [x] [y] [z] but its ground is missing or lower than [height] by [DIP_DEPTH]. */
    private fun isDip(x: Int, y: Int, z: Int, height: Double): Boolean {
        val stand = terrain.standHeight(x, y, z)
        return terrain.isOpen(x, y, z) && (stand.isNaN() || stand < height - DIP_DEPTH)
    }

    private fun isGap(x: Int, y: Int, z: Int, height: Double) =
        isDip(x, y, z, height) && terrain.standHeight(x, y - 1, z).isNaN()

    /** Whether the line to [offset] leaves the block into the target, over a gap or, to a target no lower, a dip. */
    private fun leavesOverGap(x: Int, y: Int, z: Int, height: Double, offset: Offset, ty: Int): Boolean {
        val steps = max(abs(offset.x), abs(offset.z)) * LINE_SAMPLES
        for (step in 1..steps) {
            // Half a step off, so that a diagonal never samples the very corner between two columns.
            val along = (step - 0.5) / steps
            val cx = Mth.floor(x + 0.5 + offset.x * along)
            val cz = Mth.floor(z + 0.5 + offset.z * along)
            if (cx != x || cz != z) {
                return cx == x + offset.x && cz == z + offset.z ||
                    isGap(cx, y, cz, height) || ty >= y && isDip(cx, y, cz, height)
            }
        }
        return false
    }

    private fun walkReaches(x: Int, y: Int, z: Int, offset: Offset, dy: Int) = when {
        abs(offset.x) > 1 || abs(offset.z) > 1 -> false
        offset.x == 0 || offset.z == 0 -> dy <= 1
        else -> dy == 0 && terrain.isOpen(x + offset.x, y, z) && terrain.isOpen(x, y, z + offset.z)
    }

    private fun maxReach(rise: Double): Double {
        val climb = rise - extraHeight
        val reach = when {
            climb > 0.0 -> 5.1
            climb > -1.0 -> 5.8
            else -> 5.8 - climb * 0.5
        }
        return minOf(reach * reachScale, MAX_REACH.toDouble())
    }

    private class Offset(val x: Int, val z: Int) {
        val distance = sqrt((x * x + z * z).toDouble())
    }

    private companion object {
        const val MAX_RISE = 1.25
        const val LINE_SAMPLES = 10
        const val MAX_REACH = 8

        const val DIP_DEPTH = 0.25

        val DX = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
        val DZ = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)

        val OFFSETS: Array<Offset> = buildList {
            for (x in -MAX_REACH..MAX_REACH) {
                for (z in -MAX_REACH..MAX_REACH) {
                    val offset = Offset(x, z)
                    if ((x != 0 || z != 0) && offset.distance <= MAX_REACH) {
                        add(offset)
                    }
                }
            }
            sortBy(Offset::distance)
        }.toTypedArray()
    }

}
