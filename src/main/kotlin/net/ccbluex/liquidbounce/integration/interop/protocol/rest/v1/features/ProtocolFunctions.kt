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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.features

import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.utils.client.defaultProtocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersions
import net.ccbluex.liquidbounce.utils.client.selectProtocolVersion
import net.ccbluex.netty.http.routing.Routing

// GET /api/v1/protocols
private fun Routing.getProtocols() = get { call.respond(protocolVersions, interopGson) }

// GET /api/v1/protocols/protocol
private fun Routing.getProtocol() = get { call.respond(protocolVersion, interopGson) }

// PUT /api/v1/protocols/protocol
private fun Routing.putProtocol() = put {
    data class ProtocolRequest(val version: Int)

    val protocolRequest = call.receive<ProtocolRequest>()

    selectProtocolVersion(protocolRequest.version)
    call.respondNoContent()
}

// DELETE /api/v1/protocols/protocol
private fun Routing.deleteProtocol() = delete {
    selectProtocolVersion(defaultProtocolVersion.version)
    call.respondNoContent()
}

internal fun Routing.protocolRoutes() = route("/protocols") {
    getProtocols()
    route("/protocol") {
        getProtocol()
        putProtocol()
        deleteProtocol()
    }
}
