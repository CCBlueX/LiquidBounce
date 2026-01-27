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

package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.services.cdn.ClientCdn.requestStaffList
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.rootDomain
import net.ccbluex.liquidbounce.utils.client.warning
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket

/**
 * Notifies you about staff actions.
 */
object ModuleAntiStaff : ClientModule("AntiStaff", ModuleCategories.MISC) {

    private val showInTabList by boolean("ShowInTabList", true)
    private val serverStaffList = hashMapOf<String, Set<String>>()

    override suspend fun enabledEffect() {
        val serverEntry = mc.currentServer ?: return
        val address = serverEntry.ip.dropPort().rootDomain()

        if (serverStaffList.containsKey(address)) {
            return
        }
        serverStaffList[address] = emptySet()

        loadStaffList(address)
    }

    @Suppress("unused")
    val handleServerConnect = sequenceHandler<ServerConnectEvent> { event ->
        val address = event.serverInfo.ip.dropPort().rootDomain()

        if (serverStaffList.containsKey(address)) {
            return@sequenceHandler
        }
        serverStaffList[address] = emptySet()

        // Keeps us from loading the staff list multiple times
        tickUntil { inGame && mc.screen != null }

        // Load the staff list
        loadStaffList(address)
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is ClientboundPlayerInfoUpdatePacket) {
            // playerAdditionEntries returns empty if the packet is not marked with ADD_PLAYER
            val entries = packet.newEntries()

            for (entry in entries) {
                val profile = entry.profile ?: continue

                if (isStaff(profile.name)) {
                    alert("staffAlert", profile.name)
                }
            }
        }
    }

    suspend fun loadStaffList(address: String) {
        try {
            val staffs = requestStaffList(address)
            serverStaffList[address] = staffs

            logger.info("[AntiStaff] Loaded ${staffs.size} staff member for $address")
            notification("AntiStaff", message("staffsLoaded", staffs.size, address),
                NotificationEvent.Severity.SUCCESS)
        } catch (httpException: HttpException) {
            when (httpException.code) {
                404 -> notification("AntiStaff", message("noStaffs", address),
                    NotificationEvent.Severity.ERROR)
                else -> notification("AntiStaff", message("staffsFailed", address,
                    httpException.code), NotificationEvent.Severity.ERROR)
            }
        } catch (exception: Exception) {
            logger.error("Failed to load staff list of $address", exception)
            notification("AntiStaff", message("staffsFailed", address,
                exception.javaClass.simpleName), NotificationEvent.Severity.ERROR)
        }
    }

    fun shouldShowAsStaffOnTab(username: String): Boolean {
        if (!showInTabList || !running || !enabled) {
            return false
        }

        return isStaff(username)
    }

    private fun isStaff(username: String): Boolean {
        val serverEntry = mc.currentServer ?: return false
        val serverAddress = serverEntry.ip?.dropPort()?.rootDomain() ?: return false
        val staffs = serverStaffList[serverAddress] ?: return false

        return staffs.contains(username)

    }

    /**
     * Alert the user about staff watching them.
     */
    private fun alert(key: String, username: String? = null) {
        val message = message(key, username ?: "")
        notification("Staff Detected", message, NotificationEvent.Severity.INFO)
        chat(
            warning(message(key, username ?: "")),
            metadata = MessageMetadata(id = "${this.name}#${username ?: "generic"}")
        )
    }

}
