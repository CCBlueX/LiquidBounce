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

    private val autoLeave by ListValue("AutoLeave", arrayOf("Off", "Leave", "Quit"), "Off")

    private val spectator by BoolValue("StaffSpectator", false) { staffmode == "BlocksMC" }
    private val otherSpectator by BoolValue("OtherSpectator", false) { staffmode == "BlocksMC" }

    private val inGame by BoolValue("InGame", true) { autoLeave != "Off" && staffmode == "BlocksMC" }
    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    private val checkedStaff = mutableSetOf<String>()
    private val checkedSpectator = mutableSetOf<String>()

    private var attemptLeave = false

    /**
     * BlocksMC Staff List
     * Last Updated: 3/02/2024
     */
    private val blocksMCStaff = setOf(
        "iDhoom", "7sO", "Jinaaan", "comsterr", "1Sweet", "Ev2n", "xMz7", "1Daykel", "mohmad_q8", "xImTaiG_", "Nshme", "1Rana", "Refolt",
        "e9_", "1LaB", "1HeyImHasson_", "Bunkrat", "yzed", "_NonameIsHere_", "_sadeq", "loovq", "nv0ola", "1Ahmd", "1F5aMH___3oo", "xiDayzer",
        "A7mmd", "Firas", "EyesO_Diamond", "reficiency", "0Nada", "Meedo_qb", "1flyn", "LwwH", "F2rris", "Casteret", "iTsJuan_", "RamboKinq",
        "plumsdust", "Iv2a", "Aymann_", "Zaainab", "E3Y", "KinderBueno__", "1Elyy", "xL2d", "Postme", "Wesccar", "berriesdust", "GsOMAR",
        "Sanfoor_J", "A2boD", "0hFault", "ImMEHDI", "oTalal", "Jxicide", "xLuffy1", "Tvrki", "Klinvc", "5ald_KSA", "JustRois_", "CallinU",
        "Rma7o", "brksfrb2", "0hilra", "MADIX707", "_R3", "0RyZe", "StrongesT0ne", "6ahr_Almjals", "Nar69", "1Meran", "zixgamer", "MVP11",
        "iISrab5bGIi", "Invincib1le", "FexoraNEP", "TargetConfirmed", "Qatoosa", "_Revoox_", "Watchdog", "Zz__Mr3nb__zZ", "1OdeY", "1Reyleigh",
        "5_O5", "Pynifical", "xzvv", "Aboshxm", "FaaaRis", "Wacros", "_JustIdk", "UnderTest", "420WaFFLe", "SweetyAlice", "vnmpiric", "sev2ene",
        "TheWarriorTricky", "kajo__", "1Sltan_", "sh59", "dangerousarea21", "MadDragon007", "_odex", "ItsSafe", "royallblu", "1Ely"
    )

    private fun checkedStaffRemoved() {
        val onlinePlayers = mc.netHandler?.playerInfoMap?.map { it.gameProfile.name }

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

                    staffSpectateList.forEach { player ->
                        notifySpectators(player)
                    }

                    nonStaffSpectateList.forEach { player ->
                        if (otherSpectator) {
                            notifySpectators(player)
                        }
                    }
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

        val firstSlotItemStack = mc.thePlayer.inventory.mainInventory[0]

        if (!otherSpectator) {
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

        if (inGame && (firstSlotItemStack?.item != Items.compass && firstSlotItemStack?.item != Items.bow)) {
            if (!attemptLeave) {
                when (autoLeave.lowercase()) {
                    "off" -> return
                    "leave" -> mc.thePlayer.sendChatMessage("/leave")
                    "quit" -> mc.theWorld.sendQuittingDisconnectingPacket()
                }
                attemptLeave = true
            }
        }
    }

    private fun notifyStaff() {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        mc.netHandler.playerInfoMap.forEach { playerInfo ->
            val player = playerInfo?.gameProfile?.name ?: return@forEach

            val isStaff = blocksMCStaff.any { player.contains(it) }

            if (isStaff && player !in checkedStaff) {
                if (warn == "Chat") {
                    Chat.print("§c[STAFF] §d${player} §3is a staff")
                } else {
                    hud.addNotification(Notification("§c[STAFF] §d${player} §3is a staff", 3000F))
                }

                checkedStaff.add(player)
            }
        }

        val firstSlotItemStack = mc.thePlayer.inventory.mainInventory[0]

        if (inGame && (firstSlotItemStack?.item != Items.compass && firstSlotItemStack?.item != Items.bow)) {
            if (!attemptLeave) {
                when (autoLeave.lowercase()) {
                    "off" -> return
                    "leave" -> mc.thePlayer.sendChatMessage("/leave")
                    "quit" -> mc.theWorld.sendQuittingDisconnectingPacket()
                }
                attemptLeave = true
            }
        }
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
        checkedStaffRemoved()
        notifyStaff()
    }

    /**
     * HUD TAG
     */
    override val tag
        get() = staffmode
}