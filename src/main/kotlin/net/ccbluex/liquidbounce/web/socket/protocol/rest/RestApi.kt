/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
 */

package net.ccbluex.liquidbounce.web.socket.protocol.rest

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.ClientApi
import net.ccbluex.liquidbounce.api.IpInfoApi
import net.ccbluex.liquidbounce.features.misc.AccountManager
import net.ccbluex.liquidbounce.utils.client.isPremium
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RouteController
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.option.ServerList
import java.util.*
import kotlin.io.path.absolutePathString

class RestApi {

    fun setupRoutes() {
        RouteController.new("/api/v1/client").apply {
            get("/info") {
                httpOk(JsonObject().apply {
                    addProperty("gameVersion", mc.gameVersion)
                    addProperty("clientVersion", LiquidBounce.clientVersion)
                    addProperty("clientName", LiquidBounce.CLIENT_NAME)
                    addProperty("fps", mc.currentFps)
                    addProperty("gameDir", mc.runDirectory.path)
                })
            }
            get("/exit") {
                mc.scheduleStop()
                httpOk(JsonObject())
            }
            get("/session") {
                httpOk(JsonObject().apply {
                    mc.session.let {
                        addProperty("username", it.username)
                        addProperty("uuid", it.uuid)
                        addProperty("accountType", it.accountType.getName())
                        addProperty("faceUrl", ClientApi.FACE_URL.format(mc.session.uuid))
                        addProperty("premium", it.isPremium())
                    }
                })
            }
            get("/location") {
                httpOk(protocolGson.toJsonTree(
                    IpInfoApi.localIpInfo ?: return@get httpForbidden("location is not known (yet)")
                ))
            }
            get("/virtualScreen") {
                httpOk(JsonObject().apply {
                    addProperty("name", IntegrationHandler.momentaryVirtualScreen?.name)
                })
            }
            get("/accounts") {
                val accounts = JsonArray()
                for ((i, account) in AccountManager.accounts.withIndex()) {
                    accounts.add(JsonObject().apply {
                        // Why are we not serializing the whole account?
                        // -> We do not want to share the access token or anything relevant

                        addProperty("id", i)
                        addProperty("username", account.name)
                        // Be careful with this, it will require a session refresh which takes time
                        // addProperty("uuid", account.session.uuid)
                        // addProperty("faceUrl", ClientApi.FACE_URL.format(account.session.uuid))
                        addProperty("type", account.type)
                    })
                }
                httpOk(accounts)
            }
            get("/worlds") {
                val worlds = JsonArray()

                runCatching {
                    val levelList = mc.levelStorage.levelList
                    if (levelList.isEmpty) {
                        return@get httpOk(worlds)
                    }

                    val summaries = mc.levelStorage.loadSummaries(levelList).get()

                    for (summary in summaries) {
                        worlds.add(JsonObject().apply {
                            addProperty("name", summary.name)
                            addProperty("displayName", summary.displayName)
                            addProperty("lastPlayed", summary.lastPlayed)
                            addProperty("gameMode", summary.levelInfo.gameMode.getName())
                            addProperty("difficulty", summary.levelInfo.difficulty.getName())
                            addProperty("icon", summary.iconPath.absolutePathString())
                            addProperty("hardcore", summary.levelInfo.isHardcore)
                            addProperty("commandsAllowed", summary.levelInfo.areCommandsAllowed())
                        })
                    }
                    httpOk(worlds)
                }.getOrElse {httpInternalServerError("Failed to get worlds due to ${it.message}") }
            }
            get("/servers") {
                val servers = JsonArray()

                runCatching {
                    // TODO: Cache server list until refresh occours and also make it request online status
                    val serverList = ServerList(mc)
                    serverList.loadFile()

                    for (i in 0 until serverList.size()) {
                        val server = serverList.get(i)

                        servers.add(JsonObject().apply {
                            addProperty("name", server.name)
                            addProperty("address", server.address)
                            addProperty("online", server.online)
                            add("playerList", protocolGson.toJsonTree(server.playerListSummary))
                            add("label", protocolGson.toJsonTree(server.label))
                            add("playerCountLabel", protocolGson.toJsonTree(server.playerCountLabel))
                            add("version", protocolGson.toJsonTree(server.version))
                            addProperty("protocolVersion", server.protocolVersion)
                            add("players", JsonObject().apply {
                                addProperty("max", server.players?.max)
                                addProperty("online", server.players?.online)
                            })

                            server.favicon?.let {
                                addProperty("icon", Base64.getEncoder().encodeToString(it))
                            }
                        })
                    }

                    httpOk(servers)
                }.getOrElse { httpInternalServerError("Failed to get servers due to ${it.message}") }
            }
        }
    }

}
