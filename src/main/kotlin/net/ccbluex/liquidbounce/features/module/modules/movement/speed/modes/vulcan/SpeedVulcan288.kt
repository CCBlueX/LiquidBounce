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
package net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.vulcan

import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerAfterJumpEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.movement.speed.modes.SpeedBHopBase
import net.ccbluex.liquidbounce.utils.entity.withStrafe
import net.ccbluex.liquidbounce.utils.math.copy
import net.minecraft.entity.effect.StatusEffects
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import kotlin.math.abs

/**
 * BHop Speed for Vulcan 288
 * Tested on both anticheat-test.com and loyisa.cn
 */
class SpeedVulcan288(override val parent: ChoiceConfigurable<*>) : SpeedBHopBase("Vulcan288", parent) {

    @Suppress("unused")
    private val afterJumpHandler = sequenceHandler<PlayerAfterJumpEvent> {
        val hasSpeed = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) != 0

        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.771 else 0.5)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.605 else 0.31)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.57 else 0.29)
        // does max possible motion down without introducing other issues
        player.velocity = player.velocity.copy(y = if (hasSpeed) -0.5 else -0.37)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.595 else 0.27)
        waitTicks(1)
        player.velocity = player.velocity.withStrafe(speed = if (hasSpeed) 0.595 else 0.28)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        val hasSpeed = (player.getStatusEffect(StatusEffects.SPEED)?.amplifier ?: 0) != 0
        if (!player.isOnGround) {
            if (abs(player.fallDistance) > 0 && hasSpeed) {
                player.velocity.x *= 1.055
                player.velocity.z *= 1.055
            }
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet
        if (packet is PlayerMoveC2SPacket && player.velocity.y < 0) {
            packet.onGround = true
        }
    }

}
