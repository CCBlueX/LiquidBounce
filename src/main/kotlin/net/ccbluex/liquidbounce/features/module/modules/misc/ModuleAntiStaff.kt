package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.ServerConnectEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.register.IncludeModule
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import kotlin.concurrent.thread

/**
 * Notifies you about staff actions.
 */
@IncludeModule
object ModuleAntiStaff : Module("AntiStaff", Category.MISC) {

    object VelocityCheck : ToggleableConfigurable(this, "VelocityCheck", true) {

        val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id) {
                if (packet.velocityX == 0 && packet.velocityZ == 0 && packet.velocityY / 8000.0 > 0.075) {
                    // alert the user
                    alertAboutStaff()
                    return@handler
                }
            }
        }

    }

    object VanishCheck : ToggleableConfigurable(this, "VanishCheck", false) {

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            if (packet is PlayerListS2CPacket) {
                val actions = packet.actions

                if (actions.contains(PlayerListS2CPacket.Action.UPDATE_LATENCY)) {
                    if (packet.entries.size != network.playerList?.size) {
                        alertAboutStaff()
                    } else {
                        notification("AntiStaff", message("vanishClear"), NotificationEvent.Severity.INFO)
                    }
                }
            }
        }

    }

    object UsernameCheck : ToggleableConfigurable(this, "UsernameCheck", false) {

        private val showInTabList by boolean("ShowInTabList", true)

        private val serverStaffList = hashMapOf<String, Array<String>>()

        override fun enable() {
            val serverEntry = mc.currentServerEntry ?: return
            val address = serverEntry.address.dropPort().rootDomain()

            if (serverStaffList.containsKey(address)) {
                return
            }
            serverStaffList[address] = arrayOf()

            loadStaffList(address)
            super.enable()
        }

        @Suppress("unused")
        val handleServerConnect = sequenceHandler<ServerConnectEvent> { event ->
            val address = event.serverAddress.dropPort().rootDomain()

            if (serverStaffList.containsKey(address)) {
                return@sequenceHandler
            }
            serverStaffList[address] = arrayOf()

            // Keeps us from loading the staff list multiple times
            waitUntil { inGame && mc.currentScreen != null }
            loadStaffList(address)
        }

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            if (packet is PlayerListS2CPacket) {
                // playerAdditionEntries returns empty if the packet is not marked with ADD_PLAYER
                val entries = packet.playerAdditionEntries

                for (entry in entries) {
                    val profile = entry.profile ?: continue

                    if (isStaff(profile.name)) {
                        alertAboutStaff(profile.name)
                    }
                }
            }
        }

        private fun loadStaffList(address: String) {
            // Loads the server config
            thread(name = "staff-loader") {
                runCatching {
                    val (code, staffList) =
                        HttpClient.requestWithCode("$CLIENT_CLOUD/staffs/$address", "GET")

                    when (code) {
                        200 -> {
                            val staffs = staffList.lines().toTypedArray()
                            serverStaffList[address] = staffs

                            notification("AntiStaff", message("staffsLoaded", staffs.size, address),
                                NotificationEvent.Severity.SUCCESS)
                        }

                        404 -> notification("AntiStaff", message("noStaffs", address),
                            NotificationEvent.Severity.ERROR)
                        else -> notification("AntiStaff", message("staffsFailed", address, code),
                            NotificationEvent.Severity.ERROR)
                    }
                }.onFailure {
                    notification("AntiStaff", message("staffsFailed", address, it.javaClass.simpleName),
                        NotificationEvent.Severity.ERROR)
                }
            }
        }

        fun shouldShowAsStaffOnTab(username: String): Boolean {
            if (!showInTabList || !ModuleAntiStaff.enabled || !enabled) {
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

        override fun handleEvents() = ModuleAntiStaff.enabled && enabled

    }

    init {
        tree(VelocityCheck)
        tree(VanishCheck)
        tree(UsernameCheck)
    }

    /**
     * Alert the user about staff watching them.
     */
    private fun alertAboutStaff(username: String? = null) {
        val messageKey = if (username == null) "staffDetected" else "specificStaffDetected"
        val message = message(messageKey, username ?: "")
        notification("Staff Detected", message, NotificationEvent.Severity.INFO)
        chat(warning(message(messageKey, username ?: "")))
    }

}
