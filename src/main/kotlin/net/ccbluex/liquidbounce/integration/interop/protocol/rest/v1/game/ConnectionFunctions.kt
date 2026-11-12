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

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import net.ccbluex.liquidbounce.additions.connectionDetails
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.hideSensitiveAddress
import net.minecraft.client.gui.screens.ConnectScreen
import net.minecraft.client.multiplayer.resolver.ServerAddress
import net.minecraft.network.Connection
import java.net.InetSocketAddress
import java.net.SocketAddress

// GET /api/v1/client/connection
private fun Route.getConnectionDetails() = get("/connection") {
    val details = (mc.gui.screen() as? ConnectScreen)?.connectionDetails
    if (details != null) {
        call.respond(details)
    } else {
        call.respond(HttpStatusCode.NoContent)
    }
}

internal fun Route.connectionRoutes() {
    getConnectionDetails()
}

/**
 * The route of a connection being established: the socket it connects to (the proxy when [proxy] is set) and the
 * server.
 */
@JvmRecord
data class ConnectionDetails(
    val remoteAddress: String,
    val serverAddress: String,
    val proxy: Boolean,
) {
    companion object {
        @JvmStatic
        fun of(connection: Connection, serverAddress: ServerAddress) = ConnectionDetails(
            remoteAddress = connection.remoteAddress.describe(serverAddress.host),
            serverAddress = "${serverAddress.host.hideSensitiveAddress()}:${serverAddress.port}",
            proxy = ProxyManager.currentProxy != null,
        )
    }
}

// The host string stays unredacted, it is usually not sensitive
private fun SocketAddress?.describe(serverHost: String): String {
    if (this !is InetSocketAddress) {
        return "<unknown>"
    }

    val ip = if (isUnresolved) "<unresolved>" else address.hostAddress
    return if (hostString == serverHost || hostString == ip) "$ip:$port" else "$hostString/$ip:$port"
}
