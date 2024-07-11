/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.features

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.defaultProtocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersions
import net.ccbluex.liquidbounce.utils.client.selectProtocolVersion
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

fun RestNode.protocolRest() {
    get("/protocols") {
        val jsonArray = JsonArray()

        for (protocol in protocolVersions) {
            jsonArray.add(JsonObject().apply {
                addProperty("name", protocol.name)
                addProperty("version", protocol.version)
            })
        }

        httpOk(jsonArray)
    }.apply {
        get("/protocol") {
            httpOk(JsonObject().apply {
                addProperty("name", protocolVersion.name)
                addProperty("version", protocolVersion.version)
            })
        }

        put("/protocol") {
            data class ProtocolRequest(val version: Int)
            val protocolRequest = decode<ProtocolRequest>(it.content)

            selectProtocolVersion(protocolRequest.version)
            httpOk(JsonObject())
        }

        delete("/protocol") {
            selectProtocolVersion(defaultProtocolVersion.version)
            httpOk(JsonObject())
        }
    }
}
