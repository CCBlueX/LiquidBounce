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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.features.chat

import com.google.gson.GsonBuilder
import com.mojang.authlib.exceptions.InvalidCredentialsException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFutureListener
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandler
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPipeline
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import net.ccbluex.liquidbounce.authlib.yggdrasil.GameProfileRepository
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatJwtTokenEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.features.chat.packet.S2CErrorPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CMojangInfoPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CNewJWTPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPrivateMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CSuccessPacket
import net.ccbluex.liquidbounce.features.chat.packet.AxochatPacket
import net.ccbluex.liquidbounce.features.chat.packet.PacketDeserializer
import net.ccbluex.liquidbounce.features.chat.packet.PacketSerializer
import net.ccbluex.liquidbounce.features.chat.packet.C2SBanUserPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SLoginJWTPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SLoginMojangPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPrivateMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestJWTPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestMojangInfoPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SUnbanUserPacket
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.clientChannelAndGroup
import net.ccbluex.netty.http.coroutines.syncSuspend
import java.net.URI
import java.util.UUID

class AxochatClient {

    private var channel: Channel? = null

    private val serializer = PacketSerializer().apply {
        register<C2SRequestMojangInfoPacket>("RequestMojangInfo")
        register<C2SLoginMojangPacket>("LoginMojang")
        register<C2SMessagePacket>("Message")
        register<C2SPrivateMessagePacket>("PrivateMessage")
        register<C2SBanUserPacket>("BanUser")
        register<C2SUnbanUserPacket>("UnbanUser")
        register<C2SRequestJWTPacket>("RequestJWT")
        register<C2SLoginJWTPacket>("LoginJWT")
    }

    private val deserializer = PacketDeserializer().apply {
        register<S2CMojangInfoPacket>("MojangInfo")
        register<S2CNewJWTPacket>("NewJWT")
        register<S2CMessagePacket>("Message")
        register<S2CPrivateMessagePacket>("PrivateMessage")
        register<S2CErrorPacket>("Error")
        register<S2CSuccessPacket>("Success")
    }

    val isConnected: Boolean
        get() = channel != null && channel!!.isOpen

    private var isConnecting = false
    var isLoggedIn = false
        private set

    private val serializerGson by lazy {
        GsonBuilder()
            .registerTypeAdapter(AxochatPacket.C2S::class.java, serializer)
            .create()
    }

    private val deserializerGson by lazy {
        GsonBuilder()
            .registerTypeAdapter(AxochatPacket.S2C::class.java, deserializer)
            .create()
    }

