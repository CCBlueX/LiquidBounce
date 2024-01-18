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
import io.netty.handler.codec.http.websocketx.*
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.web.socket.ClientSocket

internal class WebSocketHandler : ChannelInboundHandlerAdapter() {

    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        if (msg is WebSocketFrame) {
            logger.debug("WebSocketFrame received ({}): {}", ctx.channel(), msg.javaClass.name)

            when (msg) {
                is TextWebSocketFrame -> ctx.channel().writeAndFlush(TextWebSocketFrame(msg.text()))
                is PingWebSocketFrame -> ctx.channel().writeAndFlush(PongWebSocketFrame(msg.content().retain()))
                is CloseWebSocketFrame -> {
                    // Accept close frame and send close frame back
                    ctx.channel().writeAndFlush(msg.retainedDuplicate())
                    ctx.channel().close().sync()

                    ClientSocket.contexts -= ctx
                    logger.debug("WebSocket closed due to ${msg.reasonText()} (${msg.statusCode()})")
                }
                else -> logger.error("Unknown WebSocketFrame type: ${msg.javaClass.name}")
            }
        }
    }

}
