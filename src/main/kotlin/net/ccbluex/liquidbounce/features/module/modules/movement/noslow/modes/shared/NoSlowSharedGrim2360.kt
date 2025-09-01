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
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.untracked
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.util.Hand

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.60
 */
internal class NoSlowSharedGrim2360(override val parent: ChoiceConfigurable<*>) : Choice("Grim2360") {

    @Suppress("unused")
    private val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (player.isUsingItem && event.state == EventState.PRE) {
            val hand = player.activeHand

            if (hand == Hand.MAIN_HAND) {
                untracked {
                    // Send offhand interact packet
                    // so that grim focuses on offhand noslow checks that don't exist.
                    network.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND, 0, player.yaw, player.pitch))
                }
            } else if (hand == Hand.OFF_HAND) {
                // Switch slots (based on 1.8 grim switch noslow)
                untracked {
                    val slot = player.inventory.selectedSlot
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot % 8 + 1))
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot % 7 + 2))
                    network.sendPacket(UpdateSelectedSlotC2SPacket(slot))
                }
            }
        }
    }

}
