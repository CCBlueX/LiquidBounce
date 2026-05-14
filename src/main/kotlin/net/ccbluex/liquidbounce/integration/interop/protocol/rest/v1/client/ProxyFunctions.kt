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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.routing.Routing
import org.lwjgl.glfw.GLFW

/**
 * Proxy endpoints
 */

// GET /api/v1/client/proxy
private fun Routing.getProxyInfo() = get {
    ProxyManager.currentProxy?.let { proxy ->
        call.respond(interopGson.toJsonTree(proxy).asJsonObject.apply {
            addProperty("id", ProxyManager.proxies.indexOf(proxy))
        })
    } ?: call.respondNoContent()
}

// POST /api/v1/client/proxy
private fun Routing.postProxy() = post {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id !in ProxyManager.proxies.indices) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxy = ProxyManager.proxies[body.id]
    call.respondNoContent()
}

// DELETE /api/v1/client/proxy
private fun Routing.deleteProxy() = delete {
    ProxyManager.proxy = Proxy.NONE
    call.respondNoContent()
}

// GET /api/v1/client/proxies
private fun Routing.getProxies() = get {
    call.respond(JsonArray().apply {
        ProxyManager.proxies.forEachIndexed { index, proxy ->
            add(interopGson.toJsonTree(proxy).asJsonObject.apply {
                addProperty("id", index)
                addProperty("type", (proxy.type ?: Proxy.Type.SOCKS5).toString())
            })
        }
    })
}

// POST /api/v1/client/proxies/add
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun Routing.postAddProxy() = post {
    data class ProxyRequest(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val type: Proxy.Type,
        val forwardAuthentication: Boolean
    )
    val (host, port, username, password, type, forwardAuthentication) = call.receive<ProxyRequest>()

    if (host.isBlank()) {
        call.forbidden("No host")
    }

    if (port !in 0..65535) {
        call.forbidden("Illegal port")
    }

    ProxyManager.validateProxy(Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication))
    call.respondNoContent()
}

// POST /api/v1/client/proxies/add/clipboard
private fun Routing.postClipboardProxy() = post("/clipboard") {
    mc.execute {
        try {
            val clipboardText = GLFW.glfwGetClipboardString(mc.window.handle())
            if (clipboardText.isNullOrBlank()) {
                return@execute
            }

            val proxy = try {
                Proxy.parse(clipboardText.trim())
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Invalid proxy format. Expected format: host:port:username:password or host:port",
                    e
                )
            }

            ProxyManager.validateProxy(proxy)
        } catch (e: Exception) {
            logger.error("Failed to add proxy from clipboard.", e)
            EventManager.callEvent(ProxyCheckResultEvent(null, error = e.message ?: "Unknown error"))
        }
    }

    call.respondNoContent()
}

// POST /api/v1/client/proxies/edit
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun Routing.postEditProxy() = post("/edit") {
    data class ProxyRequest(
        val id: Int,
        val host: String,
        val port: Int,
        val type: Proxy.Type,
        val username: String,
        val password: String,
        val forwardAuthentication: Boolean
    )
    val (id, host, port, type, username, password, forwardAuthentication) = call.receive<ProxyRequest>()

    if (host.isBlank()) {
        call.forbidden("No host")
    }

    if (port !in 0..65535) {
        call.forbidden("Illegal port")
    }

    val proxy = Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication)
    ProxyManager.validateProxy(proxy, index = id)
    call.respondNoContent()
}

// POST /api/v1/client/proxies/check
private fun Routing.postCheckProxy() = post("/check") {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.validateProxy(ProxyManager.proxies[body.id], checkOnly = true)
    call.respondNoContent()
}

// DELETE /api/v1/client/proxies/remove
private fun Routing.deleteRemoveProxy() = delete("/remove") {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    if (ProxyManager.proxies.removeAt(body.id) == ProxyManager.proxy) {
        ProxyManager.proxy = Proxy.NONE
    }
    call.respondNoContent()
}

// PUT /api/v1/client/proxies/favorite
private fun Routing.putFavoriteProxy() = put {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = true
    ConfigSystem.store(ProxyManager)
    call.respondNoContent()
}

// DELETE /api/v1/client/proxies/favorite
private fun Routing.deleteFavoriteProxy() = delete {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = false
    ConfigSystem.store(ProxyManager)
    call.respondNoContent()
}

internal fun Routing.proxyRoutes() {
    route("/proxy") {
        getProxyInfo()
        postProxy()
        deleteProxy()
    }
    route("/proxies") {
        getProxies()
        route("/add") {
            postAddProxy()
            postClipboardProxy()
        }
        postEditProxy()
        postCheckProxy()
        deleteRemoveProxy()
        route("/favorite") {
            putFavoriteProxy()
            deleteFavoriteProxy()
        }
    }
}
