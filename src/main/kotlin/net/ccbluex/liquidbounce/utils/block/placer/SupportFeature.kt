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
package net.ccbluex.liquidbounce.utils.block.placer

import net.ccbluex.fastutil.objectHashSetOf
import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.utils.block.getState
import net.ccbluex.liquidbounce.utils.block.isBlockedByEntities
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.blockSortedSetOf
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.block.dijkstraShortestPath
import net.ccbluex.liquidbounce.utils.math.sq
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

// TODO multiple paths a tick if enough placements in none rotation mode
// TODO support no wall range, proper reach calculations
// wall range support could be done by taking the angle and, if the face is pointing to the player, exclude
// TODO cache blocked / allowed spots
/**
 * Places blocks to support placements.
 */
class SupportFeature(val placer: BlockPlacer) : ToggleableValueGroup(placer, "Support", true) {

    private val depth by int("Depth", 4, 1..12)
    val delay by int("Delay", 500, 0..1000, "ms")

    // what block helping blocks can be, by default just "trash blocks", meaning very common blocks
    val filter by enumChoice("Filter", Filter.BLACKLIST)
    val blocks by blocks("Blocks", blockSortedSetOf())

    // we don't have to consistently search, every once in a while is okay, see `delay`
    val chronometer = Chronometer()

    // positions we may not place at
    val blockedPositions = objectHashSetOf<BlockPos>()

    /**
     * Finds the shortest support path to make [targetPos] placeable via Dijkstra search.
     */
    fun findSupport(targetPos: BlockPos): Set<BlockPos>? {
        val rangeSq = placer.range.sq()
        val queuedBlocks = placer.blocks.keys

        val shortestPath = dijkstraShortestPath(
            start = targetPos,
            isGoal = ::canPlace,
            neighbors = { current ->
                buildList {
                    for (direction in Direction.entries) {
                        val neighbor = current.relative(direction)

                        if (
                            // don't place helping blocks where the structure will be
                            blockedPositions.contains(neighbor) ||

                            // exclude blocks where the structure is...
                            // this useless because we already search the shortest path under all structure blocks?
                            queuedBlocks.contains(neighbor.asLong()) ||
                            neighbor.distManhattan(targetPos) > depth ||
                            player.eyePosition.distanceToSqr(neighbor.center) > rangeSq ||
                            neighbor.isBlockedByEntities()
                        ) {
                            continue
                        }

                        add(WeightedEdge(node = neighbor, cost = 2.0))
                    }
                }
            }
        ) ?: return null

        return shortestPath.nodes.toSet()
    }

    private fun canPlace(pos: BlockPos): Boolean {
        val cache = BlockPos.MutableBlockPos()
        return Direction.entries.any {
            !cache.setWithOffset(pos, it).getState()!!.canBeReplaced()
        }
    }

}
