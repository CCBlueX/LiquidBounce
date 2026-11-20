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

import it.unimi.dsi.fastutil.longs.Long2BooleanOpenHashMap
import net.ccbluex.liquidbounce.utils.block.WeightedEdge
import net.ccbluex.liquidbounce.utils.navigation.route.Goal
import net.ccbluex.liquidbounce.utils.navigation.terrain.Terrain
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.phys.Vec3
import kotlin.math.abs
import kotlin.math.max

/** The blocks a flying body fits in over [terrain] and the ticks it takes from one to the 26 around it. */
internal class Moves(
    private val terrain: Terrain,
    private val abilities: Abilities,
    private val goal: Goal,
) {

    private val open = Long2BooleanOpenHashMap()

    fun isOpen(x: Int, y: Int, z: Int): Boolean {
        val key = BlockPos.asLong(x, y, z)
        if (open.containsKey(key)) {
            return open.get(key)
        }

        val isOpen = terrain.isKnown(x, y, z) && terrain.isKnown(x, y + 1, z) &&
            terrain.isClear(terrain.box(x + 0.5, y.toDouble(), z + 0.5))
        open.put(key, isOpen)
        return isOpen
    }

    /** Where the feet are in [node]: resting on the ground there, else at the bottom of the block. */
    fun point(node: BlockPos): Vec3 = terrain.feet(node) ?: Vec3.atBottomCenterOf(node)

    fun neighbors(node: BlockPos): List<WeightedEdge<BlockPos>> {
        val edges = ArrayList<WeightedEdge<BlockPos>>(NEIGHBORS)
        for (dx in -1..1) {
            for (dy in -1..1) {
                for (dz in -1..1) {
                    if (dx == 0 && dy == 0 && dz == 0 || !canMove(node, dx, dy, dz)) {
                        continue
                    }

                    val to = node.offset(dx, dy, dz)
                    edges += WeightedEdge(to, cost(to, dx, dy, dz))
                }
            }
        }
        return edges
    }

    // The body has to fit in every block a diagonal move passes, so no corner is cut.
    private fun canMove(node: BlockPos, dx: Int, dy: Int, dz: Int): Boolean {
        for (x in minOf(0, dx)..maxOf(0, dx)) {
            for (y in minOf(0, dy)..maxOf(0, dy)) {
                for (z in minOf(0, dz)..maxOf(0, dz)) {
                    if (!isOpen(node.x + x, node.y + y, node.z + z)) {
                        return false
                    }
                }
            }
        }
        return true
    }

    private fun cost(to: BlockPos, dx: Int, dy: Int, dz: Int): Double {
        val horizontal = Mth.length(dx.toDouble(), dz.toDouble()) * abilities.horizontalCost
        var cost = max(horizontal, abs(dy) * abilities.verticalCost)
        if (terrain.isWater(to.x, to.y, to.z) || terrain.isWater(to.x, to.y + 1, to.z)) {
            cost *= WATER_FACTOR
        }
        // Touching the ground ends creative flight.
        if (abilities.control == Control.MOMENTUM && !goal.isGoal(to) &&
            !terrain.standHeight(to.x, to.y, to.z).isNaN()) {
            cost += GROUND_PENALTY
        }
        return cost
    }

    private companion object {
        const val NEIGHBORS = 26
        const val WATER_FACTOR = 4.0
        const val GROUND_PENALTY = 1.0
    }

}
