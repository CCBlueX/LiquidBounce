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

import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3

enum class Move {
    TRAVERSE,
    DIAGONAL,
    ASCEND,
    FALL,
    PARKOUR,
    PILLAR,
    DOWNWARD,
    SWIM,
    CLIMB_UP,
    CLIMB_DOWN,
}

class Step(
    val from: BlockPos,
    val to: BlockPos,
    val move: Move,
    val feet: Vec3,
    val breaks: List<BlockPos>,
    val places: List<BlockPos>,

    /** Closed doors, trapdoors and fence gates to open before the move; a door by its lower half. */
    val opens: List<BlockPos>,
    val cost: Double,

    /** Through a gap only a crouching player fits. */
    val sneak: Boolean = false,

    /** Level past a deadly drop: walked with safe walk, which keeps to the edge as sneaking does but at full speed. */
    val edge: Boolean = false,
    val underwater: Boolean = false,
    val jump: Jump? = null,
) {
    /** A level walk onto a floor placed on the way, which another feature such as Scaffold can place instead. */
    val isBridge: Boolean
        get() = move == Move.TRAVERSE && to.y == from.y && breaks.isEmpty() && opens.isEmpty() && jump == null &&
            places.size == 1 && places[0] == to.below()
}

/** A walking path; an incomplete one ends at the reachable spot closest to the goal. */
class Path(
    val start: Vec3,
    val steps: List<Step>,
    val complete: Boolean,
    val abilities: Abilities,
) {

    val end: Vec3
        get() = steps.lastOrNull()?.feet ?: start

    val points: List<Vec3> by lazy { listOf(start) + steps.map(Step::feet) }

    val cost: Double
        get() = steps.sumOf(Step::cost)

}
