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
import net.ccbluex.liquidbounce.utils.client.dropPort
import net.ccbluex.liquidbounce.utils.client.inGame
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.rootDomain
import net.ccbluex.liquidbounce.utils.io.HttpClient
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import kotlin.concurrent.thread

/**
 * Notifies you about staff actions.
 */
object ModuleAntiStaff : Module("AntiStaff", Category.MISC) {

    object VelocityCheck : ToggleableConfigurable(this, "VelocityCheck", true) {

        val packetHandler = handler<PacketEvent>(priority = EventPriorityConvention.FIRST_PRIORITY) { event ->
            val packet = event.packet

            // Check if this is a regular velocity update
            if (packet is EntityVelocityUpdateS2CPacket && packet.id == player.id) {
                if (packet.velocityX == 0 && packet.velocityZ == 0 && packet.velocityY > 0) {
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

        private val serverStaffList = hashMapOf<String, Array<String>>()

        @Suppress("unused")
        val handleServerConnect = sequenceHandler<ServerConnectEvent> { event ->
            val address = event.serverAddress.dropPort().rootDomain()

            if (serverStaffList.containsKey(address)) {
                return@sequenceHandler
            }

            // Keeps us from loading the staff list multiple times
            serverStaffList[address] = arrayOf()

            waitUntil { inGame && mc.currentScreen != null }

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

        val packetHandler = handler<PacketEvent> { event ->
            val packet = event.packet

            if (packet is PlayerListS2CPacket) {
                val serverEntry = mc.currentServerEntry ?: return@handler
                val serverAddress = serverEntry.address?.dropPort()?.rootDomain() ?: return@handler

                // playerAdditionEntries returns empty if the packet is not marked with ADD_PLAYER
                val entries = packet.playerAdditionEntries

                val staffs = serverStaffList[serverAddress] ?: return@handler

                for (entry in entries) {
                    val profile = entry.profile ?: continue

                    if (staffs.contains(profile.name)) {
                        notification(
                            "Staff Detected",
                            message("specificStaffDetected", profile.name),
                            NotificationEvent.Severity.INFO
                        )
                    }
                }
            }
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
    private fun alertAboutStaff() {
        notification(
            "Staff Detected",
            message("staffDetected"),
            NotificationEvent.Severity.INFO
        )
    }

}
