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
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.integration.interop.forbidden
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import org.lwjgl.glfw.GLFW

/**
 * Proxy endpoints
 */

// GET /api/v1/client/proxy
private fun Route.getProxyInfo() = get {
    ProxyManager.currentProxy?.let { proxy ->
        call.respond(interopGson.toJsonTree(proxy).asJsonObject.apply {
            addProperty("id", ProxyManager.proxies.indexOf(proxy))
        })
    } ?: call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/proxy
private fun Route.postProxy() = post {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id !in ProxyManager.proxies.indices) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxy = ProxyManager.proxies[body.id]
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/proxy
private fun Route.deleteProxy() = delete {
    ProxyManager.proxy = Proxy.NONE
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// GET /api/v1/client/proxies
private fun Route.getProxies() = get {
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
private fun Route.postAddProxy() = post {
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
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/proxies/add/clipboard
private fun Route.postClipboardProxy() = post("/clipboard") {
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

    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/proxies/edit
@Suppress("DestructuringDeclarationWithTooManyEntries")
private fun Route.postEditProxy() = post("/edit") {
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
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// POST /api/v1/client/proxies/check
private fun Route.postCheckProxy() = post("/check") {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.validateProxy(ProxyManager.proxies[body.id], checkOnly = true)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/proxies/remove
private fun Route.deleteRemoveProxy() = delete("/remove") {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    if (ProxyManager.proxies.removeAt(body.id) == ProxyManager.proxy) {
        ProxyManager.proxy = Proxy.NONE
    }
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// PUT /api/v1/client/proxies/favorite
private fun Route.putFavoriteProxy() = put {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = true
    ConfigSystem.store(ProxyManager)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

// DELETE /api/v1/client/proxies/favorite
private fun Route.deleteFavoriteProxy() = delete {
    data class ProxyRequest(val id: Int)

    val body = call.receive<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        call.forbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = false
    ConfigSystem.store(ProxyManager)
    call.respond(io.ktor.http.HttpStatusCode.NoContent)
}

internal fun Route.proxyRoutes() {
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
