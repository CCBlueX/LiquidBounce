/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015-2024 CCBlueX
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

import com.google.gson.GsonBuilder
import com.mojang.authlib.exceptions.InvalidCredentialsException
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
import net.ccbluex.liquidbounce.authlib.yggdrasil.GameProfileRepository
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.chat.packet.*
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.minecraft.util.Util
import java.net.URI
import java.util.*

class ChatClient {

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

    private var isConnecting = false
    var loggedIn = false

    fun connectAsync() {
        if (isConnecting || connected) {
            return
        }

        // Async connecting using IO worker from Minecraft
        Util.getIoWorkerExecutor().execute {
            connect()
        }
    }

    /**
     * Connect to chat server via websocket.
     * Supports SSL and non-SSL connections.
     * Be aware SSL takes insecure certificates.
     */
    fun connect() = runCatching {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.CONNECTING))
        isConnecting = true
        loggedIn = false

        val uri = URI("wss://chat.liquidbounce.net:7886/ws")

        val ssl = uri.scheme.equals("wss", true)
        val sslContext = if (ssl) {
            SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE)
        } else {
            null
        }

        val group = NioEventLoopGroup()
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
        val gson = GsonBuilder()
            .registerTypeAdapter(Packet::class.java, serializer)
            .create()

        channel?.writeAndFlush(TextWebSocketFrame(gson.toJson(packet, Packet::class.java)))
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

                EventManager.callEvent(ClientChatErrorEvent(message))
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


    /**
     * Handle incoming message of websocket
     */
    internal fun handlePlainMessage(message: String) {
        val gson = GsonBuilder()
            .registerTypeAdapter(Packet::class.java, deserializer)
            .create()

        val packet = gson.fromJson(message, Packet::class.java)
        handleFunctionalPacket(packet)
    }

}
