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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed

import net.ccbluex.agent.simulation.SimulatedInput
import net.ccbluex.agent.simulation.SimulatedPlayer
import net.ccbluex.agent.vanilla.toAgent
import net.ccbluex.agent.world.Input
import net.ccbluex.liquidbounce.features.module.MinecraftShortcuts
import net.ccbluex.liquidbounce.utils.entity.fromClientPlayer
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.client.player.LocalPlayer
import net.minecraft.world.phys.Vec3

object SpeedPreventDeadlyJump : MinecraftShortcuts {

    fun wouldJumpToDeath(maxFallDistance: Double = 10.0): Boolean {
        val simulatedPlayer = createSimulatedPlayer(player)

        simulatedPlayer.jumpFromGround()

        var groundPos: Vec3? = null

        for (ignored in 0..40) {
            simulatedPlayer.tick()

            if (simulatedPlayer.onGround()) {
                groundPos = simulatedPlayer.position()

                break
            }
        }

        if (groundPos == null) {
            return true
        }

        simulatedPlayer.input = SimulatedInput(Input.EMPTY)

        return wouldFallToDeath(simulatedPlayer, ticksToWaitForFall = 5, maxFallDistance = maxFallDistance)
    }

    private fun createSimulatedPlayer(player: LocalPlayer): SimulatedPlayer {
        val input = SimulatedInput(DirectionalInput(player.input).toInput(sprint = true).toAgent())

        return SimulatedPlayer.fromClientPlayer(input)
    }

    private fun wouldFallToDeath(
        simulatedPlayer: SimulatedPlayer,
        ticksToWaitForFall: Int = 5,
        maxFallDistance: Double = 10.0
    ): Boolean {
        var groundPos: Vec3? = null

        for (ignored in 0 until ticksToWaitForFall) {
            simulatedPlayer.tick()
        }

        for (ignored in 0..40) {
            simulatedPlayer.tick()

            if (simulatedPlayer.onGround()) {
                groundPos = simulatedPlayer.position()

                break
            }
        }

        if (groundPos != null) {
            simulatedPlayer.input = SimulatedInput(Input.EMPTY)

            for (ignored in 0..40) {
                simulatedPlayer.tick()

                groundPos = if (simulatedPlayer.onGround()) {
                    simulatedPlayer.position()
                } else {
                    null
                }
            }
        }

        return groundPos == null || player.y - groundPos.y > maxFallDistance
    }

}
