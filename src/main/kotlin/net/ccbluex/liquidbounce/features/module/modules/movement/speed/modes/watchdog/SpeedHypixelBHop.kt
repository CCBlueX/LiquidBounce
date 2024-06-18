/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.ModuleSpeed
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.sqrtSpeed
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 12.12.2023
 * @testedOn hypixel.net
 */
object SpeedHypixelBHop : Choice("HypixelBHop") {

    override val parent: ChoiceConfigurable<Choice>
        get() = ModuleSpeed.modes

    private val horizontalAcceleration by float("HorizontalAcceleration", 0.00F, 0.00F..1.00F)
    private val verticalAcceleration by float("VerticalAcceleration", 0.01F, 0.00F..1.00F)
    private val strafe by float("Strafe", 0.7F, 0.0F..1.0F)
    /**
     * Vanilla maximum speed
     * w/o: 0.2857671997172534
     * w/ Speed 1: 0.2919055664000211
     * w/ Speed 2: 0.2999088445964323
     *
     * Speed mod: 0.008003278196411223
     */

    private const val BASE_ACCELERATION = 0.0004

    private const val AT_LEAST = 0.281
    private const val BASH = 0.2857671997172534
    private const val SPEED_EFFECT_CONST = 0.008003278196411223

    private var wasFlagged = false

    val repeatable = repeatable {
        if (player.isOnGround) {
            // Strafe when on ground
            player.strafe()
            return@repeatable
        } else {
            // Not much speed boost, but still a little bit - if someone wants to improve this, feel free to do so
            val horizontalMod = if (horizontalAcceleration > 0) {
                BASE_ACCELERATION + horizontalAcceleration *
                    (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
            } else {
                0.0
            }

            // Vertical acceleration, this makes sense to get a little bit more speed again
            val yMod = if (verticalAcceleration > 0 && player.velocity.y < 0 && player.fallDistance < 1) {
                verticalAcceleration.toDouble()
            } else {
                0.0
            }

            player.velocity = player.velocity.multiply(
                1.0 + horizontalMod,
                1.0 + yMod,
                1.0 + horizontalMod
            )

            if (strafe > 0 &&  player.velocity.y < 0 && player.fallDistance < 1) {
                player.strafe(strength = strafe.toDouble())
            }
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> {
        val atLeast = if (!wasFlagged) {
            AT_LEAST + SPEED_EFFECT_CONST * (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0)
        } else {
            0.0
        }

        player.strafe(speed = player.sqrtSpeed.coerceAtLeast(atLeast))
    }

    val moveHandler = handler<MovementInputEvent> {
        if (!player.isOnGround || !player.moving) {
            return@handler
        }

        if (ModuleSpeed.shouldDelayJump())
            return@handler

        it.jumping = true
    }

    val packetHandler = sequenceHandler<PacketEvent> {
        val packet = it.packet

        if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
            val velocityX = packet.velocityX / 8000.0
            val velocityY = packet.velocityY / 8000.0
            val velocityZ = packet.velocityZ / 8000.0

            waitTicks(1)

            // Fall damage velocity
            val speed = if (velocityX == 0.0 && velocityZ == 0.0 && velocityY == -0.078375) {
                player.sqrtSpeed.coerceAtLeast(
                    BASH *
                        (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0))
            } else {
                player.sqrtSpeed
            }
            player.strafe(speed = speed)
        } else if (packet is PlayerPositionLookS2CPacket) {
            wasFlagged = true
        }
    }

    override fun disable() {
        wasFlagged = false
    }

}
