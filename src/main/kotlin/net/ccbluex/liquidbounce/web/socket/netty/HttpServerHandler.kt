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
package net.ccbluex.liquidbounce.web.socket.netty

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.handler.codec.http.HttpContent
import io.netty.handler.codec.http.HttpHeaderNames
import io.netty.handler.codec.http.HttpRequest
import io.netty.handler.codec.http.LastHttpContent
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.ClientSocket
import net.ccbluex.liquidbounce.web.socket.netty.model.RequestContext
import net.ccbluex.liquidbounce.web.socket.netty.model.RequestObject
import java.net.URLDecoder


internal class HttpServerHandler : ChannelInboundHandlerAdapter() {

    private val localRequestContext = ThreadLocal<RequestContext>()

    private val HttpRequest.webSocketUrl: String
        get() = "ws://${headers().get("Host")}${uri()}"

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        when (msg) {
            is HttpRequest -> {
                val headers = msg.headers()
                val connection = headers.get(HttpHeaderNames.CONNECTION)
                val upgrade = headers.get(HttpHeaderNames.UPGRADE)

                logger.debug("Incoming connection ${ctx.channel()} with headers: \n" +
                    headers.joinToString { "${it.key}=${it.value}\n" })

                if (connection.equals("Upgrade", ignoreCase = true) &&
                    upgrade.equals("WebSocket", ignoreCase = true)) {
                    // Takes out Http Request Handler from the pipeline and replaces it with WebSocketHandler
                    ctx.pipeline().replace(this, "websocketHandler", WebSocketHandler())

                    // Upgrade connection from Http to WebSocket protocol
                    val wsFactory = WebSocketServerHandshakerFactory(msg.webSocketUrl, null, true)
                    val handshaker = wsFactory.newHandshaker(msg)

                    if (handshaker == null) {
                        // This means the version of the websocket protocol is not supported
                        // Unlikely to happen, but it's better to be safe than sorry
                        WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel())
                    } else {
                        // Otherwise pass handshake to the handshaker
                        handshaker.handshake(ctx.channel(), msg)
                    }

                    ClientSocket.contexts += ctx
                } else {
                    val requestContext = RequestContext(
                        msg.method(),
                        URLDecoder.decode(msg.uri(), "UTF-8"),
                        msg.headers().associate { it.key to it.value },
                    )

                    localRequestContext.set(requestContext)
                }
            }

            is HttpContent -> {
                if (localRequestContext.get() == null) {
                    logger.warn("Received HttpContent without HttpRequest")
                    return
                }

                // Append content to the buffer
                val requestContext = localRequestContext.get()
                requestContext
                    .contentBuffer
                    .append(msg.content().toString(Charsets.UTF_8))

                // If this is the last content, process the request
                if (msg is LastHttpContent) {
                    val requestObject = RequestObject(requestContext)
                    localRequestContext.remove()

                    val httpConductor = HttpConductor()
                    val response = httpConductor.processRequestObject(requestObject)
                    ctx.writeAndFlush(response)
                }
            }

        }

        super.channelRead(ctx, msg)
    }

}
