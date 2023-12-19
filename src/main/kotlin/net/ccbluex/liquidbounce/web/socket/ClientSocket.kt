/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.web.socket

import io.netty.channel.ChannelHandlerContext
import net.ccbluex.liquidbounce.web.socket.netty.NettyServer
import net.ccbluex.liquidbounce.web.socket.protocol.event.SocketEventHandler
import net.ccbluex.liquidbounce.web.socket.protocol.rest.RestApi
import kotlin.concurrent.thread

/**
 * A client websocket implementation.
 * Allows the browser to communicate with the client. (e.g. for UIs)
 *
 * @see [https://tools.ietf.org/html/rfc6455]
 */
object ClientSocket {

    internal var contexts = mutableListOf<ChannelHandlerContext>()
    internal var socketEventHandler = SocketEventHandler()
    internal val restApi = RestApi()

    fun start() {
        thread(name = "netty-websocket") {
            NettyServer().startServer()
        }

        // todo: register via RestAPI instead of hardcoding
        socketEventHandler.register("worldDisconnect")
        socketEventHandler.register("windowResize")
        socketEventHandler.register("mouseButton")
        socketEventHandler.register("mouseScroll")
        socketEventHandler.register("mouseCursor")
        socketEventHandler.register("keyboardKey")
        socketEventHandler.register("keyboardChar")
        socketEventHandler.register("session")
        socketEventHandler.register("chatSend")
        socketEventHandler.register("chatReceive")
        socketEventHandler.register("death")
        socketEventHandler.register("toggleModule")
        socketEventHandler.register("notification")
        socketEventHandler.register("clientChatMessage")
        socketEventHandler.register("clientChatError")
        socketEventHandler.register("altManagerUpdate")
        socketEventHandler.register("virtualScreen")
        socketEventHandler.register("fps")
        socketEventHandler.register("playerStats")
        socketEventHandler.register("key")
        socketEventHandler.register("splashOverlay")
        socketEventHandler.register("splashProgress")

        // RestAPI
        restApi.setupRoutes()
    }


}
