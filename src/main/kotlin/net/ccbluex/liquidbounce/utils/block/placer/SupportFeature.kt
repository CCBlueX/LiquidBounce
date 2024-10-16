/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.block.manhattanDistanceTo
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.block.Blocks
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction
import java.util.*
import kotlin.Comparator

// TODO support no wall range, proper reach calculations
// wall range support could be done by taking the angle and, if the face is pointing to the player, exclude
// TODO cache blocked / allowed spots
/**
 * Places blocks to support placements.
 */
class SupportFeature(val placer: BlockPlacer) : ToggleableConfigurable(placer, "Support", true) {

    private val depth by int("Depth", 4, 1..12)
    val delay by int("Delay", 500, 0..1000)

    // what block helping blocks can be, by default just "trash blocks", meaning very common blocks
    val filter by enumChoice("Filter", Filter.WHITELIST)
    val blocks by blocks("Blocks", mutableSetOf(Blocks.COBBLESTONE, Blocks.DIRT, Blocks.NETHERRACK))

    // we don't have to consistently search, every once in a while is okay, see `delay`
    val chronometer = Chronometer()

    // positions we may not place at
    val blockedPositions = mutableSetOf<BlockPos>()

    /**
     * Currently finds the best path of blocks to support the placement of [targetPos] using Dijkstra's algorithm,
     * the speed can possibly be improved by adding heuristics and making it an A* algorithm.
     */
    fun findSupport(targetPos: BlockPos): Set<BlockPos>? {
        val rangeSq = placer.range.sq()

        val openList = PriorityQueue<Node>(Comparator.comparingDouble { it.totalCost })
        val closedList = mutableSetOf<BlockPos>()

        val startNode = Node(targetPos, null, 0.0)
        openList.add(startNode)

        while (!openList.isEmpty()) {
            val currentNode = openList.poll()
            closedList.add(currentNode.position)

            // found a possible path
            if (canPlace(currentNode.position)) {
                return reconstructPath(currentNode)
            }

            for (direction in Direction.entries) {
                val neighbor = currentNode.position.offset(direction)

                // skip visited nodes
                if (closedList.contains(neighbor)) {
                    continue
                }

                if (
                    // don't place helping blocks where the structure will be
                    blockedPositions.contains(neighbor) ||

                    // exclude blocks where the structure is...
                    // this useless because we already search the shortest path under all structure blocks?
                    placer.blocks.contains(neighbor) ||
                    neighbor.manhattanDistanceTo(targetPos) > depth ||
                    player.eyePos.squaredDistanceTo(neighbor.toCenterPos()) > rangeSq ||
                    neighbor.isBlockedByEntities()
                    ) {
                    closedList.add(neighbor)
                    continue
                }

                val totalCost = currentNode.totalCost + 2.0 // the current total cost and the move cost of two
                val neighborNode = Node(neighbor, currentNode, totalCost)

                if (!openList.contains(neighborNode) || totalCost < neighborNode.totalCost) {
                    neighborNode.totalCost = totalCost
                    neighborNode.parent = currentNode

                    if (!openList.contains(neighborNode)) {
                        openList.add(neighborNode)
                    }
                }
            }
        }

        // no path found
        return null
    }

    private fun reconstructPath(currentNode: Node): Set<BlockPos> {
        var node: Node? = currentNode
        val path = mutableSetOf<BlockPos>()
        while (node != null) {
            path.add(node.position)
            node = node.parent
        }
        return path
    }

    private fun canPlace(pos: BlockPos): Boolean {
        for (direction in Direction.entries) {
            if (!pos.offset(direction).getState()!!.isReplaceable) {
                return true
            }
        }

        return false
    }

}

private data class Node(val position: BlockPos, var parent: Node? = null, var totalCost: Double)
