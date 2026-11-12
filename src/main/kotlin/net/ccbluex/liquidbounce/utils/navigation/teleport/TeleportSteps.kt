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
package net.ccbluex.liquidbounce.utils.navigation.teleport

import it.unimi.dsi.fastutil.ints.IntArrayList
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.world.phys.Vec3
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

class TeleportStep(val position: Vec3, val onGround: Boolean, val segment: Int)

object TeleportSteps {

    // The server adds every descending move to the fall distance, damages a landing after more than three blocks
    // and clears the distance on any rise. The margin covers a fall the player was already in.
    const val MAX_FALL = 2.5

    /** Rise between two falls of a drop, which clears the fall distance. */
    const val BOUNCE = 0.1

    // A vanilla server sets the player back when the k-th position packet since its last tick ends more than
    // 10 * sqrt(k) blocks from where that tick started; from the sixth packet on k counts as 1 again.
    const val MAX_PACKETS_PER_TICK = 5
    private const val SAFE_DISTANCE = 9.5

    private const val EPSILON = 1.0E-6

    /** Breaks [route] into steps the server's collision sweep lets through; null when a part of it is blocked. */
    fun plan(terrain: NavigationTerrain, route: TeleportRoute, stepLength: Double): List<TeleportStep>? {
        val steps = ArrayList<TeleportStep>()
        for (segment in 0 until route.points.lastIndex) {
            if (!appendSegment(terrain, route.points[segment], route.points[segment + 1], stepLength, segment, steps)) {
                return null
            }
        }
        return steps
    }

    /** Rises in the column of [from], crosses at the higher level, then drops onto [to]; false when blocked. */
    fun appendSegment(
        terrain: NavigationTerrain,
        from: Vec3,
        to: Vec3,
        stepLength: Double,
        segment: Int,
        out: MutableList<TeleportStep>,
    ): Boolean {
        val top = max(from.y, to.y)
        var position = from

        if (top - from.y > EPSILON) {
            position = appendLine(terrain, position, from.copy(y = top), stepLength, segment, out) ?: return false
        }

        position = appendLine(terrain, position, to.copy(y = top), stepLength, segment, out) ?: return false
        return top - to.y <= EPSILON || appendDrop(terrain, position, to, stepLength, segment, out)
    }

    private fun appendLine(
        terrain: NavigationTerrain,
        from: Vec3,
        to: Vec3,
        stepLength: Double,
        segment: Int,
        out: MutableList<TeleportStep>,
    ): Vec3? {
        val distance = from.distanceTo(to)
        if (distance < EPSILON) {
            return from
        }

        val count = ceil(distance / stepLength).toInt()
        var previous = from
        for (index in 1..count) {
            val next = if (index == count) to else from.lerp(to, index.toDouble() / count)
            if (!terrain.sweep(previous, next)) {
                return null
            }
            out += TeleportStep(next, terrain.isSupported(next), segment)
            previous = next
        }

        return to
    }

    /** Falls of at most [MAX_FALL] blocks with a [BOUNCE] between two of them. Only the landing claims ground. */
    private fun appendDrop(
        terrain: NavigationTerrain,
        from: Vec3,
        to: Vec3,
        stepLength: Double,
        segment: Int,
        out: MutableList<TeleportStep>,
    ): Boolean {
        val fall = min(stepLength, MAX_FALL)
        var position = from
        var fallen = 0.0

        while (position.y - to.y > EPSILON) {
            val remaining = position.y - to.y
            val next = when {
                fallen + min(fall, remaining) > MAX_FALL + EPSILON -> position.add(0.0, BOUNCE, 0.0)
                remaining <= fall + EPSILON -> to
                else -> Vec3(to.x, position.y - fall, to.z)
            }
            if (!terrain.sweep(position, next)) {
                return false
            }

            fallen = if (next.y > position.y) 0.0 else fallen + position.y - next.y
            out += TeleportStep(next, next === to && terrain.isSupported(to), segment)
            position = next
        }

        return true
    }

    /** How many of [steps] each tick sends, staying inside the server's allowance from where the tick starts. */
    fun batches(start: Vec3, steps: List<TeleportStep>, packetsPerTick: Int): IntArray {
        val perTick = packetsPerTick.coerceIn(1, MAX_PACKETS_PER_TICK)
        val sizes = IntArrayList()
        var tickStart = start
        var inTick = 0

        for ((index, step) in steps.withIndex()) {
            val packet = inTick + 1
            if (inTick > 0 &&
                (packet > perTick || step.position.distanceToSqr(tickStart) >
                    SAFE_DISTANCE * SAFE_DISTANCE * packet.toDouble())
            ) {
                sizes.add(inTick)
                tickStart = steps[index - 1].position
                inTick = 0
            }
            inTick++
        }

        if (inTick > 0) {
            sizes.add(inTick)
        }
        return sizes.toIntArray()
    }

}
