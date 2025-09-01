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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

/**
 * Notifier module
 *
 * Notifies you about all kinds of events.
 */
object ModuleNotifier : ClientModule("Notifier", Category.MISC) {

    init {
        doNotIncludeAlways()
    }

    private val joinMessages by boolean("JoinMessages", true)
    private val joinMessageFormat by text("JoinMessageFormat", "%s joined")

    private val leaveMessages by boolean("LeaveMessages", true)
    private val leaveMessageFormat by text("LeaveMessageFormat", "%s left")

    private val useNotification by boolean("UseNotification", false)

    private val uuidNameCache = hashMapOf<UUID, String>()

    override fun onEnabled() {
        for (entry in network.playerList) {
            uuidNameCache[entry.profile.id] = entry.profile.name
        }
    }

    override fun onDisabled() {
        uuidNameCache.clear()
    }

    val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerListS2CPacket) {
            for (entry in packet.playerAdditionEntries) {
                val profile = entry.profile ?: continue

                if (profile.name != null && profile.name.length > 2) {
                    uuidNameCache[profile.id] = profile.name
                    if (joinMessages) {
                        val message = joinMessageFormat.format(profile.name)

                        if (useNotification) {
                            notification("Notifier", message, NotificationEvent.Severity.INFO)
                        } else {
                            chat(regular(message))
                        }
                    }
                }
            }
        } else if (packet is PlayerRemoveS2CPacket) {
            for (uuid in packet.profileIds) {
                val entry = network.playerList.find { it.profile.id == uuid } ?: continue

                if (entry.profile.name != null && entry.profile.name.length > 2) {
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
        }
    }

}
