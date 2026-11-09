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

package net.ccbluex.liquidbounce.utils.navigation.flight

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import net.ccbluex.liquidbounce.utils.block.AStarSearch
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.route.NavigationGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * A flight route from [start]: straight lines through the air between [points], the start included. An incomplete
 * route ends at the reachable spot closest to the goal.
 */
class FlightPath(val start: Vec3, val points: List<Vec3>, val complete: Boolean) {

    val end: Vec3
        get() = points.last()

}

/**
 * Finds a flight route from [start] to [goal]: the straight line when it is clear, else an A* through the blocks the
 * body fits in, in every direction, whose corners are then cut where the straight line between them is clear.
 * Hazards are kept out of; water and the ground, which ends creative flight, cost extra. When the A* gives up, the
 * route climbs straight over whatever is in the way.
 *
 * Not thread-safe: advance it from one thread at a time.
 */
@Suppress("TooManyFunctions")
class FlightRouteSearch(
    private val terrain: NavigationTerrain,
    val start: Vec3,
    val goal: NavigationGoal,
    private val abilities: FlightAbilities,
    private val limits: RouteLimits,
) : RouteSearch<FlightPath> {

    private val open = Long2BooleanOpenHashMap()
    private val minCost = min(abilities.horizontalCost, abilities.verticalCost)
    private var startNode = BlockPos.containing(start)
    private var search: AStarSearch<BlockPos>? = null
    private var computeNanos = 0L

    var result: RouteResult<FlightPath> = RouteResult.Searching
        private set

    override fun advance(): RouteResult<FlightPath> {
        if (result != RouteResult.Searching) {
            return result
        }

        val began = System.nanoTime()
        result = search?.let(::continueSearch) ?: begin()
        computeNanos += System.nanoTime() - began

        if (result == RouteResult.Searching && computeNanos > limits.maxComputeNanos) {
            result = closestOr(RouteFailure.TIMED_OUT)
        }
        return result
    }

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.map(::point)

    private fun begin(): RouteResult<FlightPath> {
        startNode = BlockPos.containing(start).let { feet ->
            if (isOpen(feet.x, feet.y, feet.z) || !isOpen(feet.x, feet.y + 1, feet.z)) feet else feet.above()
        }
        if (goal.isGoal(startNode)) {
            return RouteResult.AlreadyThere
        }

        straightRoute()?.let { return it }

        search = AStarSearch(
            start = startNode,
            isGoal = { goal.isGoal(it) },
            neighbors = ::neighbors,
            heuristic = ::estimate,
        )
        return RouteResult.Searching
    }

    /**
     * The goal distance in ticks, split into its horizontal and vertical part the way the nearest end lies, as
     * climbing and sinking are slower than flying on.
     */
    private fun estimate(node: BlockPos): Double {
        val distance = goal.distance(node)
        val center = Vec3.atBottomCenterOf(node)
        val end = goal.ends.minByOrNull(center::distanceToSqr) ?: return distance * minCost
        val delta = end.subtract(center)
        val length = delta.length()
        if (length < 1.0E-6) {
            return 0.0
        }

        val horizontal = distance * delta.horizontalDistance() / length * abilities.horizontalCost
        val vertical = distance * abs(delta.y) / length * abilities.verticalCost
        return max(horizontal, vertical) * HEURISTIC_WEIGHT
    }

    /**
     * The straight line to the goal, when it is clear.
     */
    private fun straightRoute(): RouteResult<FlightPath>? {
        for (point in goalPoints()) {
            if (isClearLine(terrain, start, point)) {
                return RouteResult.Found(FlightPath(start, listOf(start, point), complete = true))
            }
        }
        return null
    }

    /**
     * Straight up, across and down to the goal at the lowest height where that is clear, for obstacles too wide to
     * search a way around.
     */
    private fun climbRoute(): RouteResult<FlightPath>? {
        for (point in goalPoints()) {
            val base = Mth.floor(max(start.y, point.y))
            for (height in base + 1..min(base + MAX_CLIMB, terrain.maxY)) {
                val up = Vec3(start.x, height.toDouble(), start.z)
                if (!isClearLine(terrain, start, up)) {
                    break
                }

                val over = Vec3(point.x, height.toDouble(), point.z)
                if (isClearLine(terrain, up, over) && isClearLine(terrain, over, point)) {
                    return RouteResult.Found(FlightPath(start, simplify(listOf(start, up, over, point)), true))
                }
            }
        }
        return null
    }

    /**
     * Where the feet rest at the ends of the goal within reach.
     */
    private fun goalPoints(): List<Vec3> = goal.ends.mapNotNull { end ->
        val node = BlockPos.containing(end)
        point(node).takeIf {
            start.distanceTo(end) <= limits.maxLength && goal.isGoal(node) && isOpen(node.x, node.y, node.z)
        }
    }

    private fun continueSearch(search: AStarSearch<BlockPos>): RouteResult<FlightPath> {
        val path = search.advance(minOf(limits.expansionsPerSlice, limits.maxExpansions - search.iterations))
        return when {
            path != null -> RouteResult.Found(path(path.nodes, complete = true))
            search.isFinished -> closestOr(RouteFailure.NO_PATH)
            search.iterations >= limits.maxExpansions -> closestOr(RouteFailure.TOO_COMPLEX)
            else -> RouteResult.Searching
        }
    }

    private fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val edges = ArrayList<WeightedEdge<BlockPos>>(NEIGHBORS)
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0 || !canMove(node, dx, dy, dz)) {
                        continue
                    }

                    val to = node.offset(dx, dy, dz)
                    edges += WeightedEdge(to, cost(to, dx, dy, dz))
                }
            }
        }
        return edges
    }

    /**
     * Whether the body fits in every block the move from [node] by [dx] [dy] [dz] passes, so no corner is cut.
     */
    private fun canMove(node: BlockPos, dx: Int, dy: Int, dz: Int): Boolean {
        for (x in minOf(0, dx)..maxOf(0, dx)) {
            for (y in minOf(0, dy)..maxOf(0, dy)) {
                for (z in minOf(0, dz)..maxOf(0, dz)) {
                    if (!isOpen(node.x + x, node.y + y, node.z + z)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun cost(to: BlockPos, dx: Int, dy: Int, dz: Int): Double {
        val horizontal = Mth.length(dx.toDouble(), dz.toDouble()) * abilities.horizontalCost
        var cost = max(horizontal, abs(dy) * abilities.verticalCost)
        if (terrain.isWater(to.x, to.y, to.z) || terrain.isWater(to.x, to.y + 1, to.z)) {
            cost *= WATER_FACTOR
        }
        if (abilities.control == FlightControl.MOMENTUM && !goal.isGoal(to) &&
            !terrain.standHeight(to.x, to.y, to.z).isNaN()) {
            cost += GROUND_PENALTY
        }
        return cost
    }

    private fun isOpen(x: Int, y: Int, z: Int): Boolean {
        val key = BlockPos.asLong(x, y, z)
        if (open.containsKey(key)) {
            return open.get(key)
        }

        val isOpen = terrain.isKnown(x, y, z) && terrain.isKnown(x, y + 1, z) &&
            terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))
        open.put(key, isOpen)
        return isOpen
    }

    /**
     * Where the feet are in [node]: resting on the ground there, else at the bottom of the block.
     */
    private fun point(node: BlockPos): Vec3 = terrain.feet(node) ?: Vec3.atBottomCenterOf(node)

    /**
     * The climb route, else an incomplete route to the explored spot closest to the goal, or [failure] when it gets
     * no closer than [MIN_PROGRESS].
     */
    private fun closestOr(failure: RouteFailure): RouteResult<FlightPath> {
        climbRoute()?.let { return it }

        val nodes = search?.closestPath()?.nodes
        if (nodes == null || nodes.size < 2 || goal.distance(startNode) - goal.distance(nodes.last()) < MIN_PROGRESS) {
            return RouteResult.Failed(failure)
        }
        return RouteResult.Found(path(nodes, complete = false))
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean): FlightPath {
        val points = ArrayList<Vec3>(nodes.size)
        points += start
        for (index in 1 until nodes.size) {
            points += point(nodes[index])
        }
        return FlightPath(start, simplify(points), complete)
    }

    /**
     * [points] with every corner left out that the line from the last kept corner passes clear.
     */
    private fun simplify(points: List<Vec3>): List<Vec3> {
        val corners = arrayListOf(points.first())
        var anchor = 0
        while (anchor < points.lastIndex) {
            var next = anchor + 1
            while (next < points.lastIndex && isClearLine(terrain, points[anchor], points[next + 1])) {
                next++
            }
            corners += points[next]
            anchor = next
        }
        return corners
    }

    private companion object {
        const val NEIGHBORS = 26
        const val MAX_CLIMB = 32

        /**
         * Weight on the estimate, which finds a route much sooner at the price of one a little longer than the
         * shortest; the corners are cut afterwards anyway.
         */
        const val HEURISTIC_WEIGHT = 1.5
        const val MIN_PROGRESS = 1.0
        const val WATER_FACTOR = 4.0

        /**
         * Ticks added for flying through a block with ground below, as touching it can end the flight.
         */
        const val GROUND_PENALTY = 1.0
    }

}

private const val LINE_SPACING = 0.25
private const val LINE_MARGIN = 0.1
private const val END_LENGTH = 0.5

/**
 * Whether the body moves along the straight line from [from] to [to] without touching anything, with a small margin
 * to the sides away from both ends.
 */
internal fun isClearLine(terrain: NavigationTerrain, from: Vec3, to: Vec3): Boolean {
    val delta = to.subtract(from)
    val length = delta.length()
    val samples = max(1, ceil(length / LINE_SPACING).toInt())
    for (sample in 0..samples) {
        val along = length * sample / samples
        val margin = if (along < END_LENGTH || length - along < END_LENGTH) 0.0 else LINE_MARGIN
        val point = from.fma(sample.toDouble() / samples, delta)
        if (!terrain.isClear(terrain.box(point).inflate(margin, 0.0, margin))) {
            return false
        }
    }
    return true
}
