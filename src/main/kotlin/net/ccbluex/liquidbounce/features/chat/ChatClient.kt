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

@file:Suppress("TooManyFunctions")

package net.ccbluex.liquidbounce.features.chat

import com.google.gson.GsonBuilder
import com.mojang.authlib.exceptions.InvalidCredentialsException
import io.netty.bootstrap.Bootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelInitializer
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import net.ccbluex.liquidbounce.api.core.withScope
import net.ccbluex.liquidbounce.authlib.yggdrasil.GameProfileRepository
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.chat.packet.*
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.io.clientChannelAndGroup
import java.net.URI
import java.util.*

class ChatClient {

    private var channel: Channel? = null

    private val serializer = PacketSerializer().apply {
        register<ServerRequestMojangInfoPacket>("RequestMojangInfo")
        register<ServerLoginMojangPacket>("LoginMojang")
        register<ServerMessagePacket>("Message")
        register<ServerPrivateMessagePacket>("PrivateMessage")
        register<ServerBanUserPacket>("BanUser")
        register<ServerUnbanUserPacket>("UnbanUser")
        register<ServerRequestJWTPacket>("RequestJWT")
        register<ServerLoginJWTPacket>("LoginJWT")
    }

    private val deserializer = PacketDeserializer().apply {
        register<ClientMojangInfoPacket>("MojangInfo")
        register<ClientNewJWTPacket>("NewJWT")
        register<ClientMessagePacket>("Message")
        register<ClientPrivateMessagePacket>("PrivateMessage")
        register<ClientErrorPacket>("Error")
        register<ClientSuccessPacket>("Success")
    }

    val connected: Boolean
        get() = channel != null && channel!!.isOpen

    private var isConnecting = false
    var loggedIn = false

    private val serializerGson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Packet::class.java, serializer)
            .create()
    }

    private val deserializerGson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Packet::class.java, deserializer)
            .create()
    }

    fun connectAsync() {
        if (isConnecting || connected) {
            return
        }

        withScope {
            connect()
        }
    }

    /**
     * Connect to chat server via websocket.
     * Supports SSL and non-SSL connections.
     * Be aware SSL takes insecure certificates.
     */
    private fun connect() = runCatching {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTING))
        isConnecting = true
        loggedIn = false

        val uri = URI("wss://chat.liquidbounce.net:7886/ws")

        val ssl = uri.scheme.equals("wss", true)
        val sslContext = if (ssl) {
            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
        } else {
            null
        }

        val handler = ChannelHandler(
            this,
            WebSocketClientHandshakerFactory.newHandshaker(
                uri,
                WebSocketVersion.V13,
                null,
                true,
                DefaultHttpHeaders()
            )
        )

        val bootstrap = Bootstrap()

        bootstrap.clientChannelAndGroup(tryToUseEpoll = true)
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

        channel = bootstrap.connect(uri.host, uri.port).sync()!!.channel()!!
        handler.handshakeFuture.sync()
    }.onFailure {
        EventManager.callEvent(ClientChatErrorEvent(it.localizedMessage ?: it.message ?: it.javaClass.name))

        isConnecting = false
    }.onSuccess {
        if (connected) {
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTED))
        }

        isConnecting = false
    }

    fun disconnect() {
        channel?.close()
        channel = null

        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
        isConnecting = false
        loggedIn = false
    }

    fun reconnect() {
        disconnect()
        connectAsync()
    }


    /**
     * Request Mojang authentication details for login
     */
    fun requestMojangLogin() = sendPacket(ServerRequestMojangInfoPacket())

    /**
     * Send chat message to server
     */
    fun sendMessage(message: String) = sendPacket(ServerMessagePacket(message))

    /**
     * Send private chat message to server
     */
    fun sendPrivateMessage(username: String, message: String) =
        sendPacket(ServerPrivateMessagePacket(username, message))

    /**
     * Ban user from server
     */
    fun banUser(target: String) = sendPacket(ServerBanUserPacket(toUUID(target)))

    /**
     * Unban user from server
     */
    fun unbanUser(target: String) = sendPacket(ServerUnbanUserPacket(toUUID(target)))

    /**
     * Convert username or uuid to UUID
     */
    private fun toUUID(target: String): String {
        return try {
            UUID.fromString(target)

            target
        } catch (_: IllegalArgumentException) {
            val incomingUUID = GameProfileRepository().fetchUuidByUsername(target)
            incomingUUID.toString()
        }
    }

    /**
     * Login to web socket via JWT
     */
    fun loginViaJwt(token: String) {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))
        sendPacket(ServerLoginJWTPacket(token, allowMessages = true))
    }

    /**
     * Send packet to server
     */
    internal fun sendPacket(packet: Packet) {
        channel?.writeAndFlush(TextWebSocketFrame(serializerGson.toJson(packet, Packet::class.java)))
    }

    private fun handleFunctionalPacket(packet: Packet) {
        when (packet) {
            is ClientMojangInfoPacket -> {
                EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))

                runCatching {
                    val sessionHash = packet.sessionHash

                    mc.sessionService.joinServer(mc.session.uuidOrNull, mc.session.accessToken, sessionHash)
                    sendPacket(
                        ServerLoginMojangPacket(
                            mc.session.username,
                            mc.session.uuidOrNull,
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

            is ClientMessagePacket -> EventManager.callEvent(ClientChatMessageEvent(packet.user, packet.content,
                ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT))
            is ClientPrivateMessagePacket -> EventManager.callEvent(ClientChatMessageEvent(packet.user, packet.content,
                ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT))
            is ClientErrorPacket -> {
                // TODO: Replace with translation
                EventManager.callEvent(ClientChatErrorEvent(translateErrorMessage(packet)))
            }
            is ClientSuccessPacket -> {
                when (packet.reason) {
                    "Login" -> {
                        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGED_IN))
                        loggedIn = true
                    }

                    // TODO: Replace with translation
                    "Ban" -> chat("§7[§a§lChat§7] §9Successfully banned user!")
                    "Unban" -> chat("§7[§a§lChat§7] §9Successfully unbanned user!")
                }
            }

            is ClientNewJWTPacket -> EventManager.callEvent(ClientChatJwtTokenEvent(packet.token))
        }
    }

    private fun translateErrorMessage(packet: ClientErrorPacket): String {
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
        val packet = deserializerGson.fromJson(message, Packet::class.java)
        handleFunctionalPacket(packet)
    }

}
