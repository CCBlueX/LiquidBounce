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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ResourcePolicy
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.option.MixinServerListAccessor
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.pingThemAll
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.serverList
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.routing.Routing
import net.minecraft.SharedConstants
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerData.ServerPackStatus
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.multiplayer.ServerStatusPinger
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.network.EventLoopGroupHolder
import net.minecraft.util.CommonColors
import net.minecraft.util.Util
import java.net.DatagramPacket
import java.net.InetAddress
import java.net.MulticastSocket
import java.net.UnknownHostException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Future

// GET /api/v1/client/servers
private fun Routing.getServers() = get {
    runCatching {
        serverList.load()
        pingThemAll()

        val servers = JsonArray()
        serverList.servers.forEachIndexed { id, serverInfo ->
            val json = interopGson.toJsonTree(serverInfo)

            if (!json.isJsonObject) {
                logger.warn("Failed to convert serverInfo to json")
                return@forEachIndexed
            }

            val jsonObject = json.asJsonObject
            jsonObject.addProperty("id", id)
            servers.add(jsonObject)
        }

        call.respond(servers)
    }.getOrElse { call.internalServerError("Failed to get servers due to ${it.message}") }
}

// POST /api/v1/client/servers/connect
private fun Routing.postConnect() = post("/connect") {
    data class ServerConnectRequest(val address: String)

    val serverConnectRequest = call.receive<ServerConnectRequest>()
    val serverInfo = serverList.getByAddress(serverConnectRequest.address)
        ?: ServerData("Unknown Server", serverConnectRequest.address, ServerData.Type.OTHER)

    val serverAddress = ServerAddress.parseString(serverInfo.ip)

    mc.execute {
        ConnectScreen.startConnecting(JoinMultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo, false, null)
    }
    call.respondNoContent()
}

// PUT /api/v1/client/servers/add
private fun Routing.putAddServer() = put("/add") {
    data class ServerAddRequest(val name: String, val address: String, val resourcePackPolicy: String? = null)

    val serverAddRequest = call.receive<ServerAddRequest>()

    if (!ServerAddress.isValidAddress(serverAddRequest.address)) {
        call.forbidden("Invalid address")
    }

    val serverInfo = ServerData(serverAddRequest.name, serverAddRequest.address, ServerData.Type.OTHER)
    serverAddRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackStatus = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ServerPackStatus.PROMPT
    }

    serverList.add(serverInfo, false)
    serverList.save()

    call.respondNoContent()
}

// DELETE /api/v1/client/servers/remove
private fun Routing.deleteServer() = delete("/remove") {
    data class ServerRemoveRequest(val id: Int)

    val serverRemoveRequest = call.receive<ServerRemoveRequest>()
    val serverInfo = serverList.get(serverRemoveRequest.id)

    serverList.remove(serverInfo)
    serverList.save()

    call.respondNoContent()
}

// PUT /api/v1/client/servers/edit
private fun Routing.putEditServer() = put("/edit") {
    data class ServerEditRequest(
        val id: Int,
        val name: String,
        val address: String,
        val resourcePackPolicy: String? = null
    )

    val serverEditRequest = call.receive<ServerEditRequest>()
    val serverInfo = serverList.get(serverEditRequest.id)

    serverInfo.name = serverEditRequest.name
    serverInfo.ip = serverEditRequest.address
    serverEditRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackStatus = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ServerPackStatus.PROMPT
    }
    serverList.save()

    call.respondNoContent()
}

// POST /api/v1/client/servers/swap
private fun Routing.postSwapServers() = post("/swap") {
    data class ServerSwapRequest(val from: Int, val to: Int)

    val serverSwapRequest = call.receive<ServerSwapRequest>()

    serverList.swap(serverSwapRequest.from, serverSwapRequest.to)
    serverList.save()
    call.respondNoContent()
}

