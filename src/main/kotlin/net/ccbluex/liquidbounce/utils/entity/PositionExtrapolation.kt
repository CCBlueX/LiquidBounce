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

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import kotlin.math.max
import kotlin.math.round

/**
 * A utility which predicts the position of something in n ticks.
 */
fun interface PositionExtrapolation {
    fun getPositionInTicks(ticks: Double): Vec3

    companion object {
        @JvmStatic
        fun getBestForEntity(target: Entity): PositionExtrapolation {
            return when (target) {
                is Player -> PlayerSimulationExtrapolation(target)
                else -> LinearPositionExtrapolation(target)
            }
        }

        @JvmStatic
        fun constant(pos: Vec3): PositionExtrapolation = PositionExtrapolation { pos }
    }
}

/**
 * A utility class which assumes that the subject is moving at a specified speed.
 */
class LinearPositionExtrapolation(
    private val basePosition: Vec3,
    private val velocity: Vec3
) : PositionExtrapolation {
    constructor(entity: Entity) : this(entity.position(), entity.position() - entity.lastPos)

    override fun getPositionInTicks(ticks: Double): Vec3 {
        return basePosition + velocity * ticks
    }

}

class PlayerSimulationExtrapolation(private val simulation: SimulatedPlayerCache) : PositionExtrapolation {
    constructor(player: Player) : this(PlayerSimulationCache.getSimulationForOtherPlayers(player))

    override fun getPositionInTicks(ticks: Double): Vec3 {
        val ticks = max(0, round(ticks.coerceAtMost(30.0)).toInt())
        return this.simulation.getSnapshotAt(ticks).pos
    }
}
