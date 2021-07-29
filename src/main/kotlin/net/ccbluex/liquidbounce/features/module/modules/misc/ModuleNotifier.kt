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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.NotificationEvent
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import java.util.*

/**
 * Notifier module
 *
 * Notifies you about all kinds of events.
 */
object ModuleNotifier : Module("Notifier", Category.MISC) {

    private val joinMessages by boolean("Join Messages", true)
    private val joinMessageFormat by text("Join Message Format", "%s joined")

    private val leaveMessages by boolean("Leave Messages", true)
    private val leaveMessageFormat by text("Leave Message Format", "%s left")

    private val useNotification by boolean("Use Notification", false)

    private val uuidNameCache = hashMapOf<UUID, String>()

    override fun enable() {
        for (entry in network.playerList) {
            uuidNameCache[entry.profile.id] = entry.profile.name
        }
    }

    override fun disable() {
        uuidNameCache.clear()
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerListS2CPacket) {
            when (packet.action) {
                PlayerListS2CPacket.Action.ADD_PLAYER -> {
                    for (entry in packet.entries) {
                        uuidNameCache[entry.profile.id] = entry.profile.name

                        if (joinMessages) {
                            val message = joinMessageFormat.format(entry.profile.name)

                            if (useNotification) {
                                notification("Notifier", message, NotificationEvent.Severity.INFO)
                            } else {
                                chat(regular(message))
                            }
                        }
                    }
                }
                PlayerListS2CPacket.Action.REMOVE_PLAYER -> {
                    for (entry in packet.entries) {
                        if (leaveMessages) {
                            val message = leaveMessageFormat.format(uuidNameCache[entry.profile.id])

                            if (useNotification) {
                                notification("Notifier", message, NotificationEvent.Severity.INFO)
                            } else {
                                chat(regular(message))
                            }
                        }

                        uuidNameCache.remove(entry.profile.id)
                    }
                }
                else -> {
                }
            }
        }
    }

}
