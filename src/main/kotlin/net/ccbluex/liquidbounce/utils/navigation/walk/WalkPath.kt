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
import net.minecraft.core.BlockPos
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SlimeBlock
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3

enum class WalkMove {
    /** One block sideways on the same level. */
    TRAVERSE,

    /** One block diagonally on the same level. */
    DIAGONAL,

    /** Jump one block up while moving one block sideways. */
    ASCEND,

    /** Walk off an edge one block sideways and land lower. */
    FALL,

    /** Sprint-jump over a gap. */
    PARKOUR,

    /** Jump and place a block below the feet. */
    PILLAR,

    /** Break the block below the feet and drop into its place. */
    DOWNWARD,

    /** Move through water. */
    SWIM,

    CLIMB_UP,
    CLIMB_DOWN,
}

/**
 * One move of a walking path, from the feet block [from] to the feet block [to].
 */
@Suppress("LongParameterList")
class WalkStep(
    val from: BlockPos,
    val to: BlockPos,
    val move: WalkMove,

    /**
     * Where the feet rest after the move.
     */
    val feet: Vec3,

    /**
     * Blocks to break before the move, in order.
     */
    val breaks: List<BlockPos>,

    /**
     * Blocks to place before or during the move, in order; each is the position the new block takes.
     */
    val places: List<BlockPos>,

    /**
     * Closed doors, trapdoors and fence gates to open before the move; a door by its lower half.
     */
    val opens: List<BlockPos>,

    /**
     * Estimated duration in ticks.
     */
    val cost: Double,

    /**
     * Whether the move is made sneaking: through a gap only a crouching player fits, or past a drop the player must
     * not fall down.
     */
    val sneak: Boolean = false,
)

/**
 * A walking path from [start]. An incomplete path ends at the reachable spot closest to the goal.
 */
class WalkPath(
    val start: Vec3,
    val steps: List<WalkStep>,
    val complete: Boolean,
) {

    val end: Vec3
        get() = steps.lastOrNull()?.feet ?: start

    /**
     * Feet positions of the path, the start included.
     */
    val points: List<Vec3> by lazy { listOf(start) + steps.map(WalkStep::feet) }

    val cost: Double
        get() = steps.sumOf(WalkStep::cost)

}

/**
 * What the walker may do, captured on the render thread for one search.
 */
class WalkAbilities(
    val canBreak: Boolean,
    val canPlace: Boolean,

    /**
     * Blocks in the hotbar the walker may place.
     */
    val placeableBlocks: Int,
    val canParkour: Boolean,
    val canSprint: Boolean,

    /**
     * Highest drop onto ground, in blocks.
     */
    val safeFall: Int,

    /**
     * Highest drop into water, in blocks.
     */
    val waterFall: Int,

    /**
     * Ticks to mine a block with the best tool at hand; infinite when it must not or cannot be mined.
     */
    val breakTicks: (BlockState) -> Double,

    /**
     * Feet blocks the walker got stuck at; moves into them cost extra.
     */
    val avoid: LongSet,
)

/**
 * Durations in ticks, after Baritone's action costs.
 */
object WalkCosts {
    const val WALK = 20.0 / 4.317
    const val SPRINT = 20.0 / 5.612
    const val SWIM = 20.0 / 2.2
    const val CLIMB_UP = 20.0 / 2.35
    const val CLIMB_DOWN = 20.0 / 3.0
    const val SNEAK = 20.0 / 1.295
    const val PLACE = 20.0
    const val BREAK_OVERHEAD = 2.0
    const val AVOID = 50.0

    /**
     * Ticks to stop, open a door, trapdoor or fence gate and walk on.
     */
    const val OPEN = 10.0

    /**
     * Up and down bounces after landing on a slime block.
     */
    private const val BOUNCES = 2
    private const val PLAIN_FRICTION = 0.6f
    private const val SLIME_SLOWDOWN = 0.4
    private const val GROUND_DRAG = 0.91
    private const val GROUND_ACCELERATION = 0.216

    private val fallTicks: DoubleArray = DoubleArray(257).also { ticks ->
        var distance = 0.0
        var velocity = 0.0
        var tick = 0
        for (blocks in ticks.indices) {
            while (distance < blocks) {
                velocity = (velocity + 0.08) * 0.98
                distance += velocity
                tick++
            }
            ticks[blocks] = tick.toDouble()
        }
    }

    /**
     * Ticks to fall [blocks] blocks from rest.
     */
    fun fall(blocks: Int) = fallTicks[blocks.coerceIn(0, fallTicks.lastIndex)]

    /**
     * Extra ticks of a jump compared to walking.
     */
    val JUMP = fall(2) - fall(1)

    /**
     * How many times longer walking over a floor of [block] takes than over plain ground: soul sand, honey and
     * slime slow the player down. Slippery floors such as ice reach the same top speed, so they count as plain.
     */
    fun floorFactor(block: Block): Double {
        var slowdown = block.speedFactor.toDouble()
        if (block is SlimeBlock) {
            slowdown *= SLIME_SLOWDOWN
        }

        return if (slowdown < 1.0) {
            groundSpeed(PLAIN_FRICTION, 1.0) / groundSpeed(block.friction, slowdown)
        } else {
            1.0
        }
    }

    /**
     * Extra ticks of bouncing after a drop of [blocks] onto [block].
     */
    fun bounce(block: Block, blocks: Int) = if (block is SlimeBlock) 2 * BOUNCES * fall(blocks) else 0.0

    /**
     * Top ground speed relative to the movement speed on a floor of [friction] that scales the speed by [slowdown]
     * every tick, after LivingEntity.travel: the speed gained per tick over the share lost.
     */
    private fun groundSpeed(friction: Float, slowdown: Double): Double {
        val drag = friction * GROUND_DRAG
        return GROUND_ACCELERATION / (drag * drag * drag) / (1.0 - slowdown * drag)
    }
}
