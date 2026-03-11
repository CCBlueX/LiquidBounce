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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.watchdog

import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.horizontalSpeed
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.CRITICAL_MODIFICATION
import net.ccbluex.liquidbounce.utils.network.isMovementYFallDamage
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.world.effect.MobEffects

/**
 * @anticheat Watchdog (NCP)
 * @anticheatVersion 12.12.2023
 * @testedOn hypixel.net
 */
class SpeedHypixelBHop(parent: ModeValueGroup<*>) : SpeedBHopBase("HypixelBHop", parent) {

    private val horizontalAcceleration by boolean("HorizontalAcceleration", true)
    private val verticalAcceleration by boolean("VerticalAcceleration", true)

    companion object {

        private const val BASE_HORIZONTAL_MODIFIER = 0.0004

        private const val HORIZONTAL_SPEED_AMPLIFIER = 0.0007
        private const val VERTICAL_SPEED_AMPLIFIER = 0.0004

        /**
         * Vanilla maximum speed
         * w/o: 0.2857671997172534
         * w/ Speed 1: 0.2919055664000211
         * w/ Speed 2: 0.2999088445964323
         *
         * Speed mod: 0.008003278196411223
         */
        private const val AT_LEAST = 0.281
        private const val BASH = 0.2857671997172534
        private const val SPEED_EFFECT_CONST = 0.008003278196411223

    }

    private var wasFlagged = false

    val repeatable = tickHandler {
        if (player.onGround()) {
            // Strafe when on ground
            player.deltaMovement = player.deltaMovement.withStrafe()
            return@tickHandler
        } else {
            // Not much speed boost, but still a little bit - if someone wants to improve this, feel free to do so
            val horizontalMod = if (horizontalAcceleration) {
                BASE_HORIZONTAL_MODIFIER + HORIZONTAL_SPEED_AMPLIFIER *
                    (player.getEffect(MobEffects.SPEED)?.amplifier ?: 0)
            } else {
                0.0
            }

            // Vertical acceleration, this makes sense to get a little bit more speed again
            val yMod = if (verticalAcceleration && player.deltaMovement.y < 0 && player.fallDistance < 1) {
                VERTICAL_SPEED_AMPLIFIER
            } else {
                0.0
            }

            player.deltaMovement = player.deltaMovement.multiply(1.0 + horizontalMod, 1.0 + yMod, 1.0 + horizontalMod)
        }
    }

    val jumpEvent = handler<PlayerJumpEvent> {
        val atLeast = if (!wasFlagged) {
            AT_LEAST + SPEED_EFFECT_CONST * (player.getEffect(MobEffects.SPEED)?.amplifier ?: 0)
        } else {
            0.0
        }

        player.deltaMovement = player.deltaMovement.withStrafe(speed = player.horizontalSpeed.coerceAtLeast(atLeast))
    }

    /**
     * Damage Boost
     */
    @Suppress("unused")
    val packetHandler = sequenceHandler<PacketEvent>(priority = CRITICAL_MODIFICATION) { event ->
        val packet = event.packet

        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            val velocityX = packet.movement.x
            val velocityZ = packet.movement.z

            waitTicks(1)

            // Fall damage velocity
            val speed = if (velocityX == 0.0 && velocityZ == 0.0 && packet.isMovementYFallDamage()) {
                player.horizontalSpeed.coerceAtLeast(
                    BASH *
                        (player.getEffect(MobEffects.SPEED)?.amplifier ?: 0)
                )
            } else {
                player.horizontalSpeed
            }
            player.deltaMovement = player.deltaMovement.withStrafe(speed = speed)
        } else if (packet is ClientboundPlayerPositionPacket) {
            wasFlagged = true
        }
    }

    override fun disable() {
        wasFlagged = false
    }

}
