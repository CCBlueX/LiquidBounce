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

import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.ccbluex.liquidbounce.utils.block.AStarSearch
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.navigation.route.NavigationGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

/** An A* over feet blocks from [start] to [goal]; an unreachable goal gives the path that gets closest to it. */
class WalkRouteSearch(
    terrain: NavigationTerrain,
    val start: Vec3,
    val goal: NavigationGoal,
    private val abilities: WalkAbilities,
    private val limits: RouteLimits,
) : RouteSearch<WalkPath> {

    private val moves = WalkMoves(terrain, abilities)
    private val speed = if (abilities.canSprint) WalkCosts.SPRINT else WalkCosts.WALK
    private var startNode = BlockPos.containing(start)
    private var search: AStarSearch<BlockPos>? = null
    private var computeNanos = 0L

    /** Moves in a row with the head under water on the best path to each expanded node. */
    private val dives = Long2IntOpenHashMap()
    private val maxDive = WalkWater.movesUnderwater(WalkWater.MAX_AIR)

    var result: RouteResult<WalkPath> = RouteResult.Searching
        private set

    override fun advance(): RouteResult<WalkPath> {
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

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.map(Vec3::atBottomCenterOf)

    private fun begin(): RouteResult<WalkPath> {
        startNode = startNode()
        if (goal.isGoal(startNode)) {
            return RouteResult.AlreadyThere
        }

        search = AStarSearch(
            start = startNode,
            isGoal = { goal.isGoal(it) },
            neighbors = ::neighbors,
            heuristic = { goal.distance(it) * speed },
        )
        return RouteResult.Searching
    }

    /** The block of the feet, the one above or below on a partial block, or the one beside whose edge holds them. */
    private fun startNode(): BlockPos {
        val node = BlockPos.containing(start)
        return when {
            moves.isNode(node.x, node.y, node.z) -> node
            moves.isNode(node.x, node.y + 1, node.z) -> node.above()
            moves.isNode(node.x, node.y - 1, node.z) -> node.below()
            else -> heldBy(node) ?: node
        }
    }

    /** The node beside [node] under the player's footprint closest to the feet, as when leaning out to bridge. */
    private fun heldBy(node: BlockPos): BlockPos? {
        var best: BlockPos? = null
        var bestDistance = Double.MAX_VALUE
        for (x in Mth.floor(start.x - PlayerMotion.HALF_WIDTH)..Mth.floor(start.x + PlayerMotion.HALF_WIDTH)) {
            for (z in Mth.floor(start.z - PlayerMotion.HALF_WIDTH)..Mth.floor(start.z + PlayerMotion.HALF_WIDTH)) {
                val distance = Mth.lengthSquared(x + 0.5 - start.x, z + 0.5 - start.z)
                if ((x != node.x || z != node.z) && distance < bestDistance && moves.isNode(x, node.y, z)) {
                    best = BlockPos(x, node.y, z)
                    bestDistance = distance
                }
            }
        }
        return best
    }

    private fun continueSearch(search: AStarSearch<BlockPos>): RouteResult<WalkPath> {
        val path = search.advance(minOf(limits.expansionsPerSlice, limits.maxExpansions - search.iterations))
        return when {
            path != null -> path(path.nodes, complete = true)
            search.isFinished -> closestOr(RouteFailure.NO_PATH)
            search.iterations >= limits.maxExpansions -> closestOr(RouteFailure.TOO_COMPLEX)
            else -> RouteResult.Searching
        }
    }

    private fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val dive = diveLength(node)
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        // A jump takes at least its time in the air; spots already reached cheaper are not worth simulating.
        val reachable = (search?.cost(node) ?: 0.0) + MIN_JUMP_TICKS
        val sink = MoveSink { to, _, cost, _, _, _, _ ->
            // Out of air, the only way on under water is straight up.
            val upwards = to.x == node.x && to.z == node.z && to.y > node.y
            if (dive >= maxDive && !upwards && moves.isUnderwater(to)) {
                return@MoveSink
            }
            val penalty = if (abilities.avoid.contains(to.asLong())) WalkCosts.AVOID else 0.0
            edges += WeightedEdge(to, cost + penalty)
        }
        moves.forEach(node, sink) { x, y, z ->
            (search?.cost(BlockPos(x, y, z)) ?: Double.POSITIVE_INFINITY) > reachable
        }
        return edges
    }

    /** Moves in a row with the head under water that end at [node]; the start counts the air already used. */
    private fun diveLength(node: BlockPos): Int {
        val dive = when {
            !moves.isUnderwater(node) -> 0
            node == startNode -> maxDive - WalkWater.movesUnderwater(abilities.air)
            else -> (search?.previous(node)?.let { dives.get(it.asLong()) } ?: 0) + 1
        }
        dives.put(node.asLong(), dive)
        return dive
    }

    /** An incomplete path to the explored spot closest to the goal, or [failure] if that is no real progress. */
    private fun closestOr(failure: RouteFailure): RouteResult<WalkPath> {
        val nodes = search?.closestPath()?.nodes
        if (nodes == null || nodes.size < 2 || goal.distance(startNode) - goal.distance(nodes.last()) < MIN_PROGRESS) {
            return RouteResult.Failed(failure)
        }
        return path(nodes, complete = false)
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean): RouteResult<WalkPath> {
        val steps = ArrayList<WalkStep>(nodes.size - 1)
        for (i in 1 until nodes.size) {
            steps += moves.step(nodes[i - 1], nodes[i]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
        }
        return RouteResult.Found(WalkPath(start, steps, complete, abilities))
    }

    private companion object {
        const val MIN_PROGRESS = 1.0

        /** No jump takes fewer ticks: the shortest time in the air. */
        const val MIN_JUMP_TICKS = 6.0
    }

}
