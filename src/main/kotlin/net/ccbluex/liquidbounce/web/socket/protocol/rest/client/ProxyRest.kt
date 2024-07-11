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
import com.mojang.blaze3d.systems.RenderSystem
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode
import net.ccbluex.liquidbounce.web.socket.protocol.protocolGson
import org.lwjgl.glfw.GLFW

/**
 * Proxy endpoints
 */
internal fun RestNode.proxyRest() {
    get("/proxy") {
        val proxyObject = ProxyManager.currentProxy?.let { proxy ->
            protocolGson.toJsonTree(proxy).asJsonObject.apply {
                addProperty("id", ProxyManager.proxies.indexOf(proxy))
            }
        } ?: JsonObject()

        httpOk(proxyObject)
    }

    post("/proxy") {
        data class ProxyRequest(val id: Int)
        val body = decode<ProxyRequest>(it.content)

        if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
            return@post httpForbidden("Invalid id")
        }

        ProxyManager.setProxy(body.id)
        httpOk(JsonObject())
    }

    delete("/proxy") {
        ProxyManager.unsetProxy()
        httpOk(JsonObject())
    }

    get("/proxies") {
        val proxiesArray = JsonArray()

        ProxyManager.proxies.forEachIndexed { index, proxy ->
            proxiesArray.add(protocolGson.toJsonTree(proxy).asJsonObject.apply {
                addProperty("id", index)
            })
        }

        httpOk(proxiesArray)
    }.apply {
        post("/add") {
            data class ProxyRequest(val host: String, val port: Int, val username: String, val password: String)

            val body = decode<ProxyRequest>(it.content)

            if (body.host.isBlank()) {
                return@post httpForbidden("No host")
            }

            if (body.port <= 0) {
                return@post httpForbidden("No port")
            }

            ProxyManager.addProxy(body.host, body.port, body.username, body.password)
            httpOk(JsonObject())
        }.apply {
            post("/clipboard") {
                RenderSystem.recordRenderCall {
                    runCatching {
                        // Get clipboard content via GLFW
                        val clipboard = GLFW.glfwGetClipboardString(mc.window.handle) ?: ""

                        if (clipboard.isNotBlank()) {
                            val split = clipboard.split(":")
                            val host = split[0]
                            val port = split[1].toInt()

                            if (split.size > 2) {
                                val username = split[2]
                                val password = split[3]
                                ProxyManager.addProxy(host, port, username, password)
                            } else {
                                ProxyManager.addProxy(host, port, "", "")
                            }
                        }
                    }
                }

                httpOk(JsonObject())
            }
        }

        post("/edit") {
            data class ProxyRequest(
                val id: Int,
                val host: String,
                val port: Int,
                val username: String,
                val password: String)
            val body = decode<ProxyRequest>(it.content)

            if (body.host.isBlank()) {
                return@post httpForbidden("No host")
            }

            if (body.port <= 0) {
                return@post httpForbidden("No port")
            }

            ProxyManager.editProxy(body.id, body.host, body.port, body.username, body.password)
            httpOk(JsonObject())
        }

        post("/check") {
            data class ProxyRequest(val id: Int)
            val body = decode<ProxyRequest>(it.content)

            if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
                return@post httpForbidden("Invalid id")
            }

            ProxyManager.checkProxy(body.id)
            httpOk(JsonObject())
        }

        delete("/remove") {
            data class ProxyRequest(val id: Int)
            val body = decode<ProxyRequest>(it.content)

            if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
                return@delete httpForbidden("Invalid id")
            }

            ProxyManager.removeProxy(body.id)
            httpOk(JsonObject())
        }

        put("/favorite") {
            data class ProxyRequest(val id: Int)
            val body = decode<ProxyRequest>(it.content)

            if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
                return@put httpForbidden("Invalid id")
            }

            ProxyManager.favoriteProxy(body.id)
            httpOk(JsonObject())
        }

        delete("/favorite") {
            data class ProxyRequest(val id: Int)
            val body = decode<ProxyRequest>(it.content)

            if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
                return@delete httpForbidden("Invalid id")
            }

            ProxyManager.unfavoriteProxy(body.id)
            httpOk(JsonObject())
        }
    }

}
