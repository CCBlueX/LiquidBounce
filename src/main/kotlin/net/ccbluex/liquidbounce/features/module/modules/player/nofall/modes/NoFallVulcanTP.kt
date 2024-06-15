/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.ccbluex.liquidbounce.utils.block.canStandOn
import net.ccbluex.liquidbounce.utils.entity.FallingPlayer
import net.ccbluex.liquidbounce.utils.entity.SimulatedPlayer
import net.ccbluex.liquidbounce.utils.math.toBlockPos
import net.minecraft.block.Blocks
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.util.math.BlockPos


/**
 * @anticheat Vulcan
 * @anticheatVersion 2.7.7
 * @testedOn eu.loyisa.cn
 */
internal object NoFallVulcanTP : Choice("VulcanTP288") {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    private var voidDetected = false

    private val voidThreshold by int("VoidLevel", 0, -256..0)

    @Suppress("unused")
    val movementInputHandler = handler<MovementInputEvent> {
        val simulatedPlayer = SimulatedPlayer.fromClientPlayer(
            SimulatedPlayer.SimulatedPlayerInput.fromClientPlayer(it.directionalInput)
        )

        if (player.fallDistance > 0.5 && isLikelyFalling(simulatedPlayer)) {
            val simulatedFallingPlayer = FallingPlayer.fromPlayer(player)

            if (simulatedFallingPlayer.findCollision(500) == null) {
                voidDetected = true
            }
        } else {
            // If the player is not falling, reset voidDetected to false
            voidDetected = false
        }
    }


    private fun isLikelyFalling(simulatedPlayer: SimulatedPlayer): Boolean {
        repeat(10) {
            simulatedPlayer.tick()

            if (simulatedPlayer.fallDistance > 0 && !simulatedPlayer.pos.toBlockPos().down().canStandOn()) {
                val distanceToVoid = simulatedPlayer.pos.y - voidThreshold
                val ticksToVoid = (distanceToVoid * 1.4 / 0.98).toInt()

                repeat(ticksToVoid) {
                    simulatedPlayer.tick()
                }

                return simulatedPlayer.pos.y < voidThreshold
            }
        }

        return false
    }

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet
        if (packet is PlayerMoveC2SPacket && player.fallDistance > 2.5 && player.fallDistance < 50) {
            if (!voidDetected) {
                // The Actual Bypass
                packet.onGround = true
                player.velocity.y = -99.887575
                player.velocity.x = 0.0
                player.velocity.z = 0.0
                player.input.sneaking = true
            }
        }
    }
}
