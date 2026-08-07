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

import com.google.gson.JsonObject
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.LocalStorageData.Item

/**
 * LocalStorage RestAPI
 *
 * Allows to persist data across different browser.
 *
 * Since we cannot rely on the browser's localStorage
 * we have to implement our own. This is a simple key-value store.
 *
 * Especially because we have not enabled the CEF local storage
 */

private data class LocalStorageData(val items: List<Item>) {
    data class Item(val key: String, val value: String)
}

// GET /api/v1/client/localStorage
private fun Route.getLocalStorage() = get {
    val key = call.queryParameters["key"] ?: call.forbidden("No key")
    val value = PersistentLocalStorage.map[key] ?: call.forbidden("No value for key $key")

    call.respond(JsonObject().apply {
        addProperty("value", value)
    })
}

// PUT /api/v1/client/localStorage
private fun Route.putLocalStorage() = put {
    val payload = call.receive<JsonObject>()
    val key = payload["key"]?.asString ?: call.forbidden("No key")
    val value = payload["value"]?.asString ?: call.forbidden("No value")

    PersistentLocalStorage.map[key] = value
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/localStorage
private fun Route.deleteLocalStorage() = delete {
    val key = call.queryParameters["key"] ?: call.forbidden("No key")
    PersistentLocalStorage.map.remove(key)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// GET /api/v1/client/localStorage/all
private fun Route.getAllLocalStorage() = get {
    call.respond(LocalStorageData(PersistentLocalStorage.map.map { (k, v) -> Item(k, v) }))
}

// PUT /api/v1/client/localStorage/all
private fun Route.putAllLocalStorage() = put {
    val payload = call.receive<LocalStorageData>()

    PersistentLocalStorage.map.clear()
    payload.items.forEach { item ->
        PersistentLocalStorage.map[item.key] = item.value
    }

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

internal fun Route.localStorageRoutes() = route("/localStorage") {
    getLocalStorage()
    putLocalStorage()
    deleteLocalStorage()
    route("/all") {
        getAllLocalStorage()
        putAllLocalStorage()
    }
}
