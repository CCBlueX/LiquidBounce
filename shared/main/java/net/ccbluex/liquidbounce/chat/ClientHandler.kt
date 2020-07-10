/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.chat

import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import net.ccbluex.liquidbounce.utils.ClientUtils

class ClientHandler(val client: Client, private val handshaker: WebSocketClientHandshaker) : SimpleChannelInboundHandler<Any>() {

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
        client.onDisconnect()
        client.channel = null
        client.username = ""
        client.jwt = false
    }

    /**
     * Calls [ChannelHandlerContext.fireExceptionCaught] to forward
     * to the next [ChannelHandler] in the [ChannelPipeline].
     *
     * Sub-classes may override this method to change behavior.
     */
    override fun exceptionCaught(ctx: ChannelHandlerContext, cause: Throwable) {
        ClientUtils.getLogger().error("LiquidChat error", cause)
        client.onError(cause)
        if(!handshakeFuture.isDone) handshakeFuture.setFailure(cause)
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

        if(!handshaker.isHandshakeComplete) {
            try{
                handshaker.finishHandshake(channel, msg as FullHttpResponse)
                handshakeFuture.setSuccess()

            }catch (exception: WebSocketHandshakeException) {
                handshakeFuture.setFailure(exception)
            }

            client.onHandshake(handshakeFuture.isSuccess)
            return
        }

        when (msg) {
            is TextWebSocketFrame -> client.onMessage(msg.text())
            is CloseWebSocketFrame -> channel.close()
        }
    }
}