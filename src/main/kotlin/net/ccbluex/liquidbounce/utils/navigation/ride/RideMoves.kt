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
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.math.horizontalDistanceTo
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.ccbluex.liquidbounce.utils.navigation.walk.WalkCosts
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.AABB
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

/**
 * Where a ridden [body] rests and how it moves on. Nodes are the blocks the bottom of the vehicle is in: the water
 * block a boat floats in, else the block above the floor. Wider than a block, it may rest against a side or corner.
 */
@Suppress("TooManyFunctions")
internal class RideMoves(private val terrain: NavigationTerrain, private val body: RideBody) {

    /** Where the vehicle rests in a node, and how fast it moves there in blocks per tick. */
    class Spot(val position: Vec3, val speed: Double)

    private val halfWidth = body.width / 2.0
    private val maxRise = max(body.step, body.jump)
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

    fun isJump(from: Spot, to: Spot) = to.position.y - from.position.y > body.step + EPSILON

    /** The moves from [node] onto the eight columns around it, with the ticks each takes. */
    fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val from = spot(node) ?: return emptyList()
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        for (direction in DIRECTIONS) {
            val x = node.x + direction[0]
            val z = node.z + direction[1]
            addRises(edges, from, x, node.y, z)
            addLevelOrDrop(edges, from, x, node.y, z)
        }
        return edges
    }

    private fun addRises(edges: MutableList<WeightedEdge<BlockPos>>, from: Spot, x: Int, y: Int, z: Int) {
        for (dy in ceil(maxRise).toInt() downTo 1) {
            val to = spot(x, y + dy, z) ?: continue
            val rise = to.position.y - from.position.y
            if (rise > 0.0 && rise <= maxRise + EPSILON && passes(from.position, to.position)) {
                edges += WeightedEdge(BlockPos(x, y + dy, z), cost(from, to))
            }
        }
    }

    /** The move onto the column on the level of [from], or down onto the first floor below. */
    private fun addLevelOrDrop(edges: MutableList<WeightedEdge<BlockPos>>, from: Spot, x: Int, y: Int, z: Int) {
        for (dy in 0 downTo -(body.safeFall + 1)) {
            val to = spot(x, y + dy, z)
            if (to == null) {
                if (terrain.isPassable(x, y + dy, z)) continue else return
            }

            val rise = to.position.y - from.position.y
            if (rise <= body.step + EPSILON && -rise <= body.safeFall + EPSILON &&
                passes(from.position, to.position)) {
                edges += WeightedEdge(BlockPos(x, y + dy, z), cost(from, to))
            }
            return
        }
    }

    /** Whether the vehicle goes straight from [from] to [to] on one level, resting within a step of it all the way. */
    fun isStraight(from: Vec3, to: Vec3): Boolean {
        val level = from.y
        if (abs(to.y - level) > LEVEL_TOLERANCE) {
            return false
        }

        val dx = to.x - from.x
        val dz = to.z - from.z
        val samples = max(1, ceil(Mth.length(dx, dz) / SAMPLE_SPACING).toInt())
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

    private fun computeSpot(x: Int, y: Int, z: Int): Spot? = when {
        !terrain.isKnown(x, y, z) -> null
        body.kind == RideKind.MOUNT -> restingAt(x, terrain.standHeight(x, y, z), z, body.speed)
        isWaterSurface(x, y, z) -> restingAt(x, y + BOAT_WATER_REST, z, body.speed)
        isIce(x, y, z) -> restingAt(x, y.toDouble(), z, body.iceSpeed)
        else -> null
    }

    private fun restingAt(x: Int, rest: Double, z: Int, speed: Double): Spot? {
        if (rest.isNaN()) {
            return null
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

    /** Whether the vehicle fits from [from] to [to]: straight or, sliding along a wall, one axis after the other. */
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
        val samples = max(1, ceil(Mth.length(dx, dz) / SAMPLE_SPACING).toInt())
        for (sample in 0..samples) {
            val t = sample.toDouble() / samples
            if (!terrain.isClear(box(fromX + dx * t, level, fromZ + dz * t))) {
                return false
            }
        }
        return true
    }

    private fun cost(from: Spot, to: Spot): Double {
        val half = from.position.horizontalDistanceTo(to.position) / 2.0
        var ticks = half / from.speed + half / to.speed
        if (isJump(from, to)) {
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

    private companion object {
        // A boat floats this high above the bottom of its water block.
        const val BOAT_WATER_REST = 0.5
        const val EPSILON = 1.0E-4
        const val LIFT = 0.01
        const val SAMPLE_SPACING = 0.5
        const val LEVEL_TOLERANCE = 0.6
        const val ICE_FRICTION = 0.9f

        val NONE = Spot(Vec3.ZERO, 0.0)

        val DIRECTIONS = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1),
        )

        // Rest points within a node: the centre first, then the middles of its sides and its corners.
        val ANCHORS = arrayOf(
            doubleArrayOf(0.5, 0.5),
            doubleArrayOf(0.0, 0.5), doubleArrayOf(1.0, 0.5), doubleArrayOf(0.5, 0.0), doubleArrayOf(0.5, 1.0),
            doubleArrayOf(0.0, 0.0), doubleArrayOf(0.0, 1.0), doubleArrayOf(1.0, 0.0), doubleArrayOf(1.0, 1.0),
        )
    }

}
