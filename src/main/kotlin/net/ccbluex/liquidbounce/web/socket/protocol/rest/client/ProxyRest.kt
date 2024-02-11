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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.config.util.decode
import net.ccbluex.liquidbounce.features.misc.ProxyManager
import net.ccbluex.liquidbounce.web.socket.netty.httpForbidden
import net.ccbluex.liquidbounce.web.socket.netty.httpOk
import net.ccbluex.liquidbounce.web.socket.netty.rest.RestNode

/**
 * Proxy endpoints
 *
 * TODO: These need to be reworked since we are going to use a proxy pool in the future
 *  similar to the account pool
 */
internal fun RestNode.proxyRest() {
    get("/proxy") {
        val proxyObject = JsonObject()

        ProxyManager.currentProxy?.let {
            proxyObject.addProperty("host", it.host)
            proxyObject.addProperty("port", it.port)
            proxyObject.addProperty("username", it.credentials?.username)
            proxyObject.addProperty("password", it.credentials?.password)
        }

        httpOk(proxyObject)
    }

    post("/proxy") {
        data class ProxyRequest(val host: String, val port: Int, val username: String, val password: String)

        val body = decode<ProxyRequest>(it.content)

        if (body.host.isBlank())
            return@post httpForbidden("No host")

        if (body.port <= 0)
            return@post httpForbidden("No port")

        ProxyManager.setProxy(body.host, body.port, body.username, body.password)

        httpOk(JsonObject())
    }

    delete("/proxy") {
        ProxyManager.unsetProxy()
        httpOk(JsonObject())
    }
}
