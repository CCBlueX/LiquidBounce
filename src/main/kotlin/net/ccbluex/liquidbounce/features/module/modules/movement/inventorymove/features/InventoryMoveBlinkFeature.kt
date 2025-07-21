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
package net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.features.module.modules.client.ModuleAutoConfig.message
import net.ccbluex.liquidbounce.features.module.modules.movement.inventorymove.ModuleInventoryMove
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.ccbluex.liquidbounce.utils.client.formatAsTime
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.network.packet.c2s.play.*

object InventoryMoveBlinkFeature : ToggleableConfigurable(ModuleInventoryMove, "Blink", false) {

    /**
     * After reaching this time, we will close the inventory and blink.
     */
    private val maximumTime by int("MaximumTime", 10000, 0..30000, "ms")

    private val chronometer = Chronometer()

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        val packet = event.packet

        if (mc.currentScreen is HandledScreen<*> && event.origin == TransferOrigin.OUTGOING) {
            event.action = when (packet) {
                is ClickSlotC2SPacket,
                is ButtonClickC2SPacket,
                is CreativeInventoryActionC2SPacket,
                is SlotChangedStateC2SPacket,
                is CloseHandledScreenC2SPacket -> PacketQueueManager.Action.PASS

                else -> PacketQueueManager.Action.QUEUE
            }
        }
    }

    @Suppress("unused")
    val screenHandler = handler<ScreenEvent> { event ->
        if (event.screen is HandledScreen<*>) {
            chronometer.reset()

            notification(
                "InventoryMove", message("blinkStart", maximumTime.formatAsTime()),
                NotificationEvent.Severity.INFO
            )
        }
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        if (mc.currentScreen is HandledScreen<*> && chronometer.hasElapsed(maximumTime.toLong())) {
            player.closeHandledScreen()
            notification("InventoryMove", message("blinkEnd"), NotificationEvent.Severity.INFO)
        }
    }

}
