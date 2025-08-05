/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.utils.collection.ObjectPool
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.util.math.Box
import net.minecraft.util.math.Vec3i
import java.util.Comparator
import java.util.TreeSet

private class Node(val position: Vec3i, var parent: Node? = null) {
    var g = 0
    var h = 0
    var f = 0

    override fun hashCode(): Int = position.hashCode()
    override fun equals(other: Any?): Boolean = other is Node && other.position == this.position

    fun buildPath(): List<Vec3i> {
        val path = mutableListOf<Vec3i>()
        var currentNode = this
        while (currentNode.parent != null) {
            path.add(currentNode.position)
            currentNode = currentNode.parent!!
        }
        path.reverse()
        return path
    }
}

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

private val NODE_COMPARATOR = Comparator.comparingInt<Node> { it.f }.thenComparing { it.position }

interface AStarPathBuilder {

    val allowDiagonal: Boolean

    val maxIterations: Int get() = 500

    val stopRange: Double get() = 2.0

    val Vec3i.isPassable: Boolean
        get() {
            val box = Box(x.toDouble(), y.toDouble(), z.toDouble(), x + 1.0, y + 2.0, z + 1.0)

            val collisions = world.getBlockCollisions(player, box)

            return collisions.none()
        }

    infix fun Vec3i.costWith(that: Vec3i): Int =
        (this.x - that.x).sq() + (this.y - that.y).sq() + (this.z - that.z).sq()

    fun findPath(start: Vec3i, end: Vec3i, maxCost: Int): List<Vec3i> {
        if (end.isWithinDistance(start, stopRange)) return emptyList()

        val startNode = Node(start)
        val endNode = Node(end)

        // Node::f won't be modified after added
        val openList = TreeSet(NODE_COMPARATOR).apply { add(startNode) }
        val closedList = hashSetOf<Node>()

        var iterations = 0
        while (openList.isNotEmpty()) {
            iterations++
            if (iterations > maxIterations) {
                break
            }

            val currentNode = openList.removeFirst()
            closedList.add(currentNode)

            if (currentNode.position.isWithinDistance(endNode.position, stopRange)) {
                return currentNode.buildPath()
            }

            for (node in getAdjacentNodes(currentNode)) {
                if (node in closedList || !node.position.isPassable) continue

                val tentativeG = currentNode.g + currentNode.position.costWith(node.position)
                if (tentativeG < node.g || node !in openList) {
                    if (tentativeG > maxCost) continue // Skip this node if the cost exceeds the maximum

                    node.parent = currentNode
                    node.g = tentativeG
                    node.h = node.position costWith endNode.position
                    node.f = node.g + node.h

                    openList.add(node)
                }
            }
        }

        return emptyList() // Return an empty list if no path was found
    }

    private fun getAdjacentNodes(node: Node): List<Node> = buildList {
        getAdjacentNodesDirect(node)
        if (allowDiagonal) {
            getAdjacentNodesDiagonal(node)
        }
    }

    private fun MutableList<Node>.getAdjacentNodesDirect(node: Node) {
        ObjectPool.MutableBlockPos.use { pos ->
            for (direction in directions) {
                val adjacentPosition = pos.set(node.position, direction)
                if (adjacentPosition.isPassable) {
                    add(Node(adjacentPosition.toImmutable(), node))
                }
            }
        }
    }

    private fun MutableList<Node>.getAdjacentNodesDiagonal(node: Node) {
        ObjectPool.MutableBlockPos.use { pos ->
            for (direction in diagonalDirections) {
                val adjacentPosition = pos.set(node.position, direction)
                if (adjacentPosition.isPassable &&
                        node.position.add(direction.x, 0, 0).isPassable &&
                        node.position.add(0, 0, direction.z).isPassable) {
                    add(Node(adjacentPosition.toImmutable(), node))
                }
            }
        }
    }
}
