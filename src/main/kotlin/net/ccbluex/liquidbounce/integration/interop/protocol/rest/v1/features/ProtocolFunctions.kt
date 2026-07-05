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

import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.utils.client.defaultProtocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersion
import net.ccbluex.liquidbounce.utils.client.protocolVersions
import net.ccbluex.liquidbounce.utils.client.selectProtocolVersion

// GET /api/v1/protocols
private fun Route.getProtocols() = get { call.respond(protocolVersions) }

// GET /api/v1/protocols/protocol
private fun Route.getProtocol() = get { call.respond(protocolVersion) }

// PUT /api/v1/protocols/protocol
private fun Route.putProtocol() = put {
    data class ProtocolRequest(val version: Int)

    val protocolRequest = call.receive<ProtocolRequest>()

    selectProtocolVersion(protocolRequest.version)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/protocols/protocol
private fun Route.deleteProtocol() = delete {
    selectProtocolVersion(defaultProtocolVersion.version)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

internal fun Route.protocolRoutes() = route("/protocols") {
    getProtocols()
    route("/protocol") {
        getProtocol()
        putProtocol()
        deleteProtocol()
    }
}
