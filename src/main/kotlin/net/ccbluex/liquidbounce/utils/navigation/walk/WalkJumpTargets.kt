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

import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.util.Mth
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.sqrt

/**
 * Spots worth a jump from a feet block: ones to stand on within reach, that walking does not get to as fast, across a
 * gap or dip.
 */
internal class WalkJumpTargets(
    val terrain: NavigationTerrain,
    traits: MotionTraits = MotionTraits.DEFAULT,
) {

    private val extraHeight = traits.jumpHeight - MotionTraits.DEFAULT.jumpHeight
    private val maxRise = MAX_RISE + extraHeight
    private val maxUp = ceil(maxRise).toInt()
    private val reachScale = maxOf(1.0, traits.walkSpeed.toDouble() / MotionTraits.DEFAULT.walkSpeed)

    fun interface Sink {
        /**
         * Takes the feet block [x] [y] [z] standing at [top]; false to stop.
         */
        fun accept(x: Int, y: Int, z: Int, top: Double): Boolean
    }

    /**
     * Passes the spots a jump from [x] [y] [z], standing at [height], may land on to [sink], nearest first, from as
     * high as a jump gets down to [maxDrop] blocks lower: ground to stand on, or a ladder or vine to catch, at the
     * height the feet would be. Nothing unless a side or corner is a dip.
     */
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
                val top = terrain.standHeight(tx, ty, tz).takeUnless { it.isNaN() } ?: catchHeight(tx, ty, tz)
                if (top.isNaN() || top - height > maxRise || offset.distance > reach(top - height) ||
                    isPlainMove(x, y, z, offset, ty - y) || !leavesOverGap(x, y, z, height, tx, ty, tz)
                ) {
                    continue
                }

                if (!sink.accept(tx, ty, tz, top)) {
                    return
                }
            }
        }
    }

    /**
     * The bottom of [x] [y] [z] if it is a ladder or vine the body fits in, a spot a jump catches; else NaN.
     */
    private fun catchHeight(x: Int, y: Int, z: Int): Double {
        val catches = terrain.isClimbable(x, y, z) && terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))
        return if (catches) y.toDouble() else Double.NaN
    }

    /**
     * Whether a side or corner of [x] [y] [z], standing at [height], is a dip, see [isDip].
     */
    private fun isEdge(x: Int, y: Int, z: Int, height: Double): Boolean {
        for (i in DX.indices) {
            if (isDip(x + DX[i], y, z + DZ[i], height)) {
                return true
            }
        }
        return false
    }

    /**
     * Whether the body fits into [x] [y] [z] but its ground is missing or clearly lower than [height], like the
     * floor beside a slab.
     */
    private fun isDip(x: Int, y: Int, z: Int, height: Double): Boolean {
        if (!terrain.isPassable(x, y, z) || !terrain.isPassable(x, y + 1, z)) {
            return false
        }
        val stand = terrain.standHeight(x, y, z)
        return stand.isNaN() || stand < height - DIP_DEPTH
    }

    /**
     * A dip without ground to walk on within a block below either.
     */
    private fun isGap(x: Int, y: Int, z: Int, height: Double) =
        isDip(x, y, z, height) && terrain.standHeight(x, y - 1, z).isNaN()

    /**
     * Whether the straight line from the middle of [x] [y] [z], standing at [height], to the middle of column [tx]
     * [tz] leaves the block right into the target column, over a gap, or over a dip towards a target [ty] no lower,
     * which walking down into the dip and up again does not reach as fast, if at all.
     */
    @Suppress("LongParameterList")
    private fun leavesOverGap(x: Int, y: Int, z: Int, height: Double, tx: Int, ty: Int, tz: Int): Boolean {
        val dx = (tx - x).toDouble()
        val dz = (tz - z).toDouble()
        val steps = (maxOf(abs(dx), abs(dz)) * LINE_SAMPLES).toInt()
        for (step in 1..steps) {
            // Half a step off, so that a diagonal never samples the very corner between two columns.
            val along = (step - 0.5) / steps
            val cx = Mth.floor(x + 0.5 + dx * along)
            val cz = Mth.floor(z + 0.5 + dz * along)
            if (cx != x || cz != z) {
                return cx == tx && cz == tz || isGap(cx, y, cz, height) || ty >= y && isDip(cx, y, cz, height)
            }
        }
        return false
    }

    /**
     * Whether walking already leads from [x] [y] [z] to a spot [offset] away and [dy] higher; a diagonal step needs
     * both corners open.
     */
    private fun isPlainMove(x: Int, y: Int, z: Int, offset: Offset, dy: Int): Boolean {
        if (abs(offset.x) > 1 || abs(offset.z) > 1) {
            return false
        }
        if (offset.x == 0 || offset.z == 0) {
            return dy <= 1
        }
        return dy == 0 && isOpen(x + offset.x, y, z) && isOpen(x, y, z + offset.z)
    }

    private fun isOpen(x: Int, y: Int, z: Int) = terrain.isPassable(x, y, z) && terrain.isPassable(x, y + 1, z)

    /**
     * Farthest a sprint jump that rises [rise] blocks can get, from the middle of one block to the middle of another;
     * generous, the simulation tells.
     */
    private fun reach(rise: Double): Double {
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

        /**
         * How much lower ground beside a block has to be to count as a dip.
         */
        const val DIP_DEPTH = 0.25

        val DX = intArrayOf(1, -1, 0, 0, 1, 1, -1, -1)
        val DZ = intArrayOf(0, 0, 1, -1, 1, -1, 1, -1)

        val OFFSETS: List<Offset> = buildList {
            for (x in -MAX_REACH..MAX_REACH) {
                for (z in -MAX_REACH..MAX_REACH) {
                    val offset = Offset(x, z)
                    if ((x != 0 || z != 0) && offset.distance <= MAX_REACH) {
                        add(offset)
                    }
                }
            }
            sortBy(Offset::distance)
        }
    }

}
