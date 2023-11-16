/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.s2c.play.CloseScreenS2CPacket
import net.minecraft.network.packet.s2c.play.OpenScreenS2CPacket

object InventoryTracker : Listenable {

    var isInventoryOpenServerSide = false

    val packetHandler = handler<PacketEvent> {
        val packet = it.packet

        if (packet is CloseHandledScreenC2SPacket || packet is CloseScreenS2CPacket || packet is OpenScreenS2CPacket) {
            // Prevent closing inventory if it is already closed
            if (!isInventoryOpenServerSide && packet is CloseHandledScreenC2SPacket) {
                it.cancelEvent()
                return@handler
            }

            isInventoryOpenServerSide = false
        }

        // Only way to find out if screen is silently opened
        if (packet is ClickSlotC2SPacket) {
            isInventoryOpenServerSide = true
        }
    }

    val screenHandler = handler<ScreenEvent> {
        if (it.screen is InventoryScreen) {
            isInventoryOpenServerSide = true
        }
    }

    // Someday an actual world change event
    val tickHandler = handler<GameTickEvent> {
        if (mc.player?.age == 1) {
            isInventoryOpenServerSide = false
        }
    }
}
