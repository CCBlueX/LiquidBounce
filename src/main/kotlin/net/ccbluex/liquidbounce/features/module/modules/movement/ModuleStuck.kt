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

package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.event.events.MovementInputEvent
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.movement.DirectionalInput
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleStuck : ClientModule("Stuck", Category.MOVEMENT, disableOnQuit = true) {

    private val disableOnFlag by boolean("DisableOnFlag", true)

    @Suppress("unused")
    private val movementInputEventHandler = handler<MovementInputEvent> {
        player.movement.x = 0.0
        player.movement.y = 0.0
        player.movement.z = 0.0
        it.directionalInput = DirectionalInput.NONE
    }

    @Suppress("unused")
    private val packetEventHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is PlayerPositionLookS2CPacket if disableOnFlag -> {
                notification(
                    this.name,
                    message("disabledOnFlag"),
                    NotificationEvent.Severity.INFO
                )
                enabled = false
            }

            is PlayerMoveC2SPacket -> {
                event.cancelEvent()
            }

            is PlayerInteractItemC2SPacket -> {
                event.cancelEvent()
                sendPacketSilently(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                    )
                )
                sendPacketSilently(
                    PlayerInteractItemC2SPacket(
                        packet.hand, packet.sequence, player.yaw, player.pitch
                    )
                )
            }

            is PlayerInteractEntityC2SPacket -> {
                event.cancelEvent()
                sendPacketSilently(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                    )
                )
                sendPacketSilently(packet)
            }

            is PlayerInteractBlockC2SPacket -> {
                event.cancelEvent()
                sendPacketSilently(
                    PlayerMoveC2SPacket.LookAndOnGround(
                        player.yaw, player.pitch, player.isOnGround, player.horizontalCollision
                    )
                )
                sendPacketSilently(packet)
            }
        }
    }
}
