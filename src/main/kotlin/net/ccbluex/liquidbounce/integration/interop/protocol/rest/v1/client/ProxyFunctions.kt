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
import com.mojang.blaze3d.systems.RenderSystem
import io.netty.handler.codec.http.FullHttpResponse
import net.ccbluex.liquidbounce.config.gson.interopGson
import net.ccbluex.liquidbounce.config.gson.util.emptyJsonObject
import net.ccbluex.liquidbounce.features.misc.proxy.Proxy
import net.ccbluex.liquidbounce.features.misc.proxy.ProxyManager
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpForbidden
import net.ccbluex.netty.http.util.httpOk
import org.lwjgl.glfw.GLFW

/**
 * Proxy endpoints
 */

// GET /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun getProxyInfo(requestObject: RequestObject) = httpOk(ProxyManager.currentProxy?.let { proxy ->
    interopGson.toJsonTree(proxy).asJsonObject.apply {
        addProperty("id", ProxyManager.proxies.indexOf(proxy))
    }
} ?: emptyJsonObject())

// POST /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun postProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.setProxy(body.id)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/proxy
@Suppress("UNUSED_PARAMETER")
fun deleteProxy(requestObject: RequestObject): FullHttpResponse {
    ProxyManager.unsetProxy()
    return httpOk(emptyJsonObject())
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

    ProxyManager.addProxy(host, port, username, password, type, forwardAuthentication)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/proxies/clipboard
@Suppress("UNUSED_PARAMETER")
fun postClipboardProxy(requestObject: RequestObject): FullHttpResponse {
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
                    ProxyManager.addProxy(host, port)
                }
            }
        }
    }

    return httpOk(emptyJsonObject())
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

    ProxyManager.editProxy(id, host, port, username, password, type, forwardAuthentication)
    return httpOk(emptyJsonObject())
}

// POST /api/v1/client/proxies/check
@Suppress("UNUSED_PARAMETER")
fun postCheckProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.checkProxy(body.id)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/proxies/remove
@Suppress("UNUSED_PARAMETER")
fun deleteRemoveProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.removeProxy(body.id)
    return httpOk(emptyJsonObject())
}

// PUT /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun putFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.favoriteProxy(body.id)
    return httpOk(emptyJsonObject())
}

// DELETE /api/v1/client/proxies/favorite
@Suppress("UNUSED_PARAMETER")
fun deleteFavoriteProxy(requestObject: RequestObject): FullHttpResponse {
    data class ProxyRequest(val id: Int)

    val body = requestObject.asJson<ProxyRequest>()

    if (body.id < 0 || body.id >= ProxyManager.proxies.size) {
        return httpForbidden("Invalid id")
    }

    ProxyManager.unfavoriteProxy(body.id)
    return httpOk(emptyJsonObject())
}