    /**
     * Connect to chat server via websocket.
     * Supports SSL and non-SSL connections.
     * Be aware SSL takes insecure certificates.
     */
    suspend fun connect() = runCatching {
        if (isConnecting || isConnected) {
            return@runCatching
        }

        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTING))
        isConnecting = true
        isLoggedIn = false

        val uri = URI("wss://chat.liquidbounce.net:7886/ws")

        val ssl = uri.scheme.equals("wss", true)
        val sslContext = if (ssl) {
            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else {
            null
        }

        val handler = ChannelHandler(
            WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                true,
                DefaultHttpHeaders(),
                65536,
            )
        )

        val bootstrap = Bootstrap()

        bootstrap.clientChannelAndGroup(true)
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

                    pipeline.addLast(HttpClientCodec(), HttpObjectAggregator(65536), handler)
                }

            })

        channel = bootstrap.connect(uri.host, uri.port).syncSuspend().channel()!!
        handler.handshakeFuture.syncSuspend()
    }.onFailure {
        EventManager.callEvent(ClientChatErrorEvent(it.localizedMessage ?: it.message ?: it.javaClass.name))

        isConnecting = false
    }.onSuccess {
        if (isConnected) {
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTED))
        }

        isConnecting = false
    }

    fun disconnect() {
        channel?.writeAndFlush(CloseWebSocketFrame(1000, ""))?.addListener(ChannelFutureListener.CLOSE)
        channel = null

        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
        isConnecting = false
        isLoggedIn = false
    }

    suspend fun reconnect() {
        disconnect()
        connect()
    }


    /**
     * Request Mojang authentication details for login
     */
    fun requestMojangLogin() = sendPacket(C2SRequestMojangInfoPacket())

    /**
     * Send chat message to server
     */
    fun sendMessage(message: String) = sendPacket(C2SMessagePacket(message))

    /**
     * Send private chat message to server
     */
    fun sendPrivateMessage(receiver: String, message: String) =
        sendPacket(C2SPrivateMessagePacket(receiver, message))

    /**
     * Ban user from server
     */
    fun banUser(target: String) = sendPacket(C2SBanUserPacket(toUUID(target)))

    /**
     * Unban user from server
     */
    fun unbanUser(target: String) = sendPacket(C2SUnbanUserPacket(toUUID(target)))

    /**
     * Convert username or uuid to UUID
     */
    private fun toUUID(target: String): String {
        return try {
            UUID.fromString(target)

            target
        } catch (_: IllegalArgumentException) {
            val incomingUUID = GameProfileRepository.Default.fetchUuidByUsername(target)
            incomingUUID.toString()
        }
    }

    /**
     * Login to web socket via JWT
     */
    fun loginViaJwt(token: String) {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))
        sendPacket(C2SLoginJWTPacket(token, allowMessages = true))
    }

    /**
     * Send packet to server
     */
    internal fun sendPacket(packet: AxochatPacket.C2S) {
        channel?.writeAndFlush(TextWebSocketFrame(serializerGson.toJson(packet, AxochatPacket.C2S::class.java)))
    }

    private fun handleFunctionalPacket(packet: AxochatPacket.S2C) {
        when (packet) {
            is S2CMojangInfoPacket -> {
                EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))

                runCatching {
                    val sessionHash = packet.sessionHash

                    mc.services.sessionService.joinServer(
                        mc.user.profileId,
                        mc.user.accessToken,
                        sessionHash
                    )
                    sendPacket(
                        C2SLoginMojangPacket(
                            mc.user.name,
                            mc.user.profileId,
                            allowMessages = true
                        )
                    )
                }.onFailure { cause ->
                    if (cause is InvalidCredentialsException) {
                        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.AUTHENTICATION_FAILED))
                    } else {
                        EventManager.callEvent(ClientChatErrorEvent(
                            cause.localizedMessage ?: cause.message ?: cause.javaClass.name
                        ))
                    }
                }
                return
            }

            is S2CMessagePacket -> EventManager.callEvent(ClientChatMessageEvent(packet.user, packet.content,
                ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT))
            is S2CPrivateMessagePacket -> EventManager.callEvent(ClientChatMessageEvent(packet.user, packet.content,
                ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT))
            is S2CErrorPacket -> {
                // TODO: Replace with translation
                EventManager.callEvent(ClientChatErrorEvent(translateErrorMessage(packet)))
            }
            is S2CSuccessPacket -> {
                when (packet.reason) {
                    "Login" -> {
                        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGED_IN))
                        isLoggedIn = true
                    }

                    // TODO: Replace with translation
                    "Ban" -> chat("§7[§a§lChat§7] §9Successfully banned user!")
                    "Unban" -> chat("§7[§a§lChat§7] §9Successfully unbanned user!")
                }
            }

            is S2CNewJWTPacket -> EventManager.callEvent(ClientChatJwtTokenEvent(packet.token))
        }
    }

    private fun translateErrorMessage(packet: S2CErrorPacket): String {
        val message = when (packet.message) {
            "NotSupported" -> "This method is not supported!"
            "LoginFailed" -> "Login Failed!"
            "NotLoggedIn" -> "You must be logged in to use the chat!"
            "AlreadyLoggedIn" -> "You are already logged in!"
            "MojangRequestMissing" -> "Mojang request missing!"
            "NotPermitted" -> "You are missing the required permissions!"
            "NotBanned" -> "You are not banned!"
            "Banned" -> "You are banned!"
            "RateLimited" -> "You have been rate limited. Please try again later."
            "PrivateMessageNotAccepted" -> "Private message not accepted!"
            "EmptyMessage" -> "You are trying to send an empty message!"
            "MessageTooLong" -> "Message is too long!"
            "InvalidCharacter" -> "Message contains a non-ASCII character!"
            "InvalidId" -> "The given ID is invalid!"
            "Internal" -> "An internal server error occurred!"
            else -> packet.message
        }

        return message
    }


    /**
     * Handle incoming message of websocket
     */
    internal fun handlePlainMessage(message: String) {
        val packet = deserializerGson.fromJson(message, AxochatPacket.S2C::class.java)
        handleFunctionalPacket(packet)
    }

    private inner class ChannelHandler(
        private val handshaker: WebSocketClientHandshaker,
    ) : SimpleChannelInboundHandler<Any>() {

        lateinit var handshakeFuture: ChannelPromise

        /**
         * Do nothing by default, subclasses may override this method.
         */
        override fun handlerAdded(ctx: ChannelHandlerContext) {
            handshakeFuture = ctx.newPromise()
        }

        /**
         * Calls [ChannelHandlerContext.fireChannelActive] to forward
         * to the next [ChannelInboundHandler] in the [ChannelPipeline].
         *
         * Subclasses may override this method to change behavior.
         */
        override fun channelActive(ctx: ChannelHandlerContext) {
            handshaker.handshake(ctx.channel())
        }

        /**
         * Calls [ChannelHandlerContext.fireChannelInactive] to forward
         * to the next [ChannelInboundHandler] in the [ChannelPipeline].
         *
         * Subclasses may override this method to change behavior.
         */
        override fun channelInactive(ctx: ChannelHandlerContext) {
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
        }

        /**
         * Calls [ChannelHandlerContext.fireExceptionCaught] to forward
         * to the next [ChannelHandler] in the [ChannelPipeline].
         *
         * Subclasses may override this method to change behavior.
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
         * @param ctx           the [ChannelHandlerContext] which this [SimpleChannelInboundHandler] belongs to
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
                is TextWebSocketFrame -> handlePlainMessage(msg.text())
                is CloseWebSocketFrame -> channel.close()
            }
        }
    }

}
