/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.game

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.features.Reconnect
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpInternalServerError
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import net.minecraft.client.option.ServerList
import java.util.*

internal fun RestNode.setupServerApi() {
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

    post("/reconnect") {
        Reconnect.reconnectNow()
        httpOk(JsonObject())
    }

    post("/reconnectWithRandomAccount") {
        Reconnect.reconnectWithRandomAccount()
        httpOk(JsonObject())
    }

    post("/reconnectWithRandomUsername") {
        Reconnect.reconnectWithRandomUsername()
        httpOk(JsonObject())
    }
}
