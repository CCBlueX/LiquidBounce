package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.init.Items
import net.minecraft.network.Packet
import net.minecraft.network.play.server.*

object StaffDetector : Module("StaffDetector", ModuleCategory.MISC, gameDetecting = false) {

    // TODO: Add more Staff Mode
    private val staffmode by ListValue("StaffMode", arrayOf("BlocksMC"), "BlocksMC")

    private val autoLeave by ListValue("AutoLeave", arrayOf("Off", "Leave", "Lobby", "Quit"), "Off")

    private val spectator by BoolValue("StaffSpectator", false) { staffmode == "BlocksMC" }
    private val otherSpectator by BoolValue("OtherSpectator", false) { staffmode == "BlocksMC" }

    private val inGame by BoolValue("InGame", true) { autoLeave != "Off" && staffmode == "BlocksMC" }
    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    private val checkedStaff = mutableSetOf<String>()
    private val checkedSpectator = mutableSetOf<String>()
    private val playersInSpectatorMode = mutableSetOf<String>()

    private var attemptLeave = false

    /**
     * BlocksMC Staff List
     * Last Updated: 7/02/2024
     */
    private val blocksMCStaff: Set<String> by lazy {
        loadStaffList("staffdetector/blocksmc-staff.txt")
    }

    private fun checkedStaffRemoved() {
        val onlinePlayers = mc.netHandler?.playerInfoMap?.mapNotNull { it?.gameProfile?.name }

        onlinePlayers?.toSet()?.let { checkedStaff.retainAll(it) }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        val packet = event.packet

        /**
         * OLD BlocksMC Staff Spectator Check
         * Original By HU & Modified by Eclipses
         *
         * NOTE: Doesn't detect staff spectator all the time.
         */
        if (spectator && staffmode == "BlocksMC") {
            if (packet is S3EPacketTeams) {
                val teamName = packet.name

                if (teamName.equals("Z_Spectator", true)) {
                    val players = packet.players ?: return

                    val staffSpectateList = players.filter { it in blocksMCStaff } - checkedSpectator
                    val nonStaffSpectateList = players.filter { it !in blocksMCStaff } - checkedSpectator

                    // Check for players who are using spectator menu
                    val miscSpectatorList = playersInSpectatorMode - players.toSet()

                    staffSpectateList.forEach { player ->
                        notifySpectators(player)
                    }

                    nonStaffSpectateList.forEach { player ->
                        if (otherSpectator) {
                            notifySpectators(player)
                        }
                    }

                    miscSpectatorList.forEach { player ->
                        if (player in blocksMCStaff) {
                            Chat.print("§c[STAFF] §d${player} §3is using the spectator menu §e(compass/left)")
                        } else {
                            Chat.print("§d${player} §3is using the spectator menu §e(compass/left)")
                        }
                        checkedSpectator.remove(player)
                    }

                    // Update the set of players in spectator mode
                    playersInSpectatorMode.clear()
                    playersInSpectatorMode.addAll(players)
                }
            }

            // Handle other packets
            handleOtherChecks(packet)
        }
    }

    private fun notifySpectators(player: String) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        if (player in blocksMCStaff) {
            if (warn == "Chat") {
                Chat.print("§c[STAFF] §d${player} §3is a spectators")
            } else {
                hud.addNotification(Notification("§c[STAFF] §d${player} §3is a spectators", 3000F))
            }
        } else {
            if (warn == "Chat") {
                Chat.print("§d${player} §3is a spectators")
            } else {
                hud.addNotification(Notification("§d${player} §3is a spectators", 3000F))
            }
        }

        attemptLeave = false
        checkedSpectator.add(player)

        if (player !in blocksMCStaff) {
            return
        }

        autoLeave()
    }

    private fun notifyStaff() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        mc.netHandler?.playerInfoMap?.mapNotNull { playerInfo ->
            val player = playerInfo?.gameProfile?.name ?: return@mapNotNull
            val isStaff = blocksMCStaff.any { player.contains(it) }

            val condition = when {
                playerInfo.responseTime > 0 -> "§e(${playerInfo.responseTime}ms)"
                playerInfo.responseTime == 0 -> "§a(Joined)"
                else -> "§c(Ping error)"
            }

            val warnings = "§c[STAFF] §d${player} §3is a staff $condition"

            if (isStaff && player !in checkedStaff) {
                if (warn == "Chat") {
                    Chat.print(warnings)
                } else {
                    hud.addNotification(Notification(warnings, 3000F))
                }

                attemptLeave = false
                checkedStaff.add(player)

                autoLeave()
            }
        }
    }

    private fun autoLeave() {
        val firstSlotItemStack = mc.thePlayer.inventory.mainInventory[0] ?: return

        if (inGame && (firstSlotItemStack.item == Items.compass || firstSlotItemStack.item == Items.bow)) {
            return
        }

        if (!attemptLeave) {
            when (autoLeave.lowercase()) {
                "off" -> return
                "leave" -> mc.thePlayer.sendChatMessage("/leave")
                "lobby" -> mc.thePlayer.sendChatMessage("/lobby")
                "quit" -> mc.theWorld.sendQuittingDisconnectingPacket()
            }
        }
        attemptLeave = true
    }

    private fun handleOtherChecks(packet: Packet<*>) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        when (packet) {
            is S07PacketRespawn,
            is S01PacketJoinGame,
            is S39PacketPlayerAbilities,
            is S0CPacketSpawnPlayer,
            is S18PacketEntityTeleport,
            is S1CPacketEntityMetadata,
            is S1DPacketEntityEffect,
            is S1EPacketRemoveEntityEffect,
            is S19PacketEntityStatus,
            is S19PacketEntityHeadLook,
            is S49PacketUpdateEntityNBT -> handleStaff()
        }
    }

    private fun handleStaff() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        checkedStaffRemoved()
        notifyStaff()
    }

    private fun loadStaffList(filePath: String): Set<String> {
        val staffList = mutableSetOf<String>()

        try {
            val fileStream = javaClass.classLoader.getResourceAsStream(filePath)

            if (fileStream != null) {
                val content = fileStream.reader().readText()
                val names = content.split(",").map { it.trim() }
                staffList.addAll(names)
                Chat.print("§aSuccessfully loaded §9${staffList.size} §astaff names")
            } else {
                Chat.print("§cFile not found: §9$filePath")
            }
        } catch (e: Exception) {
            Chat.print("§cError loading staff names from file: §9${e.message}")
        }

        return staffList
    }

    /**
     * HUD TAG
     */
    override val tag
        get() = staffmode
}