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
package net.ccbluex.liquidbounce.integration.interop

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.utils.client.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.integration.interop.protocol.event.SocketEventHandler
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.v1Functions
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import java.net.Socket
import kotlin.concurrent.thread

/**
 * A client server implementation.
 *
 * Allows the browser to communicate with the client. (e.g. for UIs)
 */
object ClientInteropServer {

    internal var httpServer = HttpServer()
    private var socketEventHandler = SocketEventHandler()

    private const val DEFAULT_PORT = 15000

    val port = try {
        Socket("127.0.0.1", DEFAULT_PORT).use {
            logger.info("Default port unavailable. Falling back to random port.")
            (15001..17000).random()
        }
    } catch (e: Exception) {
        logger.info("Default port $DEFAULT_PORT available.")
        DEFAULT_PORT
    }
    val url = "http://127.0.0.1:$port"

    fun start() {
        runCatching {
            // RestAPI
            httpServer.routeController.apply {
                get("/", ::getRootResponse)
                v1Functions(this)
                file("/", ConfigSystem.rootFolder.resolve("themes"))
            }

            // Register events with @WebSocketEvent annotation
            socketEventHandler.registerAll()
        }.onFailure(ErrorHandler::fatal)

        // Start the HTTP server
        thread(name = "netty-websocket") {
            runCatching {
                httpServer.start(port)
            }.onFailure(ErrorHandler::fatal)
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getRootResponse(requestObject: RequestObject) = httpOk(JsonObject().apply {
        addProperty("name", LiquidBounce.CLIENT_NAME)
        addProperty("version", LiquidBounce.clientVersion)
        addProperty("author", LiquidBounce.CLIENT_AUTHOR)
    })

}
