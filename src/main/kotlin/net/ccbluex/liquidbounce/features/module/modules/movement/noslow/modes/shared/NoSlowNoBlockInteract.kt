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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket
import net.minecraft.world.item.ItemUseAnimation

/**
 * Cancels block interactions allowing to bypass certain anti-cheats
 *
 * Tested on Watchdog-AntiCheat (hypixel.net)
 * Confirmed to be working on 25th of May 2024
 */
internal class NoSlowNoBlockInteract(
    parent: EventListener? = null,
    actionFilter: (ItemUseAnimation) -> Boolean = { true }
) : ToggleableValueGroup(parent, "NoBlockInteract", true) {

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ServerboundUseItemOnPacket) {
            val useAction =
                player.getItemInHand(packet.hand)?.useAnimation ?: return@handler
            val blockPos = packet.hitResult?.blockPos

            // Check if we might click a block that is not air
            if (blockPos != null && blockPos.getState()?.isAir != true) {
                return@handler
            }

            if (actionFilter(useAction)) {
                event.cancelEvent()
            }
        }
    }

}
