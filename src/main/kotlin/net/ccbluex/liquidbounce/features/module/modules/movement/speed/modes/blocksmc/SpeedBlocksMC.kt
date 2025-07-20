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
 *
 */
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.blocksmc

import net.ccbluex.liquidbounce.config.types.Choice
import net.ccbluex.liquidbounce.config.types.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * extensive blocksmc speed
 * @author liquidsquid1
 */

class SpeedBlocksMC(override val parent: ChoiceConfigurable<*>) : Choice("BlocksMC") {

    private var roundStrafeYaw by boolean("RoundStrafeYaw", false)

    private var fastHopMode = 0
    private var flagDelay = 0

    override fun enable() {
        fastHopMode = 0
    }

    override fun disable() {
        player.velocity = player.velocity.withStrafe(speed = 0.0)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        flagDelay--
        flagDelay = flagDelay.coerceAtLeast(0)

        var speed = 0.06 + when {
            player.isOnGround -> 0.12
            else -> 0.21
        } + player.velocity.y / 20


        if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) == 1) {
            speed += 0.1
        }

        if (flagDelay > 0) {
            repeat(flagDelay) {
                speed -= 0.007
            }
        }

        if (!player.isOnGround && fastHopMode == 3 && mc.options.jumpKey.isPressed) {
            fastHopMode = 2
        }

        when (fastHopMode) {
            0 -> {}
            1 -> {
                if (player.airTicks == 4) {
                    player.velocity.y = -0.09800000190734863
                }
            }
            2 -> {
                when (player.airTicks) {
                    1 -> player.velocity.y += 0.0568
                    3 -> player.velocity.y -= 0.13
                    4 -> player.velocity.y -= 0.2
                }
            }
            3 -> {
                if (player.isOnGround) {
                    speed = if (flagDelay > 0) 0.6 else 1.125
                    player.velocity.x *= speed
                    player.velocity.z *= speed
                }
            }
        }

        var yaw = player.getMovementDirectionOfInput(DirectionalInput(player.input))
        if (roundStrafeYaw) {
            yaw = (Math.round(yaw / 45) * 45).toFloat()
        }

        if (!player.isOnGround && fastHopMode != 0 && fastHopMode != 3) {
            player.velocity = player.velocity.withStrafe(speed = speed, yaw = yaw)
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (player.isOnGround) {
            if (fastHopMode == 3) {
                if (player.age % 2 == 0) {
                    fastHopMode = 1
                    if (ModuleScaffold.enabled) {
                        fastHopMode++
                        if (!mc.options.jumpKey.isPressed) {
                            fastHopMode++
                        }
                    }
                    if (fastHopMode < 3) {
                        player.velocity.x *= 0.6
                        player.velocity.z *= 0.6
                    }
                }
            } else {
                fastHopMode = 1
                if (ModuleScaffold.enabled) {
                    fastHopMode++
                    if (!mc.options.jumpKey.isPressed) {
                        fastHopMode++
                    }
                }
            }
        }
        if (event.directionalInput.isMoving && fastHopMode != 0 && fastHopMode != 3) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        when (fastHopMode) {
            3 -> {
                if (packet is PlayerMoveC2SPacket && player.isOnGround && player.age % 2 == 0) {
                    packet.y += 0.03
                }
            }
        }

        if (packet is PlayerPositionLookS2CPacket) {
            flagDelay = 20
        }
    }

    @Suppress("unused")
    private val jumpHandler = handler<PlayerJumpEvent> {
        when (fastHopMode) {
            3 -> {
                if (player.isOnGround && player.age % 2 == 1) {
                    it.cancelEvent()
                }
            }
        }
    }

}
