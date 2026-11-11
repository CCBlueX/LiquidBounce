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

import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer.SimulatedPlayerInput
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.core.BlockPos
import net.minecraft.world.phys.Vec3
import kotlin.jvm.optionals.getOrNull

/**
 * Follows the player with its current movement input until it lands.
 */
@Suppress("LongParameterList")
class FallingPlayer(
    private val player: LocalPlayer,
    private val x: Double,
    private val y: Double,
    private val z: Double,
    private val motionX: Double,
    private val motionY: Double,
    private val motionZ: Double,
    private val yRot: Float
) {
    companion object {
        @JvmStatic
        @JvmOverloads
        fun fromPlayer(player: LocalPlayer, movementYaw: Float = player.yRot): FallingPlayer {
            return FallingPlayer(
                player,
                player.x,
                player.y,
                player.z,
                player.deltaMovement.x,
                player.deltaMovement.y,
                player.deltaMovement.z,
                movementYaw
            )
        }
    }

    private fun simulation(descending: Boolean): SimulatedPlayer {
        val input = SimulatedPlayerInput.fromClientPlayer(
            DirectionalInput(player.input),
            jump = false,
            sneaking = descending || player.isShiftKeyDown,
        )

        return SimulatedPlayer.fromClientPlayer(input).apply {
            setPos(x, y, z)
            deltaMovement = Vec3(motionX, motionY, motionZ)
            yRot = this@FallingPlayer.yRot
        }
    }

    fun findCollision(ticks: Int): CollisionResult? {
        val simulation = simulation(descending = false)

        for (tick in 0 until ticks) {
            val positionBeforeMovement = simulation.pos
            simulation.tick()

            if (simulation.onGround()) {
                return CollisionResult(simulation.mainSupportingBlockPos.getOrNull(), tick, positionBeforeMovement)
            }
        }

        return null
    }

    /**
     * Checks whether a future movement tick starts with the player's feet in [targetPos] before landing.
     */
    fun willStartTickInBlockBeforeCollision(
        targetPos: BlockPos,
        ticks: Int,
        forceDescending: Boolean = false,
    ): Boolean {
        val simulation = simulation(forceDescending)

        repeat(ticks) {
            if (simulation.blockPosition() == targetPos) {
                return true
            }

            simulation.tick()
            if (simulation.onGround()) {
                return false
            }
        }

        return false
    }

    /**
     * [positionBeforeMovement] is where the player uses an item from before the movement step of [tick].
     */
    class CollisionResult(
        val pos: BlockPos?,
        val tick: Int,
        val positionBeforeMovement: Vec3,
    )
}
