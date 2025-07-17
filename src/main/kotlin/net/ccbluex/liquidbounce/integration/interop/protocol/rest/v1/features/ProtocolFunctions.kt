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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.utils.client.defaultProtocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersions
import net.ccbluex.liquidbounce.utils.client.selectProtocolVersion
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk

// GET /api/v1/protocols
@Suppress("UNUSED_PARAMETER")
fun getProtocols(requestObject: RequestObject) = httpOk(JsonArray().apply {
    for (protocol in protocolVersions) {
        add(JsonObject().apply {
            addProperty("name", protocol.name)
            addProperty("version", protocol.version)
        })
    }
})

// GET /api/v1/protocols/protocol
@Suppress("UNUSED_PARAMETER")
fun getProtocol(requestObject: RequestObject) = httpOk(JsonObject().apply {
    addProperty("name", protocolVersion.name)
    addProperty("version", protocolVersion.version)
})

// PUT /api/v1/protocols/protocol
fun putProtocol(requestObject: RequestObject): FullHttpResponse {
    data class ProtocolRequest(val version: Int)
    val protocolRequest = requestObject.asJson<ProtocolRequest>()

    selectProtocolVersion(protocolRequest.version)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/protocols/protocol
@Suppress("UNUSED_PARAMETER")
fun deleteProtocol(requestObject: RequestObject): FullHttpResponse {
    selectProtocolVersion(defaultProtocolVersion.version)
    return httpOk(emptyJsonObject())
}
