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
import net.ccbluex.liquidbounce.utils.navigation.route.AStarRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/**
 * Finds a teleport route from [start] to the closest of [goals], the blocks the feet end in: a straight one when it
 * is clear, otherwise an A* over standable positions, compacted into few waypoints. Not thread-safe; [retarget] may
 * be called from another thread while it runs. A [seed] of an equal search keeps its progress.
 *
 * [goalFeet] gives the feet position of each goal that has no floor to stand on, so a route can end in mid-air.
 * From a standable position within [AIR_HOP] blocks of such a goal, a clear sweep goes there directly.
 */
class WaypointSearch(
    private val terrain: Terrain,
    val start: Vec3,
    goals: List<BlockPos>,
    limits: RouteLimits,
    private val stepLength: Double,
    private val seed: AStarSearch<BlockPos>? = null,
    goalFeet: Map<BlockPos, Vec3> = emptyMap(),
) : AStarRouteSearch<BlockPos, Route>(limits) {

    private val startNode = BlockPos.containing(start)

    @Volatile
    private var goalSet = goals.toHashSet()

    /** Feet positions of goals without a floor; the rest rest on their support. */
    @Volatile
    private var goalFeet = goalFeet

    /** Feet positions of the goals within reach. */
    @Volatile
    private var targets = emptyList<Vec3>()

    /** Goals the player hovers at, reachable by a direct sweep from close standable positions. */
    @Volatile
    private var airGoals: List<Pair<BlockPos, Vec3>> = emptyList()

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.mapNotNull(::feetOf)

    override fun begin(): RouteResult<Route> {
        collectAirGoals()
        val standable = goalSet.mapNotNull(::feetOf)
        targets = standable.filter { it.closerThan(start, limits.maxLength) }
        return when {
            standable.any { it.distanceToSqr(start) < ARRIVED_SQ } -> RouteResult.AlreadyThere
            standable.isEmpty() -> RouteResult.Failed(RouteFailure.INVALID_DESTINATION)
            targets.isEmpty() -> RouteResult.Failed(RouteFailure.TOO_FAR)
            else -> straightRoute(targets) ?: searchFrom(startNode, maxCost = limits.maxLength, seed = seed)
        }
    }

    /**
     * Points the search at [goals] without losing its progress; false when none is standable. Only valid for goals
     * near the current ones — the heuristic must stay admissible for the edges already queued.
     */
    fun retarget(goals: List<BlockPos>, goalFeet: Map<BlockPos, Vec3> = emptyMap()): Boolean {
        this.goalFeet = goalFeet
        val reachable = goals.mapNotNull(::feetOf).filter { it.closerThan(start, limits.maxLength) }
        if (reachable.isEmpty()) {
            return false
        }

        goalSet = goals.toHashSet()
        targets = reachable
        collectAirGoals()
        return true
    }

    private fun collectAirGoals() {
        val feet = goalFeet
        airGoals = goalSet.mapNotNull { goal ->
            if (terrain.feet(goal) == null) feet[goal]?.let { goal to it } else null
        }
    }

    /** The running A*, to seed a search from the same start over the same terrain. */
    fun scores(): AStarSearch<BlockPos>? = search

    private fun straightRoute(goals: List<Vec3>): RouteResult.Found<Route>? {
        for (goal in goals.sortedBy { it.distanceToSqr(start) }) {
            val points = listOf(start, goal)
            if (Route.lengthOf(points) <= limits.maxLength) {
                Route.plan(terrain, points, stepLength)?.let {
                    return RouteResult.Found(it)
                }
            }
            liftedRoute(goal)?.let { return RouteResult.Found(it) }
        }
        return null
    }

    /**
     * The line lifted over whatever it runs into. A teleport only needs a clear sweep, so a terrace lip
     * or a low overhang between start and goal must not send the search walking down the cliffs: the
     * first blocking spot is raised a few blocks, at most [LIFT_ATTEMPTS] times, each higher than before.
     */
    private fun liftedRoute(goal: Vec3): Route? {
        var height = LIFT_STEP
        repeat(LIFT_ATTEMPTS) {
            val blocked = firstBlockingSpot(start, goal) ?: return null
            val points = listOf(start, blocked.add(0.0, height, 0.0), goal)
            if (Route.lengthOf(points) <= limits.maxLength) {
                Route.plan(terrain, points, stepLength)?.let { return it }
            }
            height += LIFT_STEP
        }
        return null
    }

    /** The last clear point of [from]→[to] before the sweep breaks, sampled as [Route.plan] does. */
    private fun firstBlockingSpot(from: Vec3, to: Vec3): Vec3? {
        val distance = from.distanceTo(to)
        if (distance < EPSILON) {
            return null
        }

        val count = ceil(distance / stepLength).toInt()
        var previous = from
        for (index in 1..count) {
            val next = if (index == count) to else from.lerp(to, index.toDouble() / count)
            if (!terrain.sweep(previous, next)) {
                return previous
            }
            previous = next
        }
        return null
    }

    override fun isGoal(node: BlockPos) = node in goalSet && node != startNode

    /**
     * Octile distance over block coordinates, matching the edge costs of [neighbors]: one or [DIAGONAL] per
     * horizontal step plus the change in y. Admissible and consistent, so the search expands far fewer nodes
     * than the euclidean distance between feet positions did.
     */
    override fun heuristic(node: BlockPos): Double {
        val goals = goalSet
        var best = Double.MAX_VALUE
        for (goal in goals) {
            val dx = abs(node.x - goal.x).toDouble()
            val dy = abs(node.y - goal.y).toDouble()
            val dz = abs(node.z - goal.z).toDouble()
            val horizontal = max(dx, dz) + (DIAGONAL - 1.0) * min(dx, dz)
            val estimate = horizontal + dy
            if (estimate < best) {
                best = estimate
            }
        }
        return best
    }

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

        for ((goal, feet) in airGoals) {
            if (goal != node && from.distanceToSqr(feet) <= AIR_HOP_SQ && terrain.sweep(from, feet)) {
                edges += WeightedEdge(goal, from.distanceTo(feet))
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

    private fun feetOf(node: BlockPos): Vec3? =
        if (node == startNode) start else goalFeet[node] ?: terrain.feet(node)

    private companion object {
        const val ARRIVED_SQ = 0.25 * 0.25
        const val MAX_LOOKAHEAD = 32
        val DIAGONAL = Mth.SQRT_OF_TWO.toDouble()

        // Drops end on the first floor below, so only rises cost a scan of their full height.
        const val MAX_RISE = 24
        const val MAX_DROP = 48

        /** How close a standable position hovers to a goal in mid-air for a direct sweep to reach it. */
        const val AIR_HOP = 8.0
        const val AIR_HOP_SQ = AIR_HOP * AIR_HOP

        /** How far a blocked straight line is lifted per attempt, and how often it is tried at all. */
        const val LIFT_STEP = 2.0
        const val LIFT_ATTEMPTS = 4

        const val EPSILON = 1.0E-6

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
