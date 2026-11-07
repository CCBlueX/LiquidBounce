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

import net.ccbluex.liquidbounce.utils.block.AStarSearch
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.navigation.route.NavigationGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/**
 * One point of a [RidePath]: where the vehicle rests in [node], and whether it jumps to get there.
 */
class RidePoint(val node: BlockPos, val position: Vec3, val jump: Boolean)

/**
 * A route for a ridden vehicle from [start]. An incomplete path ends at the reachable spot closest to the goal;
 * when [exhausted], no spot closer than that can be reached at all.
 */
class RidePath(
    val start: Vec3,
    val points: List<RidePoint>,
    val complete: Boolean,
    val exhausted: Boolean,
) {

    val end: Vec3
        get() = points.lastOrNull()?.position ?: start

    /**
     * Rest positions of the path, the start included.
     */
    val positions: List<Vec3> by lazy { listOf(start) + points.map(RidePoint::position) }

}

/**
 * Finds a route for a ridden vehicle [body] from [start] to [goal], an A* over the nodes of [RideMoves] that runs
 * in slices of [RouteLimits.expansionsPerSlice]. A boat reaches the goal when its rider does, sitting one block
 * above the water block it floats in.
 *
 * Not thread-safe: advance it from one thread at a time.
 */
class RideRouteSearch(
    terrain: NavigationTerrain,
    val start: Vec3,
    val goal: NavigationGoal,
    private val body: RideBody,
    private val limits: RouteLimits,
) : RouteSearch<RidePath> {

    private val moves = RideMoves(terrain, body)
    private var startNode = BlockPos.containing(start)
    private var search: AStarSearch<BlockPos>? = null
    private var computeNanos = 0L
    private var result: RouteResult<RidePath> = RouteResult.Searching

    override fun advance(): RouteResult<RidePath> {
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

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.mapNotNull { moves.spot(it)?.position }

    private fun begin(): RouteResult<RidePath> {
        startNode = startNode()
        if (isGoal(startNode)) {
            return RouteResult.AlreadyThere
        }
        if (moves.spot(startNode) == null) {
            return RouteResult.Failed(RouteFailure.INVALID_DESTINATION)
        }

        search = AStarSearch(
            start = startNode,
            isGoal = ::isGoal,
            neighbors = ::neighbors,
            heuristic = { distance(it) / body.speed },
        )
        return RouteResult.Searching
    }

    private fun isGoal(node: BlockPos) =
        goal.isGoal(node) || body.kind == RideKind.BOAT && goal.isGoal(node.x, node.y + 1, node.z)

    private fun distance(node: BlockPos): Double {
        val distance = goal.distance(node)
        return if (body.kind == RideKind.BOAT) min(distance, goal.distance(node.x, node.y + 1, node.z)) else distance
    }

    /**
     * The node the vehicle is in, or the one above or below when it rests on a partial block or between blocks.
     */
    private fun startNode(): BlockPos {
        val node = BlockPos.containing(start)
        return when {
            moves.spot(node) != null -> node
            moves.spot(node.above()) != null -> node.above()
            moves.spot(node.below()) != null -> node.below()
            else -> node
        }
    }

    private fun continueSearch(search: AStarSearch<BlockPos>): RouteResult<RidePath> {
        val path = search.advance(minOf(limits.expansionsPerSlice, limits.maxExpansions - search.iterations))
        return when {
            path != null -> path(path.nodes, complete = true, exhausted = false)
            search.isFinished -> closestOr(RouteFailure.NO_PATH)
            search.iterations >= limits.maxExpansions -> closestOr(RouteFailure.TOO_COMPLEX)
            else -> RouteResult.Searching
        }
    }

    private fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        moves.forEach(node) { to, cost, _ -> edges += WeightedEdge(to, cost) }
        return edges
    }

    /**
     * An incomplete path to the explored spot closest to the goal, or [failure] when it gets no closer than
     * [MIN_PROGRESS].
     */
    private fun closestOr(failure: RouteFailure): RouteResult<RidePath> {
        val nodes = search?.closestPath()?.nodes
        if (nodes == null || nodes.size < 2 || distance(startNode) - distance(nodes.last()) < MIN_PROGRESS) {
            return RouteResult.Failed(failure)
        }
        return path(nodes, complete = false, exhausted = failure == RouteFailure.NO_PATH)
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean, exhausted: Boolean): RouteResult<RidePath> {
        val points = ArrayList<RidePoint>(nodes.size - 1)
        for (i in 1 until nodes.size) {
            val from = moves.spot(nodes[i - 1]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
            val to = moves.spot(nodes[i]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
            val jump = to.position.y - from.position.y > body.step + JUMP_EPSILON
            points += RidePoint(nodes[i], to.position, jump)
        }
        return RouteResult.Found(RidePath(start, points, complete, exhausted))
    }

    private companion object {
        const val MIN_PROGRESS = 1.0
        const val JUMP_EPSILON = 1.0E-4
    }

}
