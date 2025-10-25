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
 *
 */

package net.ccbluex.liquidbounce.utils.entity

import net.ccbluex.liquidbounce.utils.math.minus
import net.ccbluex.liquidbounce.utils.math.plus
import net.ccbluex.liquidbounce.utils.math.times
import net.minecraft.entity.LivingEntity
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.util.math.Vec3d
import kotlin.math.max
import kotlin.math.round

/**
 * A utility which predicts the position of something in n ticks.
 */
interface PositionExtrapolation {
    fun getPositionInTicks(ticks: Double): Vec3d

    companion object {
        fun getBestForEntity(target: LivingEntity): PositionExtrapolation {
            return when (target) {
                is PlayerEntity -> PlayerSimulationExtrapolation(target)
                else -> LinearPositionExtrapolation(target)
            }
        }
    }
}

class ConstantPositionExtrapolation(private val pos: Vec3d) : PositionExtrapolation {
    override fun getPositionInTicks(ticks: Double): Vec3d {
        return pos
    }

}

/**
 * A utility class which assumes that the subject is moving at a specified speed.
 */
class LinearPositionExtrapolation(
    private val basePosition: Vec3d,
    private val velocity: Vec3d
) : PositionExtrapolation {
    constructor(entity: LivingEntity) : this(entity.pos, entity.pos - entity.prevPos)

    override fun getPositionInTicks(ticks: Double): Vec3d {
        return basePosition + velocity * ticks
    }

}

class PlayerSimulationExtrapolation(private val simulation: SimulatedPlayerCache) : PositionExtrapolation {
    constructor(player: PlayerEntity) : this(PlayerSimulationCache.getSimulationForOtherPlayers(player))

    override fun getPositionInTicks(ticks: Double): Vec3d {
        val ticks = max(0, round(ticks.coerceAtMost(30.0)).toInt())
        return this.simulation.getSnapshotAt(ticks).pos
    }
}
