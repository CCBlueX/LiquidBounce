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

import it.unimi.dsi.fastutil.ints.IntArrayList
import it.unimi.dsi.fastutil.longs.Long2DoubleOpenHashMap
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.entity.PlayerMotion
import net.ccbluex.liquidbounce.utils.navigation.route.AStarRouteSearch
import net.ccbluex.liquidbounce.utils.navigation.route.Goal
import net.ccbluex.liquidbounce.utils.navigation.route.RouteFailure
import net.ccbluex.liquidbounce.utils.navigation.route.RouteLimits
import net.ccbluex.liquidbounce.utils.navigation.route.RouteResult
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3

/** An A* over feet blocks from [start] to [goal]; an unreachable goal gives the path that gets closest to it. */
@Suppress("TooManyFunctions")
class PathSearch(
    terrain: Terrain,
    val start: Vec3,
    val goal: Goal,
    private val abilities: Abilities,
    limits: RouteLimits,
) : AStarRouteSearch<BlockPos, Path>(limits) {

    private val moves = Moves(terrain, abilities)
    private val speed = if (abilities.canSprint) Costs.SPRINT else Costs.WALK
    private var startNode = BlockPos.containing(start)

    /** Moves in a row with the head under water on the best path to each expanded node. */
    private val dives = Long2IntOpenHashMap()
    private val maxDive = Water.movesUnderwater(Water.MAX_AIR)

    /** Blocks placed on the best known way to each reached node; no path places more than are at hand. */
    private val placed = Long2IntOpenHashMap()
    private val placedAfter = IntArrayList()
    private val offered = Long2DoubleOpenHashMap()

    override fun preview(): List<Vec3>? = search?.closestPath()?.nodes?.map(Vec3::atBottomCenterOf)

    override fun begin(): RouteResult<Path> {
        startNode = startNode()
        return if (isGoal(startNode)) RouteResult.AlreadyThere else searchFrom(startNode)
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

    override fun isGoal(node: BlockPos) = goal.isGoal(node)

    override fun heuristic(node: BlockPos) = goal.distance(node) * speed

    override fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val dive = diveLength(node)
        val placedBefore = placed.get(node.asLong())
        val edges = ArrayList<WeightedEdge<BlockPos>>()
        placedAfter.clear()
        // A jump takes at least its time in the air; spots already reached cheaper are not worth simulating.
        val reachable = (search?.cost(node) ?: 0.0) + MIN_JUMP_TICKS
        val sink = MoveSink { to, _, cost, _, places, _, _ ->
            // Out of air, the only way on under water is straight up.
            val upwards = to.x == node.x && to.z == node.z && to.y > node.y
            if (dive >= maxDive && !upwards && moves.isUnderwater(to)) {
                return@MoveSink
            }
            val placedTotal = placedBefore + places.size
            if (placedTotal > abilities.placeableBlocks) {
                return@MoveSink
            }
            val penalty = if (abilities.avoid.contains(to.asLong())) Costs.AVOID else 0.0
            edges += WeightedEdge(to, cost + penalty)
            placedAfter.add(placedTotal)
        }
        moves.forEach(node, sink) { x, y, z ->
            (search?.cost(BlockPos(x, y, z)) ?: Double.POSITIVE_INFINITY) > reachable
        }
        if (abilities.canPlace) {
            recordPlaced(node, edges)
        }
        return edges
    }

    /** Keeps the count of the edge the A* takes to each neighbour: the first cheapest that beats the known cost. */
    private fun recordPlaced(node: BlockPos, edges: List<WeightedEdge<BlockPos>>) {
        val search = search ?: return
        val from = search.cost(node)
        offered.clear()
        for (i in edges.indices) {
            val edge = edges[i]
            val key = edge.node.asLong()
            val cost = from + edge.cost
            val best = if (offered.containsKey(key)) offered.get(key) else search.cost(edge.node)
            if (cost < best) {
                offered.put(key, cost)
                placed.put(key, placedAfter.getInt(i))
            }
        }
    }

    /** Moves in a row with the head under water that end at [node]; the start counts the air already used. */
    private fun diveLength(node: BlockPos): Int {
        val dive = when {
            !moves.isUnderwater(node) -> 0
            node == startNode -> maxDive - Water.movesUnderwater(abilities.air)
            else -> (search?.previous(node)?.let { dives.get(it.asLong()) } ?: 0) + 1
        }
        dives.put(node.asLong(), dive)
        return dive
    }

    override fun route(nodes: List<BlockPos>) = path(nodes, complete = true)

    override fun closestOr(failure: RouteFailure): RouteResult<Path> =
        closestNodes(goal::distance)?.let(::endOnGround)?.let { path(it, complete = false) }
            ?: RouteResult.Failed(failure)

    /**
     * [nodes] up to the last one standing on ground of its own if the way there uses up every block at hand, so a
     * partial route never ends on a bridge the walker cannot go on with.
     */
    private fun endOnGround(nodes: List<BlockPos>): List<BlockPos>? {
        if (!abilities.canPlace || placed.get(nodes.last().asLong()) < abilities.placeableBlocks) {
            return nodes
        }
        val end = nodes.indexOfLast { moves.isNode(it.x, it.y, it.z) }
        return if (end >= 1) nodes.subList(0, end + 1) else null
    }

    private fun path(nodes: List<BlockPos>, complete: Boolean): RouteResult<Path> {
        val steps = ArrayList<Step>(nodes.size - 1)
        for (i in 1 until nodes.size) {
            steps += moves.step(nodes[i - 1], nodes[i]) ?: return RouteResult.Failed(RouteFailure.NO_PATH)
        }
        return RouteResult.Found(Path(start, steps, complete, abilities))
    }

    private companion object {
        /** No jump takes fewer ticks: the shortest time in the air. */
        const val MIN_JUMP_TICKS = 6.0
    }

}
