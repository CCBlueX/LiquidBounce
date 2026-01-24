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
package net.ccbluex.liquidbounce.integration.interop

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.marketplace.MarketplaceManager
import net.ccbluex.liquidbounce.integration.interop.middleware.AuthMiddleware
import net.ccbluex.liquidbounce.integration.interop.protocol.event.SocketEventListener
import net.ccbluex.liquidbounce.integration.interop.protocol.rest.v1.registerInteropFunctions
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.env
import net.ccbluex.liquidbounce.utils.client.error.ErrorHandler
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.netty.http.HttpServer
import net.ccbluex.netty.http.middleware.CorsMiddleware
import net.ccbluex.netty.http.model.RequestObject
import net.ccbluex.netty.http.util.httpOk
import org.apache.commons.lang3.RandomStringUtils
import java.net.BindException
import java.net.ServerSocket

/**
 * A client server implementation.
 *
 * Allows the browser to communicate with the client. (e.g. for UIs)
 */
object ClientInteropServer {

    internal val httpServer = HttpServer()

    val isSkipping = env("LB_INTEROP_SKIP", "net.ccbluex.liquidbounce.interop.skip")?.toBoolean()
        ?: false

    var PORT = env("LB_INTEROP_PORT", "net.ccbluex.liquidbounce.interop.port")?.toIntOrNull()
        ?: ServerSocket(0).use { socket -> socket.localPort }
    val AUTH_CODE: String = env("LB_INTEROP_AUTH_CODE", "net.ccbluex.liquidbounce.interop.authCode")
        ?: RandomStringUtils.secure().nextAlphanumeric(16)

    val url get() = "http://127.0.0.1:$PORT"

    suspend fun start() {
        if (isSkipping) {
            logger.warn("Environment variable 'LB_INTEROP_SKIP' is set to 'true'.")
            return
        }

        runCatching {
            // RestAPI
            httpServer.apply {
                routing {
                    get("/", ::getRootResponse)
                    registerInteropFunctions()

                    LiquidBounce.resource("themes/liquidbounce.zip").use { stream ->
                        zip("/resource/liquidbounce", stream)
                    }
                    file("/local", ThemeManager.themesFolder)
                    file("/marketplace", MarketplaceManager.marketplaceRoot)
                }

                // Add CORS and auth middleware
                middleware(CorsMiddleware())
                middleware(AuthMiddleware())
            }
        }.onFailure {
            ErrorHandler.fatal(it, additionalMessage = "Register endpoints")
        }

        // Start the HTTP server
        this.PORT = startServer(this.PORT)
    }

    private var attempt = 0

    private suspend fun startServer(port: Int): Int {
        return try {
            val actualPort = httpServer.start(port)

            // Register events with @WebSocketEvent annotation
            SocketEventListener.registerAll()

            actualPort
        } catch (bindException: BindException) {
            if (attempt >= 5) {
                ErrorHandler.fatal(bindException, additionalMessage = "Bind interop server")
            }

            // Retry with random port
            attempt++
            logger.error("Failed to bind to port $port. Falling back to random port.")
            startServer((15001..17000).random())
        } catch (exception: Exception) {
            ErrorHandler.fatal(exception, additionalMessage = "Start interop server")
        }
    }

    @Suppress("UNUSED_PARAMETER")
    private fun getRootResponse(requestObject: RequestObject) = httpOk(JsonObject().apply {
        addProperty("name", LiquidBounce.CLIENT_NAME)
        addProperty("version", LiquidBounce.clientVersion)
        addProperty("author", LiquidBounce.CLIENT_AUTHOR)
    })

}
