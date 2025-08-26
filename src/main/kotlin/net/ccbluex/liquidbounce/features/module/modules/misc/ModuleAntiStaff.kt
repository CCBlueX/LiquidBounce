package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.Dispatchers
import net.ccbluex.liquidbounce.api.core.HttpException
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.api.services.cdn.ClientCdn.requestStaffList
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket

/**
 * Notifies you about staff actions.
 */
object ModuleAntiStaff : ClientModule("AntiStaff", Category.MISC) {

    private val showInTabList by boolean("ShowInTabList", true)
    private val serverStaffList = hashMapOf<String, Set<String>>()

    override fun onEnabled() {
        val serverEntry = mc.currentServerEntry ?: return
        val address = serverEntry.address.dropPort().rootDomain()

        if (serverStaffList.containsKey(address)) {
            return
        }
        serverStaffList[address] = emptySet()

        withScope {
            loadStaffList(address)
        }
        super.onEnabled()
    }

    @Suppress("unused")
    val handleServerConnect = sequenceHandler<ServerConnectEvent> { event ->
        val address = event.serverInfo.address.dropPort().rootDomain()

        if (serverStaffList.containsKey(address)) {
            return@sequenceHandler
        }
        serverStaffList[address] = emptySet()

        // Keeps us from loading the staff list multiple times
        waitUntil { inGame && mc.currentScreen != null }

        // Load the staff list
        waitFor(Dispatchers.IO) {
            loadStaffList(address)
        }
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        val packet = event.packet

        if (packet is PlayerListS2CPacket) {
            // playerAdditionEntries returns empty if the packet is not marked with ADD_PLAYER
            val entries = packet.playerAdditionEntries

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
        if (!showInTabList || !ModuleAntiStaff.running || !enabled) {
            return false
        }

        return isStaff(username)
    }

    private fun isStaff(username: String): Boolean {
        val serverEntry = mc.currentServerEntry ?: return false
        val serverAddress = serverEntry.address?.dropPort()?.rootDomain() ?: return false
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
