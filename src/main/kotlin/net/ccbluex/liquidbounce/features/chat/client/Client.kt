/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.chat.client

import com.google.gson.GsonBuilder
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.SocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.ssl.SslContext
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import net.ccbluex.liquidbounce.features.chat.Chat
import net.ccbluex.liquidbounce.features.chat.client.packet.*

import java.net.URI

class Client {

    var channel: Channel? = null

    private val serializer = PacketSerializer().apply {
        registerPacket("RequestMojangInfo", ServerRequestMojangInfoPacket::class.java)
        registerPacket("LoginMojang", ServerLoginMojangPacket::class.java)
        registerPacket("Message", ServerMessagePacket::class.java)
        registerPacket("PrivateMessage", ServerPrivateMessagePacket::class.java)
        registerPacket("BanUser", ServerBanUserPacket::class.java)
        registerPacket("UnbanUser", ServerUnbanUserPacket::class.java)
        registerPacket("RequestJWT", ServerRequestJWTPacket::class.java)
        registerPacket("LoginJWT", ServerLoginJWTPacket::class.java)
    }

    private val deserializer = PacketDeserializer().apply {
        registerPacket("MojangInfo", ClientMojangInfoPacket::class.java)
        registerPacket("NewJWT", ClientNewJWTPacket::class.java)
        registerPacket("Message", ClientMessagePacket::class.java)
        registerPacket("PrivateMessage", ClientPrivateMessagePacket::class.java)
        registerPacket("Error", ClientErrorPacket::class.java)
        registerPacket("Success", ClientSuccessPacket::class.java)
    }

    val connected: Boolean
        get() = channel != null && channel!!.isOpen

    /**
     * Connect to chat server
     */
    fun connect() {
        if (!Chat.enabled) {
            return
        }

        Chat.onConnect()

        val uri = URI("wss://chat.liquidbounce.net:7886/ws")

        val ssl = uri.scheme.equals("wss", true)
        val sslContext = if (ssl) SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE) else null

        val group = NioEventLoopGroup()
        val handler = ChannelHandler(
            WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                true,
                DefaultHttpHeaders()
            )
        )

        val bootstrap = Bootstrap()

        bootstrap.group(group)
            .channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<SocketChannel>() {

                /**
                 * This method will be called once the [Channel] was registered. After the method returns this instance
                 * will be removed from the [ChannelPipeline] of the [Channel].
                 *
                 * @param ch            the [Channel] which was registered.
                 * @throws Exception    is thrown if an error occurs. In that case the [Channel] will be closed.
                 */
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()

                    if (sslContext != null) {
                        pipeline.addLast(sslContext.newHandler(ch.alloc()))
                    }

                    pipeline.addLast(HttpClientCodec(), HttpObjectAggregator(8192), handler)
                }

            })

        channel = bootstrap.connect(uri.host, uri.port).sync().channel()
        handler.handshakeFuture.sync()

        if (connected) {
            Chat.onConnected()
        }
    }

    /**
     * Send packet to server
     */
    internal fun sendPacket(packet: Packet) {
        val gson = GsonBuilder()
            .registerTypeAdapter(Packet::class.java, serializer)
            .create()

        channel?.writeAndFlush(TextWebSocketFrame(gson.toJson(packet, Packet::class.java)))
    }


    /**
     * Handle incoming message of websocket
     */
    internal fun onMessage(message: String) {
        val gson = GsonBuilder()
            .registerTypeAdapter(Packet::class.java, deserializer)
            .create()

        val packet = gson.fromJson(message, Packet::class.java)
        Chat.onPacket(packet)
    }

}
