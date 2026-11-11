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

import net.minecraft.world.entity.LivingEntity.BASE_HORIZONTAL_AIR_DRAG
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SlimeBlock

/** Durations in ticks, after Baritone's action costs. */
object Costs {
    const val WALK = 20.0 / 4.317
    const val SPRINT = 20.0 / 5.612
    const val SWIM = 20.0 / 2.2
    const val CLIMB_UP = 20.0 / 2.35
    const val CLIMB_DOWN = 20.0 / 3.0
    const val SNEAK = 20.0 / 1.295
    const val SPRINT_SWIM = 20.0 / 3.918
    const val PLACE = 20.0
    const val BREAK_OVERHEAD = 2.0
    const val AVOID = 50.0
    const val JUMP_PENALTY = 20.0
    const val OPEN = 10.0

    private const val SLIME_BOUNCES = 2
    private const val SLIME_SLOWDOWN = 0.4
    private const val GROUND_ACCELERATION = 0.216

    private val fallTicks = DoubleArray(257).also { ticks ->
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

    fun fall(blocks: Int) = fallTicks[blocks.coerceIn(0, fallTicks.lastIndex)]

    val JUMP = fall(2) - fall(1)

    private val plainSpeed = groundSpeed(PlayerMotion.PLAIN_FRICTION, slowdown = 1.0)

    /** How many times longer walking over [block] takes than over plain ground. Ice gets as fast, so it is plain. */
    fun floorFactor(block: Block): Double {
        val slowdown = block.speedFactor * if (block is SlimeBlock) SLIME_SLOWDOWN else 1.0
        return if (slowdown < 1.0) plainSpeed / groundSpeed(block.friction, slowdown) else 1.0
    }

    fun bounce(block: Block, blocks: Int) = if (block is SlimeBlock) 2 * SLIME_BOUNCES * fall(blocks) else 0.0

    /** Top ground speed, relative to the movement speed, on a floor that scales the speed by [slowdown] every tick. */
    private fun groundSpeed(friction: Float, slowdown: Double): Double {
        val drag = (friction * BASE_HORIZONTAL_AIR_DRAG).toDouble()
        return GROUND_ACCELERATION / (drag * drag * drag) / (1.0 - slowdown * drag)
    }
}
