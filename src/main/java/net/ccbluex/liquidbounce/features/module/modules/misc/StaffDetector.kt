/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import kotlinx.coroutines.*
import net.ccbluex.liquidbounce.LiquidBounce.hud
import net.ccbluex.liquidbounce.LiquidBounce.CLIENT_CLOUD
import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.event.WorldEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.script.api.global.Chat
import net.ccbluex.liquidbounce.ui.client.hud.element.elements.Notification
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.value.BoolValue
import net.ccbluex.liquidbounce.value.ListValue
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.init.Items
import net.minecraft.network.Packet
import net.minecraft.network.play.server.*

object StaffDetector : Module("StaffDetector", Category.MISC, gameDetecting = false, hideModule = false) {

    // TODO: Add more Staff Mode
    private val staffmode by ListValue("StaffMode", arrayOf("BlocksMC"), "BlocksMC")
    private val tab by BoolValue("TAB", true) { staffmode == "BlocksMC" }
    private val packet by BoolValue("Packet", true) { staffmode == "BlocksMC" }

    private val autoLeave by ListValue("AutoLeave", arrayOf("Off", "Leave", "Lobby", "Quit"), "Off") { tab || packet }

    private val spectator by BoolValue("StaffSpectator", false) { staffmode == "BlocksMC" && (tab || packet) }
    private val otherSpectator by BoolValue("OtherSpectator", false) { staffmode == "BlocksMC" && (tab || packet) }

    private val inGame by BoolValue("InGame", true) { autoLeave != "Off" && staffmode == "BlocksMC" }
    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    private val checkedStaff = mutableSetOf<String?>()
    private val checkedSpectator = mutableSetOf<String?>()
    private val playersInSpectatorMode = mutableSetOf<String?>()

    private var attemptLeave = false

    private var blocksMCStaff = mapOf<String, Set<String>?>()

    // Run on start
    init {
        runBlocking {
            launch { blocksMCStaff = loadStaffList("$CLIENT_CLOUD/staffs/blocksmc.com") }
        }.isCompleted
    }

    /**
     * Reset on World Change
     */
    @EventTarget
    fun onWorld(event: WorldEvent) {
        if (checkedStaff.isNotEmpty())
            checkedStaff.clear()

        if (checkedSpectator.isNotEmpty())
            checkedSpectator.clear()

        if (playersInSpectatorMode.isNotEmpty())
            playersInSpectatorMode.clear()
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
                        notifySpectators(player!!)
                    }

                    nonStaffSpectateList.forEach { player ->
                        if (otherSpectator) {
                            notifySpectators(player!!)
                        }
                    }

