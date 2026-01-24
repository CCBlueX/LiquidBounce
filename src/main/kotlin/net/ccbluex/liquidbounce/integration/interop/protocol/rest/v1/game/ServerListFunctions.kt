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
import io.netty.handler.codec.http.FullHttpResponse
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
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpInternalServerError
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
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
import java.net.UnknownHostException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Future

// GET /api/v1/client/servers
@Suppress("UNUSED_PARAMETER")
fun getServers(requestObject: RequestObject) = runCatching {
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

    httpOk(servers)
}.getOrElse { httpInternalServerError("Failed to get servers due to ${it.message}") }

// POST /api/v1/client/servers/connect
@Suppress("UNUSED_PARAMETER")
fun postConnect(requestObject: RequestObject): FullHttpResponse {
    data class ServerConnectRequest(val address: String)

    val serverConnectRequest = requestObject.asJson<ServerConnectRequest>()
    val serverInfo = serverList.getByAddress(serverConnectRequest.address)
        ?: ServerData("Unknown Server", serverConnectRequest.address, ServerData.Type.OTHER)

    val serverAddress = ServerAddress.parseString(serverInfo.ip)

    mc.execute {
        ConnectScreen.startConnecting(JoinMultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo, false, null)
    }
    return httpNoContent()
}

// PUT /api/v1/client/servers/add
@Suppress("UNUSED_PARAMETER")
fun putAddServer(requestObject: RequestObject): FullHttpResponse {
    data class ServerAddRequest(val name: String, val address: String, val resourcePackPolicy: String? = null)

    val serverAddRequest = requestObject.asJson<ServerAddRequest>()

    if (!ServerAddress.isValidAddress(serverAddRequest.address)) {
        return httpForbidden("Invalid address")
    }

    val serverInfo = ServerData(serverAddRequest.name, serverAddRequest.address, ServerData.Type.OTHER)
    serverAddRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackStatus = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ServerPackStatus.PROMPT
    }

    serverList.add(serverInfo, false)
    serverList.save()

    return httpNoContent()
}

// DELETE /api/v1/client/servers/remove
@Suppress("UNUSED_PARAMETER")
fun deleteServer(requestObject: RequestObject): FullHttpResponse {
    data class ServerRemoveRequest(val id: Int)

    val serverRemoveRequest = requestObject.asJson<ServerRemoveRequest>()
    val serverInfo = serverList.get(serverRemoveRequest.id)

    serverList.remove(serverInfo)
    serverList.save()

    return httpNoContent()
}

// PUT /api/v1/client/servers/edit
@Suppress("UNUSED_PARAMETER")
fun putEditServer(requestObject: RequestObject): FullHttpResponse {
    data class ServerEditRequest(
        val id: Int,
        val name: String,
        val address: String,
        val resourcePackPolicy: String? = null
    )

    val serverEditRequest = requestObject.asJson<ServerEditRequest>()
    val serverInfo = serverList.get(serverEditRequest.id)

    serverInfo.name = serverEditRequest.name
    serverInfo.ip = serverEditRequest.address
    serverEditRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackStatus = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ServerPackStatus.PROMPT
    }
    serverList.save()

    return httpNoContent()
}

// POST /api/v1/client/servers/swap
@Suppress("UNUSED_PARAMETER")
fun postSwapServers(requestObject: RequestObject): FullHttpResponse {
    data class ServerSwapRequest(val from: Int, val to: Int)

    val serverSwapRequest = requestObject.asJson<ServerSwapRequest>()

    serverList.swap(serverSwapRequest.from, serverSwapRequest.to)
    serverList.save()
    return httpNoContent()
}

// POST /api/v1/client/servers/order
@Suppress("UNUSED_PARAMETER")
fun postOrderServers(requestObject: RequestObject): FullHttpResponse {
    data class ServerOrderRequest(val order: List<Int>)

    val serverOrderRequest = requestObject.asJson<ServerOrderRequest>()

    serverOrderRequest.order.map { serverList.get(it) }
        .forEachIndexed { index, serverInfo ->
            serverList.replace(index, serverInfo)
        }
    serverList.save()

    return httpNoContent()
}

object ActiveServerList : EventListener {

    internal val serverList = ServerList(mc).apply { load() }

    private val serverListPinger = ServerStatusPinger()
    private val cannotConnectText = Component.translatable("multiplayer.status.cannot_connect")
        .withColor(CommonColors.RED)
    private val cannotResolveText = Component.translatable("multiplayer.status.cannot_resolve")
        .withColor(CommonColors.RED)

    private val pingTasks = mutableListOf<Future<*>>()

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
