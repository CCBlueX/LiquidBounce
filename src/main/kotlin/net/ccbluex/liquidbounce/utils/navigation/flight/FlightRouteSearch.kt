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

import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.math.fma
import net.ccbluex.liquidbounce.utils.navigation.route.AStarRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.NavigationGoal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.NavigationTerrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

/** Straight lines through the air between [points], the start first; incomplete, to the closest spot reached. */
class FlightPath(val start: Vec3, val points: List<Vec3>, val complete: Boolean) {

    val end: Vec3
        get() = points.last()

}

/**
 * Finds a flight route: the straight line when it is clear, else an A* over [FlightMoves] whose corners are cut
 * where the line between them is clear, else, when the A* gives up, a climb straight over whatever is in the way.
 */
class FlightRouteSearch(
    private val terrain: NavigationTerrain,
    private val start: Vec3,
    private val goal: NavigationGoal,
    private val abilities: FlightAbilities,
    limits: RouteLimits,
) : AStarRouteSearch<BlockPos, FlightPath>(limits) {

    private val moves = FlightMoves(terrain, abilities, goal)
    private val minCost = min(abilities.horizontalCost, abilities.verticalCost)

    private val goalPoints by lazy {
        goal.ends.mapNotNull { end ->
            val node = BlockPos.containing(end)
            moves.point(node).takeIf {
                start.distanceTo(end) <= limits.maxLength && goal.isGoal(node) && moves.isOpen(node.x, node.y, node.z)
            }
        }
    }

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.map(moves::point)

    override fun begin(): RouteResult<FlightPath> {
        val feet = BlockPos.containing(start)
        val fitsAbove = !moves.isOpen(feet.x, feet.y, feet.z) && moves.isOpen(feet.x, feet.y + 1, feet.z)
        val startNode = if (fitsAbove) feet.above() else feet
        if (goal.isGoal(startNode)) {
            return RouteResult.AlreadyThere
        }
        return straightRoute() ?: searchFrom(startNode)
    }

    override fun isGoal(node: BlockPos) = goal.isGoal(node)

    override fun neighbors(node: BlockPos) = moves.neighbors(node)

    // Ticks to the goal, split the way its nearest end lies, as flying up and down is slower than flying on.
    override fun heuristic(node: BlockPos): Double {
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

    override fun route(nodes: List<BlockPos>) = RouteResult.Found(path(nodes, complete = true))

    override fun closestOr(failure: RouteFailure): RouteResult<FlightPath> {
        climbRoute()?.let { return it }

        val nodes = closestNodes { goal.distance(it) } ?: return RouteResult.Failed(failure)
        return RouteResult.Found(path(nodes, complete = false))
    }

    private fun straightRoute(): RouteResult<FlightPath>? {
        val point = goalPoints.firstOrNull { isClearLine(terrain, start, it) } ?: return null
        return RouteResult.Found(FlightPath(start, listOf(start, point), complete = true))
    }

    /** Up, across and down at the lowest height where that is clear, for obstacles too wide to search around. */
    private fun climbRoute(): RouteResult<FlightPath>? {
        for (point in goalPoints) {
            val base = Mth.floor(max(start.y, point.y))
            for (height in base + 1..min(base + MAX_CLIMB, terrain.maxY)) {
                val up = start.copy(y = height.toDouble())
                if (!isClearLine(terrain, start, up)) {
                    break
                }

                val over = point.copy(y = height.toDouble())
                if (isClearLine(terrain, up, over) && isClearLine(terrain, over, point)) {
                    return RouteResult.Found(FlightPath(start, simplify(listOf(start, up, over, point)), true))
                }
            }
        }
        return null
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean): FlightPath {
        val points = ArrayList<Vec3>(nodes.size)
        points += start
        for (index in 1 until nodes.size) {
            points += moves.point(nodes[index])
        }
        return FlightPath(start, simplify(points), complete)
    }

    /** [points] without the corners that the line from the last kept corner passes clear. */
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
        const val MAX_CLIMB = 32

        // Finds a route much sooner for one a little longer than the shortest; its corners are cut anyway.
        const val HEURISTIC_WEIGHT = 1.5
    }

}

private const val LINE_SPACING = 0.25
private const val LINE_MARGIN = 0.1
private const val END_LENGTH = 0.5

/** Whether the body flies straight from [from] to [to] touching nothing, with a side margin away from both ends. */
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
