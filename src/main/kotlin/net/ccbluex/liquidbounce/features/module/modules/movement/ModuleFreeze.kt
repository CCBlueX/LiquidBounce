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
package net.ccbluex.liquidbounce.features.module.modules.movement

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerMoveEvent
import net.ccbluex.liquidbounce.event.events.PlayerTickEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Freeze module
 *
 * Allows you to freeze yourself without the server knowing.
 */
object ModuleFreeze : Module("Freeze", Category.MOVEMENT) {

    private val modes = choices("Mode", Cancel, arrayOf(Cancel, Stationary))
        .apply { tagBy(this) }

    private val disableOnFlag by boolean("DisableOnFlag", true)

    /**
     * Acts as timer = 0 replacement
     */
    @Suppress("unused")
    private val moveHandler = handler<PlayerTickEvent> { event ->
        event.cancelEvent()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.packet is PlayerPositionLookS2CPacket && disableOnFlag) {
            enabled = false
        }
    }

    /**
     * Cancel network communication
     */
    object Cancel : Choice("Cancel") {

        private val cancelIncoming by boolean("CancelIncoming", false)
        private val cancelOutgoing by boolean("CancelOutgoing", true)

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            when (event.origin) {
                TransferOrigin.RECEIVE -> if (cancelIncoming) {
                    event.cancelEvent()
                }

                TransferOrigin.SEND -> if (cancelOutgoing) {
                    event.cancelEvent()
                }
            }
        }

    }

    /**
     * Stationary freeze - only cancel movement but keeps network communication intact
     */
    object Stationary : Choice("Stationary") {

        override val parent: ChoiceConfigurable<Choice>
            get() = modes

        @Suppress("unused")
        private val packetHandler = handler<PacketEvent> { event ->
            // This might actually be useless since we cancel [PlayerTickEvent] which is responsible for movement
            // as well, so this is just a double check
            when (event.packet) {
                is PlayerMoveC2SPacket -> event.cancelEvent()
            }
        }

    }

}
