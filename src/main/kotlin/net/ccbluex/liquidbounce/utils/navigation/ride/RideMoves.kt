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

package net.ccbluex.liquidbounce.utils.navigation.ride

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkCosts
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Where a ridden [body] rests and how it moves on, over [terrain]. Nodes are the blocks the bottom of the vehicle
 * is in: the water block a boat floats in, else the block above the floor.
 *
 * A vehicle wider than a block may rest off the centre of its node, against the side or corner of the block, so it
 * fits through gaps two blocks wide.
 *
 * Not thread-safe: use it from one thread at a time.
 */
@Suppress("TooManyFunctions")
internal class RideMoves(private val terrain: NavigationTerrain, private val body: RideBody) {

    /**
     * Where the vehicle rests in a node, and how fast it moves there in blocks per tick.
     */
    class Spot(val position: Vec3, val speed: Double)

    private val halfWidth = body.width / 2.0
    private val spots = Long2ObjectOpenHashMap<Spot>()

    fun spot(node: BlockPos) = spot(node.x, node.y, node.z)

    fun spot(x: Int, y: Int, z: Int): Spot? {
        val key = BlockPos.asLong(x, y, z)
        val cached = spots.get(key)
        if (cached != null) {
            return cached.takeIf { it !== NONE }
        }

        val spot = computeSpot(x, y, z)
        spots.put(key, spot ?: NONE)
        return spot
    }

    /**
     * Calls [action] for every node reachable in one move from [node] with the ticks the move takes and whether it
     * needs a jump.
     */
    fun forEach(node: BlockPos, action: (to: BlockPos, cost: Double, jump: Boolean) -> Unit) {
        val from = spot(node) ?: return
        for (direction in DIRECTIONS) {
            val x = node.x + direction[0]
            val z = node.z + direction[1]
            rises(from, x, node.y, z, action)
            levelOrDrop(from, x, node.y, z, action)
        }
    }

    /**
     * Moves up onto the column [x] [z] from [from] in node height [y], stepping or jumping.
     */
    private fun rises(from: Spot, x: Int, y: Int, z: Int, action: (BlockPos, Double, Boolean) -> Unit) {
        val maxRise = max(body.step, body.jump)
        for (dy in ceil(maxRise).toInt() downTo 1) {
            val to = spot(x, y + dy, z) ?: continue
            val rise = to.position.y - from.position.y
            if (rise > 0.0 && rise <= maxRise + EPSILON && passes(from.position, to.position)) {
                val jump = rise > body.step + EPSILON
                action(BlockPos(x, y + dy, z), cost(from, to, jump), jump)
            }
        }
    }

    /**
     * The move onto the column [x] [z] on the level of [from] in node height [y], or down onto the first floor below.
     */
    private fun levelOrDrop(from: Spot, x: Int, y: Int, z: Int, action: (BlockPos, Double, Boolean) -> Unit) {
        for (dy in 0 downTo -(body.safeFall + 1)) {
            val to = spot(x, y + dy, z)
            if (to == null) {
                if (terrain.isPassable(x, y + dy, z)) continue else return
            }

            val rise = to.position.y - from.position.y
            if (rise <= body.step + EPSILON && -rise <= body.safeFall + EPSILON &&
                passes(from.position, to.position)) {
                action(BlockPos(x, y + dy, z), cost(from, to, false), false)
            }
            return
        }
    }

    /**
     * Whether the vehicle goes straight from [from] to [to] on one level: every point on the way rests in a node
     * within a step of the level, with room for the vehicle and its rider.
     */
    fun isStraight(from: Vec3, to: Vec3): Boolean {
        val level = from.y
        if (abs(to.y - level) > LEVEL_TOLERANCE) {
            return false
        }

        val dx = to.x - from.x
        val dz = to.z - from.z
        val samples = max(1, ceil(sqrt(dx * dx + dz * dz) / SAMPLE_SPACING).toInt())
        val nodeY = Mth.floor(level + EPSILON)
        for (sample in 1..samples) {
            val t = sample.toDouble() / samples
            val x = from.x + dx * t
            val z = from.z + dz * t
            val spot = spot(Mth.floor(x), nodeY, Mth.floor(z)) ?: return false
            if (abs(spot.position.y - level) > LEVEL_TOLERANCE || !terrain.isClear(box(x, level, z))) {
                return false
            }
        }
        return true
    }