// POST /api/v1/client/servers/order
private fun Routing.postOrderServers() = post("/order") {
    data class ServerOrderRequest(val order: List<Int>)

    val serverOrderRequest = call.receive<ServerOrderRequest>()

    serverOrderRequest.order.map { serverList.get(it) }
        .forEachIndexed { index, serverInfo ->
            serverList.replace(index, serverInfo)
        }
    serverList.save()

    call.respondNoContent()
}

object ActiveServerList : EventListener {

    internal val serverList = ServerList(mc).apply { load() }

    private val serverListPinger = ServerStatusPinger()
    private val cannotConnectText = Component.translatable("multiplayer.status.cannot_connect")
        .withColor(CommonColors.RED)
    private val cannotResolveText = Component.translatable("multiplayer.status.cannot_resolve")
        .withColor(CommonColors.RED)

    private val pingTasks = mutableListOf<Future<*>>()

    // LAN server detection - listens for UDP multicast from LAN worlds
    private val LAN_GROUP = InetAddress.getByName("224.0.2.60")
    private const val LAN_PORT = 4445
    private const val LAN_SERVER_TTL_MS = 15_000L // Remove servers not seen for 15 seconds

    private data class LanServerEntry(val motd: String, val lastSeen: Long, val serverData: ServerData)

    // ConcurrentHashMap: address -> entry (thread-safe without explicit synchronization)
    private val lanServers = ConcurrentHashMap<String, LanServerEntry>()

    @Volatile
    private var lanDetectorThread: Thread? = null

    @Volatile
    private var lanSocket: MulticastSocket? = null

    init {
        startLanDetection()
        // Shutdown hook to clean up socket when client closes
        Runtime.getRuntime().addShutdownHook(Thread { stopLanDetection() })
    }

    private fun startLanDetection() {
        try {
            val socket = MulticastSocket(LAN_PORT)
            socket.joinGroup(LAN_GROUP)
            socket.soTimeout = 5000
            lanSocket = socket

            lanDetectorThread = Thread({
                runLanDetectionLoop(socket)
            }, "LanServerDetector").apply { isDaemon = true; start() }
        } catch (e: Exception) {
            logger.warn("Unable to start LAN server detection: {}", e.message)
        }
    }

    private fun runLanDetectionLoop(socket: MulticastSocket) {
        try {
            val buf = ByteArray(1024)
            val packet = DatagramPacket(buf, buf.size)

            while (!Thread.currentThread().isInterrupted) {
                try {
                    socket.receive(packet)
                    handleLanBroadcast(packet)
                } catch (_: java.net.SocketTimeoutException) {
                    pruneStaleServers()
                }
            }
        } catch (e: Exception) {
            if (!Thread.currentThread().isInterrupted) {
                logger.warn("LAN server detection error: {}", e.message)
            }
        } finally {
            runCatching { socket.leaveGroup(LAN_GROUP) }
            runCatching { socket.close() }
        }
    }

    private fun handleLanBroadcast(packet: DatagramPacket) {
        val response = String(packet.data, packet.offset, packet.length)
        // LAN broadcast format: "MOTD§port" (e.g., "My World§25565")
        val parts = response.split("\u00A7".toRegex(), limit = 2)
        if (parts.size != 2) return

        val motd = parts[0]
        val addrPart = parts[1]
        // addrPart can be "port" or "address:port"
        val address = if (addrPart.contains(":")) {
            addrPart
        } else {
            "${packet.address.hostAddress}:$addrPart"
        }

        val existing = lanServers[address]
        if (existing != null) {
            // Update lastSeen, keep existing ServerData (preserves ping state)
            lanServers[address] = existing.copy(lastSeen = System.currentTimeMillis())
        } else {
            // New LAN server: create ServerData and trigger ping
            val serverData = ServerData(motd, address, ServerData.Type.LAN)
            lanServers[address] = LanServerEntry(motd, System.currentTimeMillis(), serverData)
            ping(serverData)
        }
    }

    private fun stopLanDetection() {
        lanDetectorThread?.interrupt()
        lanSocket?.close()
        lanServers.clear()
    }

    private fun pruneStaleServers() {
        val now = System.currentTimeMillis()
        lanServers.entries.removeIf { now - it.value.lastSeen > LAN_SERVER_TTL_MS }
    }

