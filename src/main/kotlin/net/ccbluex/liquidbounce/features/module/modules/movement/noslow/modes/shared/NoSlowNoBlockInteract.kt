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

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.item.consume.UseAction
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket

/**
 * Cancels block interactions allowing to bypass certain anti-cheats
 *
 * Tested on Watchdog-AntiCheat (hypixel.net)
 * Confirmed to be working on 25th of May 2024
 */
internal class NoSlowNoBlockInteract(
    parent: EventListener? = null,
    actionFilter: (UseAction) -> Boolean = { true }
) : ToggleableConfigurable(parent, "NoBlockInteract", true) {

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerInteractBlockC2SPacket) {
            val useAction =
                player.getStackInHand(packet.hand)?.useAction ?: return@handler
            val blockPos = packet.blockHitResult?.blockPos

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