                    miscSpectatorList.forEach { player ->
                        val isStaff = player in blocksMCStaff

                        if (isStaff && spectator) {
                            Chat.print("§c[STAFF] §d${player} §3is using the spectator menu §e(compass/left)")
                        }

                        if (!isStaff && otherSpectator) {
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

        val isStaff = player in blocksMCStaff

        if (isStaff && spectator) {
            if (warn == "Chat") {
                Chat.print("§c[STAFF] §d${player} §3is a spectators")
            } else {
                hud.addNotification(Notification("§c[STAFF] §d${player} §3is a spectators", 3000F))
            }
        }

        if (!isStaff && otherSpectator) {
            if (warn == "Chat") {
                Chat.print("§d${player} §3is a spectators")
            } else {
                hud.addNotification(Notification("§d${player} §3is a spectators", 3000F))
            }
        }

        attemptLeave = false
        checkedSpectator.add(player)

        if (isStaff) {
            autoLeave()
        }
    }

    /**
     * Check staff using TAB
     */
    private fun notifyStaff() {
        if (!tab)
            return

        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        mc.netHandler?.playerInfoMap?.mapNotNull { playerInfo ->
            val player = playerInfo?.gameProfile?.name ?: return@mapNotNull

            val isStaff = blocksMCStaff.any { entry ->
                entry.value?.any { staffName -> player.contains(staffName) } == true
            }

            val condition = when {
                playerInfo.responseTime > 0 -> "§e(${playerInfo.responseTime}ms)"
                playerInfo.responseTime == 0 -> "§a(Joined)"
                else -> "§c(Ping error)"
            }

            val warnings = "§c[STAFF] §d${player} §3is a staff §b(TAB) $condition"

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

    /**
     * Check staff using Packet
     */
    private fun notifyStaffPacket(staff: Entity) {
        if (!packet)
            return

        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        val isStaff = if (staff is EntityPlayer) {
            val playerName = staff.gameProfile.name

            blocksMCStaff.any { entry ->
                entry.value?.any { staffName -> playerName.contains(staffName) } == true
            }
        } else {
            false
        }

        val condition = when (staff) {
            is EntityPlayer -> {
                val responseTime = mc.netHandler?.getPlayerInfo(staff.uniqueID)?.responseTime ?: 0
                when {
                    responseTime > 0 -> "§e(${responseTime}ms)"
                    responseTime == 0 -> "§a(Joined)"
                    else -> "§c(Ping error)"
                }
            }
            else -> ""
        }

        val playerName = if (staff is EntityPlayer) staff.gameProfile.name else ""

        val warnings = "§c[STAFF] §d${playerName} §3is a staff §b(Packet) $condition"

        if (isStaff && playerName !in checkedStaff) {
            if (warn == "Chat") {
                Chat.print(warnings)
            } else {
                hud.addNotification(Notification(warnings, 3000F))
            }

            attemptLeave = false
            checkedStaff.add(playerName)

            autoLeave()
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

    private fun handleOtherChecks(packet: Packet<*>?) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        fun handlePlayer(player: Entity?) {
            player ?: return
            handleStaff(player)
        }

        when (packet) {
            is S01PacketJoinGame -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S0CPacketSpawnPlayer -> handlePlayer(mc.theWorld.getEntityByID(packet.entityID))
            is S18PacketEntityTeleport -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S1CPacketEntityMetadata -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S1DPacketEntityEffect -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S1EPacketRemoveEntityEffect -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S19PacketEntityStatus -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S19PacketEntityHeadLook -> handlePlayer(packet.getEntity(mc.theWorld))
            is S49PacketUpdateEntityNBT -> handlePlayer(packet.getEntity(mc.theWorld))
            is S1BPacketEntityAttach -> handlePlayer(mc.theWorld.getEntityByID(packet.entityId))
            is S04PacketEntityEquipment -> handlePlayer(mc.theWorld.getEntityByID(packet.entityID))
        }
    }

    private fun handleStaff(staff: Entity) {
        if (mc.thePlayer == null || mc.theWorld == null) {
            return
        }

        checkedStaffRemoved()

        notifyStaff()
        notifyStaffPacket(staff)
    }

    private suspend fun loadStaffList(url: String): Map<String, Set<String>> {
        return try {
            val (response, code) = fetchDataAsync(url)
            if (code == 200) {
                val staffList = response.split("\n").filter { it.isNotBlank() && it.isNotEmpty() }.map { it.trim() }.toSet()
                Chat.print("§aSuccessfully loaded §9${staffList.size} §astaff names.")
                mapOf(url to staffList)
            } else {
                Chat.print("§cFailed to load staff list. §9(ERROR CODE: $code)")
                emptyMap()
            }
        } catch (e: Exception) {
            Chat.print("§cFailed to load staff list. §9(${e.message})")
            e.printStackTrace()
            emptyMap()
        }
    }

    private suspend fun fetchDataAsync(url: String): Pair<String, Int> {
        return withContext(Dispatchers.IO) {
            HttpUtils.request(url, "GET").let { Pair(it.first, it.second) }
        }
    }

    /**
     * HUD TAG
     */
    override val tag
        get() = staffmode
}