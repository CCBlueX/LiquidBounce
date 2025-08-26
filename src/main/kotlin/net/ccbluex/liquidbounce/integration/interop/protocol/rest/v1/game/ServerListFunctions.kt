/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.game

import com.google.gson.JsonArray
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.serializer.minecraft.ResourcePolicy
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
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
import net.ccbluex.netty.http.util.httpOk
import net.minecraft.SharedConstants
import net.minecraft.client.gui.screen.TitleScreen
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen
import net.minecraft.client.network.MultiplayerServerListPinger
import net.minecraft.client.network.ServerAddress
import net.minecraft.client.network.ServerInfo
import net.minecraft.client.network.ServerInfo.ResourcePackPolicy
import net.minecraft.client.option.ServerList
import net.minecraft.screen.ScreenTexts
import net.minecraft.text.Text
import net.minecraft.util.Colors
import net.minecraft.util.Util
import java.net.UnknownHostException
import java.util.concurrent.Future

// GET /api/v1/client/servers
@Suppress("UNUSED_PARAMETER")
fun getServers(requestObject: RequestObject) = runCatching {
    serverList.loadFile()
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
        ?: ServerInfo("Unknown Server", serverConnectRequest.address, ServerInfo.ServerType.OTHER)

    val serverAddress = ServerAddress.parse(serverInfo.address)

    RenderSystem.recordRenderCall {
        ConnectScreen.connect(MultiplayerScreen(TitleScreen()), mc, serverAddress, serverInfo, false, null)
    }
    return httpOk(emptyJsonObject())
}

// PUT /api/v1/client/servers/add
@Suppress("UNUSED_PARAMETER")
fun putAddServer(requestObject: RequestObject): FullHttpResponse {
    data class ServerAddRequest(val name: String, val address: String, val resourcePackPolicy: String? = null)
    val serverAddRequest = requestObject.asJson<ServerAddRequest>()

    if (!ServerAddress.isValid(serverAddRequest.address)) {
        return httpForbidden("Invalid address")
    }

    val serverInfo = ServerInfo(serverAddRequest.name, serverAddRequest.address, ServerInfo.ServerType.OTHER)
    serverAddRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackPolicy = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ResourcePackPolicy.PROMPT
    }

    serverList.add(serverInfo, false)
    serverList.saveFile()

    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/servers/remove
@Suppress("UNUSED_PARAMETER")
fun deleteServer(requestObject: RequestObject): FullHttpResponse {
    data class ServerRemoveRequest(val id: Int)
    val serverRemoveRequest = requestObject.asJson<ServerRemoveRequest>()
    val serverInfo = serverList.get(serverRemoveRequest.id)

    serverList.remove(serverInfo)
    serverList.saveFile()

    return httpOk(emptyJsonObject())
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
    serverInfo.address = serverEditRequest.address
    serverEditRequest.resourcePackPolicy?.let {
        serverInfo.resourcePackPolicy = ResourcePolicy.fromString(it)?.toMinecraftPolicy() ?: ResourcePackPolicy.PROMPT
    }
    serverList.saveFile()

    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/servers/swap
@Suppress("UNUSED_PARAMETER")
fun postSwapServers(requestObject: RequestObject): FullHttpResponse {
    data class ServerSwapRequest(val from: Int, val to: Int)
    val serverSwapRequest = requestObject.asJson<ServerSwapRequest>()

    serverList.swapEntries(serverSwapRequest.from, serverSwapRequest.to)
    serverList.saveFile()
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/servers/order
@Suppress("UNUSED_PARAMETER")
fun postOrderServers(requestObject: RequestObject): FullHttpResponse {
    data class ServerOrderRequest(val order: List<Int>)
    val serverOrderRequest = requestObject.asJson<ServerOrderRequest>()

    serverOrderRequest.order.map { serverList.get(it) }
        .forEachIndexed { index, serverInfo ->
            serverList.set(index, serverInfo)
        }
    serverList.saveFile()

    return httpOk(emptyJsonObject())
}

object ActiveServerList : EventListener {

    internal val serverList = ServerList(mc).apply { loadFile() }

    private val serverListPinger = MultiplayerServerListPinger()
    private val cannotConnectText = Text.translatable("multiplayer.status.cannot_connect")
        .withColor(Colors.RED)
    private val cannotResolveText = Text.translatable("multiplayer.status.cannot_resolve")
        .withColor(Colors.RED)

    private val pingTasks = mutableListOf<Future<*>>()

    private fun cancelTasks() {
        pingTasks.forEach { it.cancel(true) }
        pingTasks.clear()
    }

    internal fun pingThemAll() {
        cancelTasks()
        serverList.servers
            .distinctBy { it.address } // We do not want to ping the same server multiple times
            .forEach(this::ping)
    }

    @Suppress("unused")
    private val screenHandler = handler<ScreenEvent> {
        cancelTasks()
    }

    fun ping(serverEntry: ServerInfo) {
        if (serverEntry.status != ServerInfo.Status.INITIAL) {
            return
        }

        serverEntry.status = ServerInfo.Status.PINGING
        serverEntry.label = ScreenTexts.EMPTY
        serverEntry.playerCountLabel = ScreenTexts.EMPTY

        pingTasks += Util.getDownloadWorkerExecutor().service.submit {
            try {
                serverListPinger.add(serverEntry, { mc.execute(serverList::saveFile) }) {
                    serverEntry.status =
                        if (serverEntry.protocolVersion == SharedConstants.getGameVersion().protocolVersion) {
                            ServerInfo.Status.SUCCESSFUL
                        } else {
                            ServerInfo.Status.INCOMPATIBLE
                        }
                }
            } catch (unknownHostException: UnknownHostException) {
                serverEntry.status = ServerInfo.Status.UNREACHABLE
                serverEntry.label = cannotResolveText
                logger.error("Failed to ping server ${serverEntry.name} due to ${unknownHostException.message}")
            } catch (exception: Exception) {
                serverEntry.status = ServerInfo.Status.UNREACHABLE
                serverEntry.label = cannotConnectText
                logger.error("Failed to ping server ${serverEntry.name}", exception)
            }
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        serverListPinger.tick()
    }

    override val running = true

}

val ServerList.servers: List<ServerInfo>
    get() = (this as MixinServerListAccessor).`liquid_bounce$getServers`()

fun ServerList.getByAddress(address: String) = servers.firstOrNull { it.address == address }
