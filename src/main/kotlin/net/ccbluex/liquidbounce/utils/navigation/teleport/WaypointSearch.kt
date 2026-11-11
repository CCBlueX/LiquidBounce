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

import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.navigation.route.AStarRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Finds a teleport route from [start] to the closest of [goals], the blocks the feet end in: a straight one when it
 * is clear, otherwise an A* over standable positions, compacted into few waypoints. Not thread-safe.
 */
class WaypointSearch(
    private val terrain: Terrain,
    private val start: Vec3,
    private val goals: List<BlockPos>,
    limits: RouteLimits,
    private val stepLength: Double,
) : AStarRouteSearch<BlockPos, Route>(limits) {

    private val startNode = BlockPos.containing(start)
    private val goalSet = goals.toHashSet()

    /** Feet positions of the goals within reach. */
    private var targets = emptyList<Vec3>()

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.mapNotNull(::feetOf)

    override fun begin(): RouteResult<Route> {
        val standable = goals.mapNotNull(::feetOf)
        targets = standable.filter { it.closerThan(start, limits.maxLength) }
        return when {
            standable.any { it.distanceToSqr(start) < ARRIVED_SQ } -> RouteResult.AlreadyThere
            standable.isEmpty() -> RouteResult.Failed(RouteFailure.INVALID_DESTINATION)
            targets.isEmpty() -> RouteResult.Failed(RouteFailure.TOO_FAR)
            else -> straightRoute(targets) ?: searchFrom(startNode, maxCost = limits.maxLength)
        }
    }

    private fun straightRoute(goals: List<Vec3>): RouteResult.Found<Route>? {
        for (goal in goals.sortedBy { it.distanceToSqr(start) }) {
            val points = listOf(start, goal)
            if (Route.lengthOf(points) <= limits.maxLength) {
                Route.plan(terrain, points, stepLength)?.let {
                    return RouteResult.Found(it)
                }
            }
        }
        return null
    }

    override fun isGoal(node: BlockPos) = node in goalSet && node != startNode

    override fun heuristic(node: BlockPos) = feetOf(node)?.let { feet -> targets.minOf(feet::distanceTo) } ?: 0.0

    override fun route(nodes: List<BlockPos>) = compact(nodes)

    override fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val from = feetOf(node) ?: return emptyList()
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        val canRise = terrain.isRiseClear(from, node.y + 1.0)

        for ((dx, dz) in HORIZONTAL) {
            val horizontal = if (dx != 0 && dz != 0) DIAGONAL else 1.0
            addLanding(edges, node, from, node.x + dx, node.z + dz, horizontal)
            if (canRise) {
                addRises(edges, node, from, node.x + dx, node.z + dz, horizontal)
            }
        }

        return edges
    }

    /** The first standable position in column [x] [z] at or below [node]. Its floor hides every lower one. */
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
                if (terrain.isRiseClear(from, height) && terrain.isCrossingClear(from, to)) {
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
     * Standable positions in column [x] [z] above [node], reached by rising in the column of [node]. Stops at the
     * first one a ceiling keeps the rise from, as every higher one needs a longer rise.
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
            if (!terrain.isRiseClear(from, height)) {
                return
            }

            if (terrain.isCrossingClear(from, Vec3(x + 0.5, height, z + 0.5))) {
                edges += WeightedEdge(BlockPos(x, y, z), horizontal + (y - node.y))
            }
        }
    }

    /** Keeps a node only where the straight route from the previous waypoint stops being clear. */
    private fun compact(nodes: List<BlockPos>): RouteResult<Route> {
        val positions = nodes.map { feetOf(it) ?: return RouteResult.Failed(RouteFailure.NO_PATH) }
        val waypoints = arrayListOf(positions.first())
        val steps = ArrayList<Route.Step>()
        val scratch = ArrayList<Route.Step>()
        val accepted = ArrayList<Route.Step>()

        var anchor = 0
        while (anchor < positions.lastIndex) {
            var next = -1
            val farthest = min(positions.lastIndex, anchor + MAX_LOOKAHEAD)
            for (candidate in anchor + 1..farthest) {
                scratch.clear()
                val clear = Route.appendSegment(
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

        val route = Route(waypoints, steps)
        if (route.length > limits.maxLength) {
            return RouteResult.Failed(RouteFailure.TOO_FAR)
        }
        return RouteResult.Found(route)
    }

    private fun feetOf(node: BlockPos): Vec3? = if (node == startNode) start else terrain.feet(node)

    private companion object {
        const val ARRIVED_SQ = 0.25 * 0.25
        const val MAX_LOOKAHEAD = 32
        val DIAGONAL = Mth.SQRT_OF_TWO.toDouble()

        // Drops end on the first floor below, so only rises cost a scan of their full height.
        const val MAX_RISE = 24
        const val MAX_DROP = 48

        val HORIZONTAL = arrayOf(
            intArrayOf(1, 0), intArrayOf(-1, 0), intArrayOf(0, 1), intArrayOf(0, -1),
            intArrayOf(1, 1), intArrayOf(1, -1), intArrayOf(-1, 1), intArrayOf(-1, -1),
        )
    }

}

private fun Terrain.isRiseClear(from: Vec3, height: Double) =
    height <= from.y || isClear(box(from).expandTowards(0.0, height - from.y, 0.0))

/** The crossing at the higher level and the drop into the column of [to], checked as boxes. */
private fun Terrain.isCrossingClear(from: Vec3, to: Vec3): Boolean {
    val top = max(from.y, to.y)
    val crossing = box(from.x, top, from.z).minmax(box(to.x, top, to.z))

    return isClear(crossing) && (to.y >= from.y || isClear(box(to).expandTowards(0.0, from.y - to.y, 0.0)))
}
