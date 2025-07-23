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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.sendPacketSilently
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket

internal class NoSlowSharedInvalidHand(override val parent: ChoiceConfigurable<*>) : Choice("InvalidHand") {

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.READ_FINAL_STATE) { event ->
        val packet = event.packet

        if (!event.isCancelled && event.origin == TransferOrigin.OUTGOING && packet is PlayerInteractItemC2SPacket) {
            event.cancelEvent()
            sendPacketSilently(PlayerInteractItemC2SPacket(null, packet.sequence, packet.pitch, packet.yaw))
        }
    }

}
