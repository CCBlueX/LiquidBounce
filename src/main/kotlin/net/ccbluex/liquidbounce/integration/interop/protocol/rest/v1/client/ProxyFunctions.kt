/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.client

import com.google.gson.JsonArray
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ProxyCheckResultEvent
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpNoContent
import net.ccbluex.netty.http.util.httpOk
import org.lwjgl.glfw.GLFW

/**
 * Proxy endpoints
 */

// GET /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun getProxyInfo(requestObject: RequestObject) = ProxyManager.currentProxy?.let { proxy ->
    httpOk(interopGson.toJsonTree(proxy).asJsonObject.apply {
        addProperty("id", ProxyManager.proxies.indexOf(proxy))
    })
} ?: httpNoContent()

// POST /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun postProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.proxy = ProxyManager.proxies[body.id]
    return httpNoContent()
}

// DELETE /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun deleteProxy(requestObject: RequestObject): FullHttpResponse {
    ProxyManager.proxy = Proxy.NONE
    return httpNoContent()
}

// GET /api/v1/client/proxies
@Suppress("UNUSED_PARAMETER")
fun getProxies(requestObject: RequestObject) = httpOk(JsonArray().apply {
    ProxyManager.proxies.forEachIndexed { index, proxy ->
        add(interopGson.toJsonTree(proxy).asJsonObject.apply {
            addProperty("id", index)
            addProperty("type", (proxy.type ?: Proxy.Type.SOCKS5).toString())
        })
    }
})

// POST /api/v1/client/proxies/add
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun postAddProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(
        val host: String,
        val port: Int,
        val username: String,
        val password: String,
        val type: Proxy.Type,
        val forwardAuthentication: Boolean
    )
    val (host, port, username, password, type, forwardAuthentication) = requestObject.asJson<ProxyRequest>()

    if (host.isBlank()) {
        return httpForbidden("No host")
    }

    if (port !in 0..65535) {
        return httpForbidden("Illegal port")
    }

    ProxyManager.validateProxy(Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication))
    return httpNoContent()
}

// POST /api/v1/client/proxies/add/clipboard
@Suppress("UNUSED_PARAMETER")
fun postClipboardProxy(requestObject: RequestObject): FullHttpResponse {
    mc.execute {
        try {
            val clipboardText = GLFW.glfwGetClipboardString(mc.window.handle)
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

    return httpNoContent()
}

// POST /api/v1/client/proxies/edit
@Suppress("DestructuringDeclarationWithTooManyEntries")
fun postEditProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(
        val id: Int,
        val host: String,
        val port: Int,
        val type: Proxy.Type,
        val username: String,
        val password: String,
        val forwardAuthentication: Boolean
    )
    val (id, host, port, type, username, password, forwardAuthentication) = requestObject.asJson<ProxyRequest>()

    if (host.isBlank()) {
        return httpForbidden("No host")
    }

    if (port !in 0..65535) {
        return httpForbidden("Illegal port")
    }

    val proxy = Proxy(host, port, Proxy.credentials(username, password), type, forwardAuthentication)
    ProxyManager.validateProxy(proxy, index = id)
    return httpNoContent()
}

// POST /api/v1/client/proxies/check
@Suppress("UNUSED_PARAMETER")
fun postCheckProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.validateProxy(ProxyManager.proxies[body.id], checkOnly = true)
    return httpNoContent()
}

// DELETE /api/v1/client/proxies/remove
@Suppress("UNUSED_PARAMETER")
fun deleteRemoveProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    if (ProxyManager.proxies.removeAt(body.id) == ProxyManager.proxy) {
        ProxyManager.proxy = Proxy.NONE
    }
    return httpNoContent()
}

// PUT /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun putFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = true
    ConfigSystem.store(ProxyManager)
    return httpNoContent()
}

// DELETE /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun deleteFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.proxies[body.id].favorite = false
    ConfigSystem.store(ProxyManager)
    return httpNoContent()
}
