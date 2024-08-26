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
import net.minecraft.item.Items
import net.minecraft.network.Packet
import net.minecraft.network.packet.s2c.play.*
import java.util.concurrent.ConcurrentHashMap

object StaffDetector : Module("StaffDetector", Category.MISC, gameDetecting = false, hideModule = false) {

    private val staffMode by object : ListValue("StaffMode", arrayOf("BlocksMC", "CubeCraft", "Gamster", "AgeraPvP"), "BlocksMC") {
        override fun onUpdate(value: String) {
            loadStaffData()
        }
    }

    private val tab by BoolValue("TAB", true)
    private val packet by BoolValue("Packet", true)

    private val autoLeave by ListValue("AutoLeave", arrayOf("Off", "Leave", "Lobby", "Quit"), "Off") { tab || packet }

    private val spectator by BoolValue("StaffSpectator", false) { tab || packet }
    private val otherSpectator by BoolValue("OtherSpectator", false) { tab || packet }

    private val inGame by BoolValue("InGame", true) { autoLeave != "Off" }
    private val warn by ListValue("Warn", arrayOf("Chat", "Notification"), "Chat")

    private val checkedStaff = ConcurrentHashMap.newKeySet<String>()
    private val checkedSpectator = ConcurrentHashMap.newKeySet<String>()
    private val playersInSpectatorMode = ConcurrentHashMap.newKeySet<String>()

    private var attemptLeave = false

    private var staffList = mapOf<String, Set<String>?>()
    private var serverIp = ""

    private val moduleJob = SupervisorJob()
    private val moduleScope = CoroutineScope(Dispatchers.IO + moduleJob)

    override fun onDisable() {
        serverIp = ""
        moduleJob.cancel()
        checkedStaff.clear()
        checkedSpectator.clear()
        playersInSpectatorMode.clear()
        attemptLeave = false
    }

    /**
     * Reset on World Change
     */
    @EventTarget
    fun onWorld(event: WorldEvent) {
        checkedStaff.clear()
        checkedSpectator.clear()
        playersInSpectatorMode.clear()
    }

    private fun loadStaffData() {
        val serverIpMap = mapOf(
            "blocksmc" to "blocksmc.com",
            "cubecraft" to "cubecraft.net",
            "gamster" to "gamster.org",
            "agerapvp" to "agerapvp.club"
        )

        serverIp = serverIpMap[staffMode.lowercase()] ?: return

        moduleScope.launch {
            staffList = loadStaffList("$CLIENT_CLOUD/staffs/$serverIp")
        }
    }

    private fun checkedStaffRemoved() {
        val onlinePlayers = mc.networkHandler?.playerList?.mapNotNull { it?.gameProfile?.name }

        synchronized(checkedStaff) {
            onlinePlayers?.toSet()?.let { checkedStaff.retainAll(it) }
        }
    }

    @EventTarget
    fun onPacket(event: PacketEvent) {
        if (mc.player == null || mc.world == null) {
            return
        }

        val packet = event.packet

        /**
         * OLD BlocksMC Staff Spectator Check
         * Original By HU & Modified by Eclipses
         *
         * NOTE: Doesn't detect staff spectator all the time.
         */
        if (spectator) {
            if (packet is TeamS2CPacket) {
                val teamName = packet.name

                if (teamName.equals("Z_Spectator", true)) {
                    val players = packet.players ?: return

                    val staffSpectateList = players.filter { it in staffList.keys } - checkedSpectator
                    val nonStaffSpectateList = players.filter { it !in staffList.keys } - checkedSpectator

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
                        val isStaff = player in staffList

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
        if (mc.player == null || mc.world == null) {
            return
        }

        val isStaff = staffList.any { entry ->
            entry.value?.any { staffName -> player.contains(staffName) } == true
        }

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

        if (mc.player == null || mc.world == null) {
            return
        }

        val playerList = mc.networkHandler?.playerList ?: return

        val playerInfos = synchronized(playerList) {
            playerList.mapNotNull { playerInfo ->
                playerInfo?.gameProfile?.name?.let { playerName ->
                    playerName to playerInfo.responseTime
                }
            }
        }

        playerInfos.forEach { (player, responseTime) ->
            val isStaff = staffList.any { entry ->
                entry.value?.any { staffName -> player.contains(staffName) } == true
            }

            val condition = when {
                responseTime > 0 -> "§e(${responseTime}ms)"
                responseTime == 0 -> "§a(Joined)"
                else -> "§c(Ping error)"
            }

            val warnings = "§c[STAFF] §d${player} §3is a staff §b(TAB) $condition"

            synchronized(checkedStaff) {
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
    }

    /**
     * Check staff using Packet
     */
    private fun notifyStaffPacket(staff: Entity) {
        if (!packet)
            return

        if (mc.player == null || mc.world == null) {
            return
        }

        val isStaff = if (staff is EntityPlayer) {
            val playerName = staff.gameProfile.name

            staffList.any { entry ->
                entry.value?.any { staffName -> playerName.contains(staffName) } == true
            }
        } else {
            false
        }

        val condition = when (staff) {
            is EntityPlayer -> {
                val responseTime = mc.networkHandler?.getPlayerInfo(staff.uniqueID)?.responseTime ?: 0
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

        synchronized(checkedStaff) {
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
    }

    private fun autoLeave() {
        val firstSlotItemStack = mc.player.inventory.main[0] ?: return

        if (inGame && (firstSlotItemStack.item == Items.COMPASS || firstSlotItemStack.item == Items.bow)) {
            return
        }

        if (!attemptLeave && autoLeave != "Off") {
            when (autoLeave.lowercase()) {
                "leave" -> mc.player.sendChatMessage("/leave")
                "lobby" -> mc.player.sendChatMessage("/lobby")
                "quit" -> mc.world.sendQuittingDisconnectingPacket()
            }
            attemptLeave = true
        }
    }

    private fun handleOtherChecks(packet: Packet<*>?) {
        if (mc.player == null || mc.world == null) {
            return
        }

        fun handlePlayer(player: Entity?) {
            player ?: return
            handleStaff(player)
        }

        when (packet) {
            is GameJoinS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is PlayerSpawnS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityID))
            is EntityPositionS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is EntityTrackerUpdateS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is EntityStatusEffectS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is RemoveEntityStatusEffectS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is EntityStatusS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is EntitySetHeadYawS2CPacket -> handlePlayer(packet.getEntity(mc.world))
            is UpdateEntityNbtS2CPacket -> handlePlayer(packet.getEntity(mc.world))
            is EntityAttachS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityId))
            is EntityEquipmentUpdateS2CPacket -> handlePlayer(mc.world.getEntityById(packet.entityID))
        }
    }

    private fun handleStaff(staff: Entity) {
        if (mc.player == null || mc.world == null) {
            return
        }

        checkedStaffRemoved()

        notifyStaff()
        notifyStaffPacket(staff)
    }

    private suspend fun loadStaffList(url: String): Map<String, Set<String>> {
        return try {
            val (response, code) = fetchDataAsync(url)

            when (code) {
                200 -> {
                    val staffList = response.lineSequence()
                        .filter { it.isNotBlank() }
                        .map { it.trim() }
                        .toSet()

                    Chat.print("§aSuccessfully loaded §9${staffList.size} §astaff names.")
                    mapOf(url to staffList)
                }
                404 -> {
                    Chat.print("§cFailed to load staff list. §9(§3Doesn't exist in LiquidCloud§9)")
                    emptyMap()
                }
                else -> {
                    Chat.print("§cFailed to load staff list. §9(§3ERROR CODE: $code§9)")
                    emptyMap()
                }
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
        get() = staffMode
}