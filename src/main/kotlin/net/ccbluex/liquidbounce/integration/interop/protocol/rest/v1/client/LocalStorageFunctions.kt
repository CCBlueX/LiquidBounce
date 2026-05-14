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
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.integration.interop.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client.LocalStorageData.Item
import net.ccbluex.netty.http.routing.Routing

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
private fun Routing.getLocalStorage() = get {
    val key = call.queryParameters["key"] ?: call.forbidden("No key")
    val value = PersistentLocalStorage.map[key] ?: call.forbidden("No value for key $key")

    call.respond(JsonObject().apply {
        addProperty("value", value)
    })
}

// PUT /api/v1/client/localStorage
private fun Routing.putLocalStorage() = put {
    val payload = call.receive<JsonObject>()
    val key = payload["key"]?.asString ?: call.forbidden("No key")
    val value = payload["value"]?.asString ?: call.forbidden("No value")

    PersistentLocalStorage.map[key] = value
    call.respondNoContent()
}

// DELETE /api/v1/client/localStorage
private fun Routing.deleteLocalStorage() = delete {
    val key = call.queryParameters["key"] ?: call.forbidden("No key")
    PersistentLocalStorage.map.remove(key)
    call.respondNoContent()
}

// GET /api/v1/client/localStorage/all
private fun Routing.getAllLocalStorage() = get {
    call.respond(LocalStorageData(PersistentLocalStorage.map.map { (k, v) -> Item(k, v) }), interopGson)
}

// PUT /api/v1/client/localStorage/all
private fun Routing.putAllLocalStorage() = put {
    val payload = call.receive<LocalStorageData>()

    PersistentLocalStorage.map.clear()
    payload.items.forEach { item ->
        PersistentLocalStorage.map[item.key] = item.value
    }

    call.respondNoContent()
}

internal fun Routing.localStorageRoutes() = route("/localStorage") {
    getLocalStorage()
    putLocalStorage()
    deleteLocalStorage()
    route("/all") {
        getAllLocalStorage()
        putAllLocalStorage()
    }
}
