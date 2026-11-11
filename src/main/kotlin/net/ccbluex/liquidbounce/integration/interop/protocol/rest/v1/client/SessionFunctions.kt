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

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import io.ktor.http.HttpStatusCode.Companion.NoContent
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ccbluex.liquidbounce.api.thirdparty.IpInfoApi
import net.ccbluex.liquidbounce.features.account.AccountManager
import net.ccbluex.liquidbounce.features.client.Client
import net.ccbluex.liquidbounce.features.client.ClientContext
import net.ccbluex.liquidbounce.features.client.ClientManager
import net.ccbluex.liquidbounce.features.client.ClientOverview
import net.ccbluex.liquidbounce.integration.interop.badRequest
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.notFound
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.client.multiplayer.resolver.ServerAddress

// GET /api/v1/client/session
private fun Route.getSessionInfo() = get("/session") { call.respond(mc.user) }

// GET /api/v1/client/location
private fun Route.getLocationInfo() = get("/location") {
    val locationInfo = IpInfoApi.current ?: call.forbidden("Location is not known")
    call.respond(locationInfo)
}

// GET /api/v1/client/clients
private fun Route.getClients() = get {
    call.respond(withContext(Dispatchers.Main) { ClientOverview.snapshots() })
}

/**
 * Opens a client for an account of the alt manager. It joins the given server, or the one the active
 * client is on. With no server to join it takes the window, so that the menus can send it somewhere.
 */
// POST /api/v1/client/clients
private fun Route.postClient() = post {
    data class ClientForm(val account: Int, val address: String?)

    val form = call.receive<ClientForm>()
    val account = AccountManager.accounts.getOrNull(form.account) ?: call.badRequest("Account not found")
    if (form.address != null && !ServerAddress.isValidAddress(form.address)) {
        call.badRequest("Invalid server address")
    }

    val login = withContext(Dispatchers.IO) { AccountManager.authenticate(account) }
    withContext(Dispatchers.Main) {
        val address = form.address ?: ClientContext.current.connection?.serverData?.ip
        val client = ClientManager.open(login)
        if (address != null) {
            ClientManager.connect(client, address)
        } else {
            ClientManager.activate(client)
        }
    }
    call.respond(NoContent)
}

// POST /api/v1/client/clients/{id}/activate
private fun Route.postActivateClient() = post("/{id}/activate") {
    ClientManager.activate(call.client())
    call.respond(NoContent)
}

// DELETE /api/v1/client/clients/{id}
private fun Route.deleteClient() = delete("/{id}") {
    val client = call.client()
    if (client.isMain) {
        call.forbidden("The main client cannot be closed")
    }

    ClientManager.close(client)
    call.respond(NoContent)
}

private suspend fun RoutingCall.client(): Client {
    val id = parameters["id"]?.toIntOrNull() ?: badRequest("Missing client id")
    return ClientManager.byId(id) ?: notFound("/clients/$id", "Client not found")
}

internal fun Route.sessionRoutes() {
    getSessionInfo()
    getLocationInfo()

    route("/clients") {
        getClients()
        postClient()
        postActivateClient()
        deleteClient()
    }
}