    /**
     * Returns the list of currently detected LAN servers with full Server-compatible JSON fields.
     * Uses negative IDs prefixed to avoid collision with regular server IDs.
     */
    fun getLanServers(): List<JsonObject> {
        pruneStaleServers()
        return lanServers.entries.mapIndexed { index, (address, entry) ->
            val sd = entry.serverData
            JsonObject().apply {
                // Use negative IDs to distinguish from regular servers (e.g., -1, -2, ...)
                addProperty("id", -(index + 1))
                addProperty("address", address)
                addProperty("name", entry.motd)
                addProperty("lan", true)
                // Use pinged data from ServerData
                addProperty("ping", sd.ping)
                addProperty("icon", sd.iconBytes?.let { java.util.Base64.getEncoder().encodeToString(it) } ?: "")
                addProperty("label", sd.motd?.string ?: entry.motd)
                add("players", JsonObject().apply {
                    addProperty("max", sd.players?.max() ?: 0)
                    addProperty("online", sd.players?.online() ?: 0)
                })
                addProperty("online", sd.ping > 0)
                addProperty("playerCountLabel", sd.status?.string ?: "")
                addProperty("protocolVersion", sd.protocol)
                addProperty("version", sd.version?.string ?: "LAN")
                addProperty("resourcePackPolicy", sd.resourcePackStatus?.name ?: "PROMPT")
            }
        }
    }

    private fun cancelTasks() {
        pingTasks.forEach { it.cancel(true) }
        pingTasks.clear()
        serverListPinger.removeAll()
    }

    internal fun pingThemAll() {
        cancelTasks()
        serverList.servers
            .distinctBy { it.ip } // We do not want to ping the same server multiple times
            .forEach(this::ping)
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> {
        cancelTasks()
    }

    fun ping(serverEntry: ServerData) {
        if (serverEntry.state() != ServerData.State.INITIAL) {
            return
        }

        serverEntry.setState(ServerData.State.PINGING)
        serverEntry.motd = CommonComponents.EMPTY
        serverEntry.status = CommonComponents.EMPTY

        pingTasks += CompletableFuture.runAsync({
            try {
                serverListPinger.pingServer(serverEntry, { mc.execute(serverList::save) }, {
                    serverEntry.setState(
                        if (serverEntry.protocol == SharedConstants.getCurrentVersion().protocolVersion()) {
                            ServerData.State.SUCCESSFUL
                        } else {
                            ServerData.State.INCOMPATIBLE
                        }
                    )
                }, EventLoopGroupHolder.remote(true))
            } catch (unknownHostException: UnknownHostException) {
                serverEntry.setState(ServerData.State.UNREACHABLE)
                serverEntry.motd = cannotResolveText
                logger.error("Failed to ping server ${serverEntry.name} due to ${unknownHostException.message}")
            } catch (exception: Exception) {
                serverEntry.setState(ServerData.State.UNREACHABLE)
                serverEntry.motd = cannotConnectText
                logger.error("Failed to ping server ${serverEntry.name}", exception)
            }
        }, Util.nonCriticalIoPool())
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        serverListPinger.tick()
    }

    override val running = true

}

val ServerList.servers: List<ServerData>
    get() = (this as MixinServerListAccessor).`liquid_bounce$getServerList`()

fun ServerList.getByAddress(address: String) = servers.firstOrNull { it.ip == address }

// GET /api/v1/client/servers/lan
private fun Routing.getLanServers() = get("/lan") {
    runCatching {
        val servers = JsonArray()
        ActiveServerList.getLanServers().forEach { servers.add(it) }
        call.respond(servers)
    }.getOrElse { call.internalServerError("Failed to get LAN servers due to ${it.message}") }
}

internal fun Routing.serverListRoutes() = route("/servers") {
    getServers()
    getLanServers()
    putAddServer()
    deleteServer()
    putEditServer()
    postSwapServers()
    postOrderServers()
    postConnect()
}
