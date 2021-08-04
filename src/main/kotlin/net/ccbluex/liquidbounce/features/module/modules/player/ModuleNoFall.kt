/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.config.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket

/**
 * NoFall module
 *
 * Protects you from taking fall damage.
 */

object ModuleNoFall : Module("NoFall", Category.PLAYER) {

    private val modes = choices(
        "Mode",
        SpoofGround,
        arrayOf(
            SpoofGround,
            NoGround,
            Packet
        )
    )

    private object SpoofGround : Choice("SpoofGround") {

        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = true
            }

        }

    }

    private object NoGround : Choice("NoGround") {

        override val parent: ChoiceConfigurable
            get() = modes

        val packetHandler = handler<PacketEvent> {
            val packet = it.packet

            if (packet is PlayerMoveC2SPacket) {
                packet.onGround = false
            }

        }

    }

    private object Packet : Choice("Packet") {

        override val parent: ChoiceConfigurable
            get() = modes

        val repeatable = repeatable {
            if (player.fallDistance > 2f) {
                network.sendPacket(PlayerMoveC2SPacket.OnGroundOnly(true))
            }
        }

    }

}
