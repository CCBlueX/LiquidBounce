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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ResourcePolicy
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientShutdownEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ScreenEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.injection.mixins.minecraft.client.option.MixinServerListAccessor
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.internalServerError
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.pingThemAll
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game.ActiveServerList.serverList
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.kotlin.Minecraft
import net.minecraft.SharedConstants
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.gui.screens.TitleScreen
import net.minecraft.client.gui.screens.multiplayer.JoinMultiplayerScreen
import net.minecraft.client.multiplayer.ServerData
import net.minecraft.client.multiplayer.ServerData.ServerPackStatus
import net.minecraft.client.multiplayer.ServerList
import net.minecraft.client.multiplayer.ServerStatusPinger
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.client.server.LanServerDetection
import net.minecraft.network.chat.CommonComponents
import net.minecraft.network.chat.Component
import net.minecraft.server.network.EventLoopGroupHolder
import net.minecraft.util.CommonColors
import net.minecraft.util.Util
import java.net.UnknownHostException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

// GET /api/v1/client/servers
private fun Route.getServers() = get {
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
private fun Route.postConnect() = post("/connect") {
    data class ServerConnectRequest(val address: String)

    val serverConnectRequest = call.receive<ServerConnectRequest>()
    val serverInfo = serverList.getByAddress(serverConnectRequest.address)
        ?: ServerData("Unknown Server", serverConnectRequest.address, ServerData.Type.OTHER)

    val serverAddress = ServerAddress.parseString(serverInfo.ip)

    mc.execute {
        ConnectScreen.startConnecting(JoinMultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo, false, null)
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// PUT /api/v1/client/servers/add
private fun Route.putAddServer() = put("/add") {
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

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/servers/remove
private fun Route.deleteServer() = delete("/remove") {
    data class ServerRemoveRequest(val id: Int)

    val serverRemoveRequest = call.receive<ServerRemoveRequest>()
    val serverInfo = serverList.get(serverRemoveRequest.id)

    serverList.remove(serverInfo)
    serverList.save()

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// PUT /api/v1/client/servers/edit
private fun Route.putEditServer() = put("/edit") {
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

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/servers/swap
private fun Route.postSwapServers() = post("/swap") {
    data class ServerSwapRequest(val from: Int, val to: Int)

    val serverSwapRequest = call.receive<ServerSwapRequest>()

    serverList.swap(serverSwapRequest.from, serverSwapRequest.to)
    serverList.save()
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/servers/order
private fun Route.postOrderServers() = post("/order") {
    data class ServerOrderRequest(val order: List<Int>)

    val serverOrderRequest = call.receive<ServerOrderRequest>()

    serverOrderRequest.order.map { serverList.get(it) }
        .forEachIndexed { index, serverInfo ->
            serverList.replace(index, serverInfo)
        }
    serverList.save()

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// GET /api/v1/client/servers/lan
private fun Route.getLanServers() = get("/lan") {
    runCatching {
        call.respond(ActiveServerList.getLanServers())
    }.getOrElse { call.internalServerError("Failed to get LAN servers due to ${it.message}") }
}

object ActiveServerList : EventListener {

    internal val serverList = ServerList(mc).apply { load() }

    private val serverListPinger = ServerStatusPinger()
    private val cannotConnectText = Component.translatable("multiplayer.status.cannot_connect")
        .withColor(CommonColors.RED)
    private val cannotResolveText = Component.translatable("multiplayer.status.cannot_resolve")
        .withColor(CommonColors.RED)

    private val pingTasks = mutableListOf<Future<*>>()

    // LAN server detection using vanilla Minecraft's LanServerDetection
    private val lanServerList = LanServerDetection.LanServerList()
    @Volatile
    private var lanDetector: LanServerDetection.LanServerDetector? = null

    /**
     * Tracks ServerData for each LAN server (for ping info), keyed by address
     * Should be accessed from main thread
     */
    private val lanServers = hashMapOf<String, ServerData>()

    init {
        startLanDetection()
    }

    @Suppress("unused")
    private val shutdownHandler = handler<ClientShutdownEvent> {
        stopLanDetection()
    }

    private fun startLanDetection() {
        try {
            lanDetector = LanServerDetection.LanServerDetector(lanServerList).apply { start() }
        } catch (exception: Exception) {
            logger.warn("Unable to start LAN server detection", exception)
        }
    }

    private fun stopLanDetection() {
        lanDetector?.interrupt()
        lanDetector = null
        lanServerList.takeDirtyServers()
        lanServers.clear()
    }

    /**
     * Returns the list of currently detected LAN servers with full Server-compatible JSON fields.
     * Mirrors vanilla's updateNetworkServers pattern: takeDirtyServers returns full list → full replacement.
     * Uses negative IDs (sorted by address) to avoid collision with regular server IDs.
     */
    suspend fun getLanServers(): List<JsonObject> {
        // Check for new/updated servers from vanilla detector — returns full list when dirty
        val serverDatas = withContext(Dispatchers.Minecraft) {
            lanServerList.takeDirtyServers()?.let { allServers ->
                // Full replacement: stale servers are naturally removed when takeDirtyServers drops them
                lanServers.clear()
                for (lan in allServers) {
                    lanServers.computeIfAbsent(lan.address) {
                        ServerData(lan.motd, it, ServerData.Type.LAN)
                    }
                }
            }
            lanServers.values.toTypedArray()
        }

        serverDatas.sortBy { it.ip }

        // Ping newly added LAN servers
        serverDatas.forEach {
            if (it.state() == ServerData.State.INITIAL) {
                this.ping(it)
            }
        }

        return serverDatas.mapIndexed { index, serverData ->
            interopGson.toJsonTree(serverData).asJsonObject.apply {
                addProperty("id", -(index + 1))
                addProperty("lan", true)
                addProperty("online", serverData.state() == ServerData.State.SUCCESSFUL ||
                    serverData.state() == ServerData.State.INCOMPATIBLE)
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
        maybeRePingLanServers()
    }

    // Periodic re-ping interval for LAN servers
    private var lastLanPingTime = 0L

    private fun maybeRePingLanServers() {
        val now = System.currentTimeMillis()
        if (now - lastLanPingTime < 30_000L) return
        lastLanPingTime = now

        for (entry in lanServers.values) {
            when (entry.state()) {
                ServerData.State.SUCCESSFUL,
                ServerData.State.INCOMPATIBLE,
                ServerData.State.UNREACHABLE -> {
                    entry.setState(ServerData.State.INITIAL)
                    ping(entry)
                }
                else -> {}
            }
        }
    }

    override val running = true

}

val ServerList.servers: List<ServerData>
    get() = (this as MixinServerListAccessor).`liquid_bounce$getServerList`()

fun ServerList.getByAddress(address: String) = servers.firstOrNull { it.ip == address }

internal fun Route.serverListRoutes() = route("/servers") {
    getServers()
    getLanServers()
    putAddServer()
    deleteServer()
    putEditServer()
    postSwapServers()
    postOrderServers()
    postConnect()
}
