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
 *
 */

package net.ccbluex.liquidbounce.features.chat

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.utils.client.logger

class ChannelHandler(private val chatClient: ChatClient,
                     private val handshaker: WebSocketClientHandshaker) : SimpleChannelInboundHandler<Any>() {

    lateinit var handshakeFuture: ChannelPromise

    /**
     * Do nothing by default, sub-classes may override this method.
     */
    override fun handlerAdded(ctx: ChannelHandlerContext) {
        handshakeFuture = ctx.newPromise()
    }

    /**
     * Calls [ChannelHandlerContext.fireChannelActive] to forward
     * to the next [ChannelInboundHandler] in the [ChannelPipeline].
     *
     * Sub-classes may override this method to change behavior.
     */
    override fun channelActive(ctx: ChannelHandlerContext) {
        handshaker.handshake(ctx.channel())
    }

    /**
     * Calls [ChannelHandlerContext.fireChannelInactive] to forward
     * to the next [ChannelInboundHandler] in the [ChannelPipeline].
     *
     * Sub-classes may override this method to change behavior.
     */
    override fun channelInactive(ctx: ChannelHandlerContext) {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
    }

    /**
     * Calls [ChannelHandlerContext.fireExceptionCaught] to forward
     * to the next [ChannelHandler] in the [ChannelPipeline].
     *
     * Sub-classes may override this method to change behavior.
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        logger.error("LiquidChat error", cause)
        EventManager.callEvent(ClientChatErrorEvent(
            cause.localizedMessage ?: cause.message ?: cause.javaClass.name
        ))

        if (!handshakeFuture.isDone) {
            handshakeFuture.setFailure(cause)
        }
        ctx.close()
    }

    /**
     * **Please keep in mind that this method will be renamed to
     * `messageReceived(ChannelHandlerContext, I)` in 5.0.**
     *
     * Is called for each message of type [I].
     *
     * @param ctx           the [ChannelHandlerContext] which this [SimpleChannelInboundHandler]
     * belongs to
     * @param msg           the message to handle
     * @throws Exception    is thrown if an error occurred
     */
    override fun channelRead0(ctx: ChannelHandlerContext, msg: Any) {
        val channel = ctx.channel()

        if (!handshaker.isHandshakeComplete) {
            try {
                handshaker.finishHandshake(channel, msg as FullHttpResponse)
                handshakeFuture.setSuccess()

            } catch (exception: WebSocketHandshakeException) {
                handshakeFuture.setFailure(exception)
            }
            return
        }

        when (msg) {
            is TextWebSocketFrame -> chatClient.handlePlainMessage(msg.text())
            is CloseWebSocketFrame -> channel.close()
        }
    }
}
