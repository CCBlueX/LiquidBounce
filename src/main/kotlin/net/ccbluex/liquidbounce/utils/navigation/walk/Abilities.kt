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

import it.unimi.dsi.fastutil.longs.LongSet
import net.ccbluex.liquidbounce.utils.entity.MotionTraits
import net.minecraft.world.level.block.state.BlockState
import java.util.function.ToDoubleFunction

/** What the walker may do, captured on the render thread for one search. Falls are in blocks, [air] in ticks. */
class Abilities(
    val canBreak: Boolean,
    val canPlace: Boolean,
    val placeableBlocks: Int,
    val canParkour: Boolean,
    val riskyJumps: Boolean = false,
    val canSprint: Boolean,
    val safeFall: Int,
    val waterFall: Int,

    /** Ticks to mine a block with the best tool at hand; infinite when it must not or cannot be mined. */
    val breakTicks: ToDoubleFunction<BlockState>,

    /** Feet blocks the walker got stuck at; moves into them cost extra. */
    val avoid: LongSet,
    val air: Int = Water.MAX_AIR,
    val motion: MotionTraits = MotionTraits.DEFAULT,

    /** Whether holding jump climbs a ladder or vine; before 1.14 only pushing against a block beside it does. */
    val jumpClimbs: Boolean = true,

    /** Extra ticks per floor block a level walk places; few where another feature runs bridges across. */
    val bridgeTicks: Double = Costs.PLACE,
)
