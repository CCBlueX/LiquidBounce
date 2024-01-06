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
package net.ccbluex.liquidbounce.web.socket.protocol.rest.client

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.persistant.PersistentLocalStorage
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.minecraft.client.gui.screen.SplashOverlay
import net.minecraft.registry.Registries
import net.minecraft.util.Util

internal fun RestNode.setupClientRestApi() {
    get("/info") {
        httpOk(JsonObject().apply {
            addProperty("gameVersion", mc.gameVersion)
            addProperty("clientVersion", LiquidBounce.clientVersion)
            addProperty("clientName", LiquidBounce.CLIENT_NAME)
            addProperty("fps", mc.currentFps)
            addProperty("gameDir", mc.runDirectory.path)
        })
    }

    get("/update") {
        httpOk(JsonObject().apply {
            addProperty("updateAvailable", LiquidBounce.updateAvailable)
            addProperty("commit", LiquidBounce.clientCommit)
        })
    }

    get("/exit") {
        mc.scheduleStop()
        httpOk(JsonObject())
    }

    get("/virtualScreen") {
        httpOk(JsonObject().apply {
            addProperty("name", IntegrationHandler.momentaryVirtualScreen?.name)
            addProperty("splash", mc.overlay is SplashOverlay)
        })
    }

    get("/window") {
        httpOk(JsonObject().apply {
            addProperty("width", mc.window.width)
            addProperty("height", mc.window.height)
            addProperty("scaledWidth", mc.window.scaledWidth)
            addProperty("scaledHeight", mc.window.scaledHeight)
        })
    }

    post("/browse") {
        val body = decode<JsonObject>(it.content)
        val url = body["url"]?.asString ?: return@post httpForbidden("No url")
        Util.getOperatingSystem().open(url)
        httpOk(JsonObject())
    }

    setupScreenRestApi()
    blocksAndItemRestApi()
    setupLocalStorage()
}

private fun RestNode.setupLocalStorage() {
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


private fun RestNode.setupScreenRestApi() {
    get("/screen") {
        val mcScreen = mc.currentScreen ?: return@get httpForbidden("No screen")
        val name = IntegrationHandler.VirtualScreenType.values().find { it.recognizer(mcScreen) }?.assignedName
            ?: mcScreen::class.qualifiedName

        httpOk(JsonObject().apply {
            addProperty("name", name)
        })
    }.apply {
        get("/size") {
            httpOk(JsonObject().apply {
                addProperty("width", mc.window.scaledWidth)
                addProperty("height", mc.window.scaledHeight)
            })
        }
    }

    put("/screen") {
        val body = decode<JsonObject>(it.content)
        val screenName = body["name"]?.asString ?: return@put httpForbidden("No screen name")

        IntegrationHandler.VirtualScreenType.values().find { it.assignedName == screenName }?.open()
            ?: return@put httpForbidden("No screen with name $screenName")
        httpOk(JsonObject())
    }

}

private fun RestNode.blocksAndItemRestApi() {
    get("/blocks") {
        val jsonArray = JsonArray()

        Registries.BLOCK.forEach { block ->
            jsonArray.add(block.translationKey)
        }

        httpOk(jsonArray)
    }

    get("/items") {
        httpOk(JsonObject().apply {
            val jsonArray = JsonArray()

            Registries.ITEM.forEach { item ->
                jsonArray.add(item.translationKey)
            }

            httpOk(jsonArray)
        })
    }
}
