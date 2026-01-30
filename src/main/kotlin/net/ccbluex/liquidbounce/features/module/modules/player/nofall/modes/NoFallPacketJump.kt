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
package net.ccbluex.liquidbounce.features.module.modules.player.nofall.modes

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.MovePacketType
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

internal object NoFallPacketJump : NoFallMode("PacketJump") {
    private val packetType by enumChoice("PacketType", MovePacketType.FULL,
        enumSetOf(MovePacketType.FULL, MovePacketType.POSITION_AND_ON_GROUND)
    )
    private val fallDistance = modes("FallDistance", Smart, arrayOf(Smart, Constant))
    private val timing = modes("Timing", Landing, arrayOf(Landing, Falling))

    @Volatile
    private var falling = false

    val tickHandler = handler<PlayerTickEvent> {
        falling = player.fallDistance > fallDistance.activeMode.value
        if (timing.activeMode is Falling && !player.onGround() && falling) {
            network.send(packetType.generatePacket().apply {
                y += 1.0E-9
            })
            if (Falling.resetFallDistance) {
                player.resetFallDistance()
            }
        }
    }

    val packetHandler = handler<PacketEvent> { event ->
        if (timing.activeMode is Landing &&
            event.packet is ServerboundMovePlayerPacket && event.packet.onGround && falling
        ) {
            falling = false
            network.send(packetType.generatePacket().apply {
                x = player.xo
                y = player.yLast + 1.0E-9
                z = player.zo
                onGround = false
            })
        }
    }

    private object Landing : Mode("Landing") {
        override val parent: ModeValueGroup<*>
            get() = timing
    }

    private object Falling : Mode("Falling") {
        override val parent: ModeValueGroup<*>
            get() = timing

        val resetFallDistance by boolean("ResetFallDistance", true)
    }

    private abstract class DistanceMode(name: String) : Mode(name) {
        override val parent: ModeValueGroup<*>
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
