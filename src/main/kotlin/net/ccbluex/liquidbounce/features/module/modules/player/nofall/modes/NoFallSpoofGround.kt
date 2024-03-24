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
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.player.nofall.ModuleNoFall
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * SpoofGround mode for the NoFall module.
 * This mode spoofs the 'onGround' flag in PlayerMoveC2SPacket to prevent fall damage.
 */
internal object NoFallSpoofGround : Choice("SpoofGround") {

    val fallDistance by float("FallDistance", 1.7f, 0f..5f)

    // Specify the parent configuration for this mode
    override val parent: ChoiceConfigurable<*>
        get() = ModuleNoFall.modes

    // Packet handler to intercept and modify PlayerMoveC2SPacket
    val packetHandler = handler<PacketEvent> {
        // Retrieve the packet from the event
        val packet = it.packet

        // Check if the packet is a PlayerMoveC2SPacket
        if (packet is PlayerMoveC2SPacket && player.fallDistance >= fallDistance) {
            // Modify the 'onGround' flag to true, preventing fall damage
            packet.onGround = true
        }
    }

}
