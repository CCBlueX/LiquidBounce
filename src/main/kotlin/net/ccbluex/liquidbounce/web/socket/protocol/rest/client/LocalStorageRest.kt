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

package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.web.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

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
fun RestNode.localStorageRest() {
    get("/localStorage") {
        val key = it.params["key"] ?: return@get httpForbidden("No key")
        val value = PersistentLocalStorage.getItem(key) ?: return@get httpForbidden("No value for key $key")

        httpOk(JsonObject().apply {
            addProperty("value", value)
        })
    }

    put("/localStorage") {
        val body = decode<JsonObject>(it.content)
        val key = body["key"]?.asString ?: return@put httpForbidden("No key")
        val value = body["value"]?.asString ?: return@put httpForbidden("No value")

        PersistentLocalStorage.setItem(key, value)
        httpOk(JsonObject())
    }

    delete("/localStorage") {
        val key = it.params["key"] ?: return@delete httpForbidden("No key")
        PersistentLocalStorage.removeItem(key)
        httpOk(JsonObject())
    }

    // PUT and GET whole localStorage
    get("/localStorage/all") {
        httpOk(JsonObject().apply {
            val jsonArray = JsonArray()

            PersistentLocalStorage.map.forEach { (key, value) ->
                jsonArray.add(JsonObject().apply {
                    addProperty("key", key)
                    addProperty("value", value)
                })
            }

            add("items", jsonArray)
        })
    }

    put("/localStorage/all") {
        data class Item(val key: String, val value: String)
        data class StoragePutRequest(val items: List<Item>)

        val body = decode<StoragePutRequest>(it.content)

        PersistentLocalStorage.clear()
        body.items.forEach { item ->
            PersistentLocalStorage.setItem(item.key, item.value)
        }

        httpOk(JsonObject())
    }
}

