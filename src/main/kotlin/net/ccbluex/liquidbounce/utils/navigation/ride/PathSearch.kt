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

import net.ccbluex.liquidbounce.utils.navigation.route.AStarRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.Goal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.math.min

/** One point of a [Path]: where the vehicle rests in [node], and whether it jumps to get there. */
class Point(val node: BlockPos, val position: Vec3, val jump: Boolean)

/** A ride from [start]. Incomplete, it ends at the closest spot reached; [exhausted], no closer one can be reached. */
class Path(val start: Vec3, val points: List<Point>, val complete: Boolean, val exhausted: Boolean) {

    /** Rest positions of the path, the start included. */
    val positions: List<Vec3> = listOf(start) + points.map(Point::position)

    val end: Vec3
        get() = positions.last()

}

/** Finds a route for a ridden vehicle [body] from [start] to [goal], an A* over the nodes of [Moves]. */
class PathSearch(
    terrain: Terrain,
    private val start: Vec3,
    private val goal: Goal,
    private val body: Body,
    limits: RouteLimits,
) : AStarRouteSearch<BlockPos, Path>(limits) {

    private val moves = Moves(terrain, body)

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.mapNotNull { moves.spot(it)?.position }

    override fun begin(): RouteResult<Path> {
        val startNode = startNode()
        return when {
            isGoal(startNode) -> RouteResult.AlreadyThere
            moves.spot(startNode) == null -> RouteResult.Failed(RouteFailure.INVALID_DESTINATION)
            else -> searchFrom(startNode)
        }
    }

    private fun startNode(): BlockPos {
        val node = BlockPos.containing(start)
        return when {
            moves.spot(node) != null -> node
            moves.spot(node.above()) != null -> node.above()
            moves.spot(node.below()) != null -> node.below()
            else -> node
        }
    }

    // A boat is there when its rider is, who sits one block above the water block it floats in.
    override fun isGoal(node: BlockPos) =
        goal.isGoal(node) || body.kind == VehicleKind.BOAT && goal.isGoal(node.x, node.y + 1, node.z)

    private fun distance(node: BlockPos): Double {
        val distance = goal.distance(node)
        return if (body.kind == VehicleKind.BOAT) min(distance, goal.distance(node.x, node.y + 1, node.z)) else distance
    }

    override fun neighbors(node: BlockPos) = moves.neighbors(node)

    override fun heuristic(node: BlockPos) = distance(node) / body.speed

    override fun route(nodes: List<BlockPos>) = path(nodes, complete = true, exhausted = false)

    override fun closestOr(failure: RouteFailure): RouteResult<Path> {
        val nodes = closestNodes(::distance) ?: return RouteResult.Failed(failure)
        return path(nodes, complete = false, exhausted = failure == RouteFailure.NO_PATH)
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean, exhausted: Boolean): RouteResult<Path> {
        val points = ArrayList<Point>(nodes.size - 1)
        for (i in 1 until nodes.size) {
            val from = moves.spot(nodes[i - 1]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
            val to = moves.spot(nodes[i]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
            points += Point(nodes[i], to.position, moves.isJump(from, to))
        }
        return RouteResult.Found(Path(start, points, complete, exhausted))
    }

}