    private fun computeSpot(x: Int, y: Int, z: Int): Spot? {
        if (!terrain.isKnown(x, y, z)) {
            return null
        }

        val speed: Double
        val rest: Double
        when (body.kind) {
            RideKind.BOAT -> when {
                isWaterSurface(x, y, z) -> {
                    rest = y + BOAT_WATER_REST
                    speed = body.speed
                }

                isIce(x, y, z) -> {
                    rest = y.toDouble()
                    speed = body.iceSpeed
                }

                else -> return null
            }

            RideKind.MOUNT -> {
                rest = terrain.standHeight(x, y, z)
                if (rest.isNaN()) {
                    return null
                }
                speed = body.speed
            }
        }

        for (anchor in ANCHORS) {
            val position = Vec3(x + anchor[0], rest, z + anchor[1])
            if (terrain.isClear(box(position.x, rest, position.z))) {
                return Spot(position, speed)
            }
        }
        return null
    }

    private fun isWaterSurface(x: Int, y: Int, z: Int) = terrain.isWater(x, y, z) && !terrain.isWater(x, y + 1, z)

    private fun isIce(x: Int, y: Int, z: Int) = !terrain.isWater(x, y, z) && terrain.isPassable(x, y, z) &&
        terrain.state(x, y - 1, z).block.friction >= ICE_FRICTION && !terrain.isPassable(x, y - 1, z)

    /**
     * Whether the vehicle fits all the way from [from] to [to], level with the higher of both: straight or, as it
     * slides along what it bumps into, along one axis and then the other.
     */
    private fun passes(from: Vec3, to: Vec3): Boolean {
        val level = max(from.y, to.y)
        if (isClearLine(from.x, from.z, to.x, to.z, level)) {
            return true
        }
        if (from.x == to.x || from.z == to.z) {
            return false
        }

        return isClearLine(from.x, from.z, from.x, to.z, level) && isClearLine(from.x, to.z, to.x, to.z, level) ||
            isClearLine(from.x, from.z, to.x, from.z, level) && isClearLine(to.x, from.z, to.x, to.z, level)
    }

    private fun isClearLine(fromX: Double, fromZ: Double, toX: Double, toZ: Double, level: Double): Boolean {
        val dx = toX - fromX
        val dz = toZ - fromZ
        val samples = max(1, ceil(sqrt(dx * dx + dz * dz) / SAMPLE_SPACING).toInt())
        for (sample in 0..samples) {
            val t = sample.toDouble() / samples
            if (!terrain.isClear(box(fromX + dx * t, level, fromZ + dz * t))) {
                return false
            }
        }
        return true
    }

    private fun cost(from: Spot, to: Spot, jump: Boolean): Double {
        val dx = to.position.x - from.position.x
        val dz = to.position.z - from.position.z
        val half = sqrt(dx * dx + dz * dz) / 2.0
        var ticks = half / from.speed + half / to.speed
        if (jump) {
            ticks += RideBody.JUMP_CHARGE_TICKS + WalkCosts.fall(Mth.ceil(body.jump))
        }

        val drop = from.position.y - to.position.y
        if (drop > body.step) {
            ticks += WalkCosts.fall(Mth.ceil(drop))
        }
        return ticks
    }

    private fun box(x: Double, y: Double, z: Double) =
        AABB(x - halfWidth, y + LIFT, z - halfWidth, x + halfWidth, y + body.height, z + halfWidth)

    companion object {
        /**
         * How high above the bottom of its water block a boat floats.
         */
        const val BOAT_WATER_REST = 0.5

        private const val EPSILON = 1.0E-4
        private const val LIFT = 0.01
        private const val SAMPLE_SPACING = 0.5
        private const val LEVEL_TOLERANCE = 0.6
        private const val ICE_FRICTION = 0.9f

        private val NONE = Spot(Vec3.ZERO, 0.0)

        private val DIRECTIONS = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1),
        )

        /**
         * Rest points within a node, the centre first, then the middles of its sides and its corners.
         */
        private val ANCHORS = arrayOf(
            doubleArrayOf(0.5, 0.5),
            doubleArrayOf(0.0, 0.5), doubleArrayOf(1.0, 0.5), doubleArrayOf(0.5, 0.0), doubleArrayOf(0.5, 1.0),
            doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0, 1.0),
        )
    }

}
