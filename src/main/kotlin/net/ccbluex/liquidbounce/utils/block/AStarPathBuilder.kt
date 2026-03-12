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

package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.core.Vec3i
import net.minecraft.world.phys.AABB

private val directions = buildList(22) {
    add(Vec3i(-1, 0, 0)) // left
    add(Vec3i(1, 0, 0)) // right
    (-9..-1).mapTo(this) { Vec3i(0, it, 0) } // down
    (1..9).mapTo(this) { Vec3i(0, it, 0) } // up
    add(Vec3i(0, 0, -1)) // front
    add(Vec3i(0, 0, 1)) // back
}

private val diagonalDirections = arrayOf(
    Vec3i(-1, 0, -1), // left front
    Vec3i(1, 0, -1), // right front
    Vec3i(-1, 0, 1), // left back
    Vec3i(1, 0, 1) // right back
)

interface AStarPathBuilder {

    val allowDiagonal: Boolean

    val maxIterations: Int get() = 500

    val stopRange: Double get() = 2.0

    val Vec3i.isPassable: Boolean
        get() {
            val box = AABB(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 2.0, z + 1.0)

            val collisions = world.getBlockCollisions(player, box)

            return collisions.none()
        }

    fun findPath(start: Vec3i, end: Vec3i, maxCost: Int): List<Vec3i> {
        if (end.closerThan(start, stopRange)) return emptyList()

        val shortestPath = aStarShortestPath(
            start = start,
            isGoal = { it.closerThan(end, stopRange) },
            neighbors = ::getAdjacentEdges,
            heuristic = { (it.distSqr(end)) },
            maxIterations = maxIterations,
            maxCost = maxCost.toDouble(),
        ) ?: return emptyList()

        // Exclude start node to preserve the original API contract.
        return shortestPath.nodes.drop(1)
    }

    private fun getAdjacentEdges(position: Vec3i): List<WeightedEdge<Vec3i>> = buildList {
        getAdjacentNodesDirect(position)
        if (allowDiagonal) {
            getAdjacentNodesDiagonal(position)
        }
    }

    private fun MutableList<WeightedEdge<Vec3i>>.getAdjacentNodesDirect(position: Vec3i) {
        val pos = BlockPos.MutableBlockPos()
        for (direction in directions) {
            val adjacentPosition = pos.setWithOffset(position, direction)
            if (adjacentPosition.isPassable) {
                val adjacentImmutable = adjacentPosition.immutable()
                add(WeightedEdge(adjacentImmutable, (position.distSqr(adjacentImmutable))))
            }
        }
    }

    private fun MutableList<WeightedEdge<Vec3i>>.getAdjacentNodesDiagonal(position: Vec3i) {
        val pos = BlockPos.MutableBlockPos()
        for (direction in diagonalDirections) {
            val adjacentPosition = pos.setWithOffset(position, direction)
            if (adjacentPosition.isPassable &&
                position.offset(direction.x, 0, 0).isPassable &&
                position.offset(0, 0, direction.z).isPassable
            ) {
                val adjacentImmutable = adjacentPosition.immutable()
                add(WeightedEdge(adjacentImmutable, (position.distSqr(adjacentImmutable))))
            }
        }
    }
}
