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

import net.ccbluex.liquidbounce.utils.block.AStarSearch
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.route.RouteSearch
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Finds a teleport route from [start] to the closest of [goals], the blocks the feet end in.
 *
 * A straight route when it is clear, otherwise an A* over standable positions that runs in slices of
 * [RouteLimits.expansionsPerSlice] and gets compacted into few waypoints.
 *
 * Not thread-safe: advance it from one thread at a time.
 */
@Suppress("TooManyFunctions")
class TeleportRouteSearch(
    private val terrain: NavigationTerrain,
    val start: Vec3,
    val goals: List<BlockPos>,
    private val limits: RouteLimits,
    private val stepLength: Double,
) : RouteSearch<TeleportPlan> {

    private val startNode = BlockPos.containing(start)
    private val goalSet = goals.toHashSet()
    private var search: AStarSearch<BlockPos>? = null
    private var computeNanos = 0L

    var result: RouteResult<TeleportPlan> = RouteResult.Searching
        private set

    override fun advance(): RouteResult<TeleportPlan> {
        if (result != RouteResult.Searching) {
            return result
        }

        val began = System.nanoTime()
        result = search?.let(::continueSearch) ?: begin()
        computeNanos += System.nanoTime() - began

        if (result == RouteResult.Searching && computeNanos > limits.maxComputeNanos) {
            result = RouteResult.Failed(RouteFailure.TIMED_OUT)
        }
        return result
    }

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.mapNotNull(::feetOf)

    private fun begin(): RouteResult<TeleportPlan> {
        val standable = goals.mapNotNull { goal -> feetOf(goal)?.let { goal to it } }
        if (standable.any { (_, feet) -> feet.distanceToSqr(start) < ARRIVED_SQ }) {
            return RouteResult.AlreadyThere
        }

        val inReach = standable.filter { (_, feet) -> feet.distanceTo(start) <= limits.maxLength }
        return when {
            standable.isEmpty() -> RouteResult.Failed(RouteFailure.INVALID_DESTINATION)
            inReach.isEmpty() -> RouteResult.Failed(RouteFailure.TOO_FAR)
            else -> straightRoute(inReach.map { it.second }) ?: startSearch(inReach.map { it.second })
        }
    }

    private fun straightRoute(goals: List<Vec3>): RouteResult.Found<TeleportPlan>? {
        for (goal in goals.sortedBy { it.distanceToSqr(start) }) {
            val route = TeleportRoute(listOf(start, goal))
            if (route.length <= limits.maxLength) {
                TeleportSteps.plan(terrain, route, stepLength)?.let {
                    return RouteResult.Found(TeleportPlan(route, it))
                }
            }
        }
        return null
    }

    private fun startSearch(goalFeet: List<Vec3>): RouteResult<TeleportPlan> {
        search = AStarSearch(
            start = startNode,
            isGoal = { it in goalSet && it != startNode },
            neighbors = ::neighbors,
            heuristic = { node -> feetOf(node)?.let { feet -> goalFeet.minOf(feet::distanceTo) } ?: 0.0 },
            maxCost = limits.maxLength,
        )
        return RouteResult.Searching
    }

    private fun continueSearch(search: AStarSearch<BlockPos>): RouteResult<TeleportPlan> {
        val path = search.advance(limits.expansionsPerSlice)
        return when {
            path != null -> compact(path.nodes)
            search.isFinished -> RouteResult.Failed(RouteFailure.NO_PATH)
            search.iterations >= limits.maxExpansions -> RouteResult.Failed(RouteFailure.TOO_COMPLEX)
            else -> RouteResult.Searching
        }
    }

    private fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val from = feetOf(node) ?: return emptyList()
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        val canRise = isRiseClear(from, node.y + 1.0)

        for ((dx, dz) in HORIZONTAL) {
            val horizontal = if (dx != 0 && dz != 0) DIAGONAL else 1.0
            addLanding(edges, node, from, node.x + dx, node.z + dz, horizontal)
            if (canRise) {
                addRises(edges, node, from, node.x + dx, node.z + dz, horizontal)
            }
        }

        return edges
    }

    /**
     * The first standable position in column [x] [z] at or below the level of [node]. Its floor hides every
     * lower one.
     */
    private fun addLanding(
        edges: MutableList<WeightedEdge<BlockPos>>,
        node: BlockPos,
        from: Vec3,
        x: Int,
        z: Int,
        horizontal: Double,
    ) {
        val lowest = max(node.y - MAX_DROP, terrain.minY + 1)

        for (y in node.y downTo lowest) {
            val height = terrain.standHeight(x, y, z)
            if (!height.isNaN()) {
                val to = Vec3(x + 0.5, height, z + 0.5)
                if (isRiseClear(from, height) && isCrossingClear(from, to)) {
                    edges += WeightedEdge(BlockPos(x, y, z), horizontal + abs(y - node.y))
                }
                return
            }

            if (!terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))) {
                return
            }
        }
    }

    /**
     * Standable positions in column [x] [z] above the level of [node], reached by rising in the column of [node].
     * Stops at the first one a ceiling keeps the rise from, as every higher one needs a longer rise.
     */
    private fun addRises(
        edges: MutableList<WeightedEdge<BlockPos>>,
        node: BlockPos,
        from: Vec3,
        x: Int,
        z: Int,
        horizontal: Double,
    ) {
        val highest = min(node.y + MAX_RISE, terrain.maxY)

        for (y in node.y + 1..highest) {
            val height = terrain.standHeight(x, y, z)
            if (height.isNaN()) {
                continue
            }
            if (!isRiseClear(from, height)) {
                return
            }

            if (isCrossingClear(from, Vec3(x + 0.5, height, z + 0.5))) {
                edges += WeightedEdge(BlockPos(x, y, z), horizontal + (y - node.y))
            }
        }
    }

    private fun isRiseClear(from: Vec3, height: Double) =
        height <= from.y || terrain.isClear(terrain.box(from).expandTowards(0.0, height - from.y, 0.0))

    /**
     * The crossing at the higher level and the drop into the column of [to], checked as boxes.
     */
    private fun isCrossingClear(from: Vec3, to: Vec3): Boolean {
        val top = max(from.y, to.y)
        val crossing = terrain.box(from.x, top, from.z).minmax(terrain.box(to.x, top, to.z))

        return terrain.isClear(crossing) &&
            (to.y >= from.y || terrain.isClear(terrain.box(to).expandTowards(0.0, from.y - to.y, 0.0)))
    }

    /**
     * Keeps a node only where the straight route from the previous waypoint stops being clear.
     */
    private fun compact(nodes: List<BlockPos>): RouteResult<TeleportPlan> {
        val positions = nodes.map { feetOf(it) ?: return RouteResult.Failed(RouteFailure.NO_PATH) }
        val waypoints = arrayListOf(positions.first())
        val steps = ArrayList<TeleportStep>()
        val scratch = ArrayList<TeleportStep>()
        val accepted = ArrayList<TeleportStep>()

        var anchor = 0
        while (anchor < positions.lastIndex) {
            var next = -1
            accepted.clear()

            val farthest = minOf(positions.lastIndex, anchor + MAX_LOOKAHEAD)
            for (candidate in anchor + 1..farthest) {
                scratch.clear()
                val clear = TeleportSteps.appendSegment(
                    terrain, positions[anchor], positions[candidate], stepLength, waypoints.size - 1, scratch
                )
                if (!clear) {
                    break
                }

                next = candidate
                accepted.clear()
                accepted.addAll(scratch)
            }

            if (next == -1) {
                return RouteResult.Failed(RouteFailure.NO_PATH)
            }

            waypoints += positions[next]
            steps += accepted
            anchor = next

            if (waypoints.size - 1 > limits.maxWaypoints) {
                return RouteResult.Failed(RouteFailure.TOO_COMPLEX)
            }
        }

        val route = TeleportRoute(waypoints)
        if (route.length > limits.maxLength) {
            return RouteResult.Failed(RouteFailure.TOO_FAR)
        }
        return RouteResult.Found(TeleportPlan(route, steps))
    }

    private fun feetOf(node: BlockPos): Vec3? = if (node == startNode) start else terrain.feet(node)

    companion object {
        private const val ARRIVED_SQ = 0.25 * 0.25
        private const val MAX_LOOKAHEAD = 32
        private val DIAGONAL = sqrt(2.0)

        /**
         * Highest rise and deepest drop to a neighbouring column. Drops end on the first floor below, so only
         * rises cost a scan of their full height.
         */
        private const val MAX_RISE = 24
        private const val MAX_DROP = 48

        private val HORIZONTAL = arrayOf(
            1 to 0, -1 to 0, 0 to 1, 0 to -1,
            1 to 1, 1 to -1, -1 to 1, -1 to -1,
        )
    }

}
