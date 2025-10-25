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
 */
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import java.util.*

internal object NoFallPacketJump : NoFallMode("PacketJump") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL,
        EnumSet.of(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND))
    private val fallDistance = choices("FallDistance", Smart, arrayOf(Smart, Constant))
    private val timing = choices("Timing", Landing, arrayOf(Landing, Falling))

    private var falling = false

    val tickHandler = handler<PlayerTickEvent> {
        falling = player.fallDistance > fallDistance.activeChoice.value
        if (timing.activeChoice is Falling && !player.isOnGround && falling) {
            network.sendPacket(packetType.generatePacket().apply {
                y += 1.0E-9
            })
            if (Falling.resetFallDistance) {
                player.onLanding()
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (timing.activeChoice is Landing && event.packet is PlayerMoveC2SPacket && event.packet.onGround && falling) {
            falling = false
            network.sendPacket(packetType.generatePacket().apply {
                x = player.lastX
                y = player.lastBaseY + 1.0E-9
                z = player.lastZ
                onGround = false
            })
        }
    }

    private object Landing : Choice("Landing") {
        override val parent: ChoiceConfigurable<*>
            get() = timing
    }

    private object Falling : Choice("Falling") {
        override val parent: ChoiceConfigurable<*>
            get() = timing

        val resetFallDistance by boolean("ResetFallDistance", true)
    }

    private abstract class DistanceMode(name: String) : Choice(name) {
        override val parent: ChoiceConfigurable<*>
            get() = fallDistance

        abstract val value: Float
    }

    private object Smart : DistanceMode("Smart") {
        override val value: Float
            get() = playerSafeFallDistance.toFloat()
    }

    private object Constant : DistanceMode("Constant") {
        override val value by float("Value", 3f, 0f..5f)
    }
}
