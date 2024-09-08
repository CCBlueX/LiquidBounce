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
package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.client.isOlderThanOrEqual1_8
import net.minecraft.item.ShieldItem
import net.minecraft.item.SwordItem
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.util.Hand

/**
 * This module allows the user to block with swords. This makes sense to be used on servers with ViaVersion.
 */
@IncludeModule
object ModuleSwordBlock : Module("SwordBlock", Category.COMBAT) {

    val onlyVisual by boolean("OnlyVisual", false)

    val onPacket = sequenceHandler<PacketEvent> {
        if (onlyVisual) {
            return@sequenceHandler
        }

        // If we are already on the old combat protocol, we don't need to do anything
        if (isOlderThanOrEqual1_8) {
            return@sequenceHandler
        }

        val packet = it.packet

        if (packet is PlayerInteractItemC2SPacket) {
            val hand = packet.hand
            val itemInHand = player.getStackInHand(hand) // or activeItem

            if (hand == Hand.MAIN_HAND && itemInHand.item is SwordItem) {
                val offHandItem = player.getStackInHand(Hand.OFF_HAND)
                if (offHandItem?.item !is ShieldItem) {
                    // Until "now" we should get a shield from the server
                    waitTicks(1)
                    interaction.sendSequencedPacket(world) { sequence ->
                        // This time we use a new sequence
                        PlayerInteractItemC2SPacket(Hand.OFF_HAND, sequence,
                            player.yaw, player.pitch)
                    }
                } else {
                    it.cancelEvent()
                    // We use the old sequence
                    network.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND, packet.sequence,
                        player.yaw, player.pitch))
                }
            }
        }
    }
}
