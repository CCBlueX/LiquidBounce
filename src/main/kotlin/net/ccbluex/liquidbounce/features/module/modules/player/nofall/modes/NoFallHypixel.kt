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

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

internal object NoFallHypixel : NoFallMode("Hypixel") {

    private var doJump = false

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundMovePlayerPacket) {
            if (player.fallDistance >= 3.3) {
                doJump = true
            }

            if (doJump && player.onGround()) {
                packet.onGround = false
                if (!mc.options.keyJump.isDown) {
                    player.setPos(player.position().x, player.position().y + 0.09, player.position().z)
                }

                doJump = false
            }
        }
    }
}
