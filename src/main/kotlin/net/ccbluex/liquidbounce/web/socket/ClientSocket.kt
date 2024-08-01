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
    private var socketEventHandler = SocketEventHandler()
    private val restApi = RestApi()

    /**
     * Basic events which are always registered.
     *
     * These makes sense to always provide to the websocket client, as they are not causing any high traffic.
     * In case of a high traffic event, it should be registered manually via RestAPI endpoint.
     */
    private val baseEvents = arrayOf(
        // Without this event, NOTHING will work!!!
        "virtualScreen",

        // Most essential events
        "spaceSeperatedNamesChange",
        "clickGuiScaleChange",
        "toggleModule",
        "notification",
        "accountManagerMessage",
        "accountManagerAddition",
        "accountManagerLogin",
        "session",
        "splashOverlay",
        "splashProgress",
        "key",
        "refreshArrayList",
        "serverConnect",
        "serverPinged",
        "targetChange",
        "gameModeChange",
        "componentsUpdate",
        "proxyAdditionResult",
        "proxyEditResult",
        "proxyCheckResult",
        "scaleFactorChange",
        "overlayMessage",

        // Statistic events
        "fps",
        "clientPlayerData",

        // LiquidChat events, needed for chat UI
        "clientChatMessage",
        "clientChatError",

        // Nice to have events
        "chatSend",
        "chatReceive",

        "death",
        "disconnect",

        // Might be nice to have in case someone needs them for any reason
        "mouseButton",
        "mouseScroll",
        "keyboardKey",
        "keyboardChar",
        // "mouseCursor", Not needed
        // "windowResize",
        "keybindChange",

        // browser support events
        "browserUrlChange"
    )

    fun start() {
        thread(name = "netty-websocket") {
            NettyServer().startServer()
        }

        baseEvents.forEach(socketEventHandler::register)

        // RestAPI
        restApi.setupRoutes()
    }


}
