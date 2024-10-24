/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.specific

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.utils.client.Timer
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.entity.strafe
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import kotlin.math.sqrt


object FlyMospixelZoom : Choice("MospixelZoom") {

    override val parent: ChoiceConfigurable<*>
        get() = ModuleFly.modes


    private var timer by float("Timer", 2.5f, 1.0f..5.0f)

    private var ticksEnabled = 0
    private var speed = 0.0

    val repeatable = repeatable {

        ticksEnabled++

        if (!player.moving || player.horizontalCollision) {
            // The player needs to be slowed down if they restart the flying
            // process, therefore if they stop moving or bump into a wall
            // then the default speed needs to be there.
            speed = 0.12
        }

        if (player.isOnGround) {
            player.jump()
        } else {

            player.velocity.y = 0.0

            player.strafe(speed = speed)
            if (ticksEnabled < 25) {
                Timer.requestTimerSpeed(timer, Priority.IMPORTANT_FOR_USAGE_1, ModuleFly)
            }

            if (player.age % 10 == 0) {
                player.setPos(player.x, player.y - 0.001, player.z) // Stops certain fly flags.
            }

            speed *= 0.992 // The fly needs to decelerate as it goes on

        }

    }

    override fun enable() {

        ticksEnabled = 0
        speed = 0.12 // Base speed, seems to last an okay amount of time
        if (player.isOnGround) {
            speed += sqrt(6.0) // Boost speed, seems to be a stable amount
        }

        super.enable()
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerMoveC2SPacket) {
            event.packet.onGround = false
        }
    }

}
