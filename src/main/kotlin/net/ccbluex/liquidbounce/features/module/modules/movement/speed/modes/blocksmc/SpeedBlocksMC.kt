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

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.SprintEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleDebug.debugParameter
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.ccbluex.liquidbounce.utils.entity.getMovementDirectionOfInput
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.SAFETY_FEATURE
import net.ccbluex.liquidbounce.utils.math.copy
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.round

/**
 * extensive blocksmc speed
 * @author liquidsquid1
 */

class SpeedBlocksMC(override val parent: ChoiceConfigurable<*>) : Choice("BlocksMC") {

    private var roundStrafeYaw by boolean("RoundStrafeYaw", false)

    private var state = 0
    private var flagDelay = 0

    override fun enable() {
        state = 0
    }

    override fun disable() {
        player.velocity = player.velocity.copy(x = 0.0, z = 0.0)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (ModuleScaffold.enabled && ModuleScaffold.isTowering) {
            state = 0
            return@tickHandler
        }

        if (player.isOnGround) {
            state = 1
            if (ModuleScaffold.enabled) {
                state = 2
            }
        }

        var speed = 0.06 + when {
            player.isOnGround -> 0.12
            else -> 0.21
        } + player.velocity.y / 20

        if ((player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) == 1) {
            speed += 0.1
        }

        if (flagDelay > 0) {
            flagDelay--
            repeat(flagDelay) {
                speed -= 0.007
            }
        }

        debugParameter("State") { state }
        when (state) {
            0 -> {} // Pause state, do nothing
            1 -> { // AVG 6,403 BPS
                if (player.airTicks == 4) {
                    player.velocity.y = -0.09800000190734863
                }
            }
            2 -> { // AVG 6,475 BPS
                when (player.airTicks) {
                    1 -> player.velocity.y += 0.0568
                    3 -> player.velocity.y -= 0.13
                    4 -> player.velocity.y -= 0.2
                }
            }
        }

        var yaw = player.getMovementDirectionOfInput(DirectionalInput(player.input))
        if (roundStrafeYaw) {
            yaw = round(yaw / 45) * 45
        }

        if (!player.isOnGround && state != 0) {
            player.velocity = player.velocity.withStrafe(speed = speed, yaw = yaw)
        }
    }

    @Suppress("unused")
    private val movementInputHandler = handler<MovementInputEvent> { event ->
        if (event.directionalInput.isMoving && state != 0) {
            event.jump = true
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerPositionLookS2CPacket) {
            flagDelay = 20
        }
    }

    @Suppress("unused")
    private val sprintHandler = handler<SprintEvent>(priority = SAFETY_FEATURE) { event ->
        if (!event.directionalInput.isMoving) {
            return@handler
        }

        if (event.source == SprintEvent.Source.MOVEMENT_TICK || event.source == SprintEvent.Source.INPUT) {
            event.sprint = true
        }
    }

}
