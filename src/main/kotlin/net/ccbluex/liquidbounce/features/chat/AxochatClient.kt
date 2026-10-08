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
import io.netty.channel.ChannelInitializer
import io.netty.channel.ChannelPromise
import io.netty.channel.SimpleChannelInboundHandler
import io.netty.channel.socket.SocketChannel
import io.netty.handler.codec.http.DefaultHttpHeaders
import io.netty.handler.codec.http.FullHttpResponse
import io.netty.handler.codec.http.HttpClientCodec
import io.netty.handler.codec.http.HttpObjectAggregator
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshaker
import io.netty.handler.codec.http.websocketx.WebSocketClientHandshakerFactory
import io.netty.handler.codec.http.websocketx.WebSocketHandshakeException
import io.netty.handler.codec.http.websocketx.WebSocketVersion
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.withTimeoutOrNull
import net.ccbluex.liquidbounce.api.thirdparty.lookupUuidByName
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatErrorEvent
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.features.chat.packet.AxochatPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SBanUserPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SBlockPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SChatMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SFriendPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SGroupPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SHelloPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SLocationPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SLoginAccountPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SLoginMojangPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPardonPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyStatePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPrivateMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPunishPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SReportPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestMojangInfoPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestPunishmentsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestReportsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestUserCountPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SResolveReportPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SSettingsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SSightingsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SUnbanUserPacket
import net.ccbluex.liquidbounce.features.chat.packet.PacketDeserializer
import net.ccbluex.liquidbounce.features.chat.packet.PacketSerializer
import net.ccbluex.liquidbounce.features.chat.packet.S2CBlocksPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CChatMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CErrorPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CFriendsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CGroupsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CHelloPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CMojangInfoPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyInvitePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyMemberStatePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyWarpPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPresencePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPrivateMessagePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPunishedPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPunishmentsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CReportCreatedPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CReportsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CSettingsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CSuccessPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CUserCountPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CWelcomePacket
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.logger
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.netty.clientChannelAndGroup
import net.ccbluex.liquidbounce.utils.netty.syncSuspend
import java.net.URI
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

private const val MAX_FRAME_SIZE = 256 * 1024
private const val PROTOCOL_VERSION = 2

private val KNOWN_ERRORS = setOf(
    "NotSupported", "LoginFailed", "NotLoggedIn", "AlreadyLoggedIn", "MojangRequestMissing", "NotPermitted",
    "NotBanned", "Banned", "RateLimited", "PrivateMessageNotAccepted", "EmptyMessage", "MessageTooLong",
    "InvalidCharacter", "InvalidId", "Internal", "UnknownUser", "UnknownChannel", "UnknownGroup", "Muted",
    "NotInParty", "AlreadyInParty", "PartyFull", "PartyLocked", "NoInvite", "NotFriends", "AlreadyFriends",
    "RequestsDisabled", "GroupFull", "InvalidName", "TooLarge",
)

class AxochatClient(private val allowMessages: () -> Boolean) {

    private var channel: Channel? = null

    private val serializer = PacketSerializer().apply {
        register<C2SRequestMojangInfoPacket>("RequestMojangInfo")
        register<C2SLoginMojangPacket>("LoginMojang")
        register<C2SMessagePacket>("Message")
        register<C2SPrivateMessagePacket>("PrivateMessage")
        register<C2SBanUserPacket>("BanUser")
        register<C2SUnbanUserPacket>("UnbanUser")
        register<C2SRequestUserCountPacket>("RequestUserCount")
        register<C2SHelloPacket>("Hello")
        register<C2SLoginAccountPacket>("LoginAccount")
        register<C2SSettingsPacket>("Settings")
        register<C2SChatMessagePacket>("ChatMessage")
        register<C2SFriendPacket>("Friend")
        register<C2SBlockPacket>("Block")
        register<C2SGroupPacket>("Group")
        register<C2SPartyPacket>("Party")
        register<C2SLocationPacket>("Location")
        register<C2SSightingsPacket>("Sightings")
        register<C2SPartyStatePacket>("PartyState")
        register<C2SReportPacket>("Report")
        register<C2SPunishPacket>("Punish")
        register<C2SPardonPacket>("Pardon")
        register<C2SRequestPunishmentsPacket>("RequestPunishments")
        register<C2SRequestReportsPacket>("RequestReports")
        register<C2SResolveReportPacket>("ResolveReport")
    }

    private val deserializer = PacketDeserializer().apply {
        register<S2CMojangInfoPacket>("MojangInfo")
        register<S2CMessagePacket>("Message")
        register<S2CPrivateMessagePacket>("PrivateMessage")
        register<S2CErrorPacket>("Error")
        register<S2CSuccessPacket>("Success")
        register<S2CUserCountPacket>("UserCount")
        register<S2CHelloPacket>("Hello")
        register<S2CWelcomePacket>("Welcome")
        register<S2CSettingsPacket>("Settings")
        register<S2CChatMessagePacket>("ChatMessage")
        register<S2CFriendsPacket>("Friends")
        register<S2CPresencePacket>("Presence")
        register<S2CBlocksPacket>("Blocks")
        register<S2CGroupsPacket>("Groups")
        register<S2CPartyPacket>("Party")
        register<S2CPartyInvitePacket>("PartyInvite")
        register<S2CPartyWarpPacket>("PartyWarp")
        register<S2CPartyMemberStatePacket>("PartyMemberState")
        register<S2CPunishedPacket>("Punished")
        register<S2CPunishmentsPacket>("Punishments")
        register<S2CReportsPacket>("Reports")
        register<S2CReportCreatedPacket>("ReportCreated")
    }

    val isConnected: Boolean
        get() = channel != null && channel!!.isOpen

    private var isConnecting = false
    var isLoggedIn = false
        private set

    /**
     * Negotiated protocol version; 1 until the server answers [C2SHelloPacket].
     */
    @Volatile
    var protocol = 1
        private set

    val isModern: Boolean
        get() = protocol >= PROTOCOL_VERSION

    @Volatile
    private var helloReply: CompletableDeferred<Int>? = null

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
        protocol = 1

        val uri = URI(System.getProperty("net.ccbluex.liquidbounce.chat.url", "wss://chat.liquidbounce.net:7886/ws"))

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
                MAX_FRAME_SIZE,
            )
        )

        val bootstrap = Bootstrap()

        bootstrap.clientChannelAndGroup(true)
            .handler(object : ChannelInitializer<SocketChannel>() {
                override fun initChannel(ch: SocketChannel) {
                    val pipeline = ch.pipeline()

                    if (sslContext != null) {
                        pipeline.addLast(sslContext.newHandler(ch.alloc()))
                    }

                    pipeline.addLast(HttpClientCodec(), HttpObjectAggregator(MAX_FRAME_SIZE), handler)
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
        protocol = 1
    }

    suspend fun reconnect() {
        disconnect()
        connect()
    }

    /**
     * Offers protocol v2. Older servers drop the unknown packet, so the session stays on v1
     * when no answer arrives in time.
     */
    suspend fun negotiate(): Int {
        val reply = CompletableDeferred<Int>()
        helloReply = reply
        sendPacket(C2SHelloPacket(PROTOCOL_VERSION))
        protocol = withTimeoutOrNull(3.seconds) { reply.await() }?.coerceAtMost(PROTOCOL_VERSION) ?: 1
        helloReply = null
        return protocol
    }

    /**
     * Request Mojang authentication details for login
     */
    fun requestMojangLogin() = sendPacket(C2SRequestMojangInfoPacket())

    /**
     * Sends [message] to [channel]. v1 servers only know the global channel and direct messages.
     *
     * @return false if the channel needs protocol v2
     */
    fun sendMessage(channel: String, message: String): Boolean {
        when {
            isModern -> sendPacket(C2SChatMessagePacket(channel, message))
            channel == ChatSession.GLOBAL -> sendPacket(C2SMessagePacket(message))
            channel.startsWith(ChatSession.USER_PREFIX) ->
                sendPacket(C2SPrivateMessagePacket(channel.removePrefix(ChatSession.USER_PREFIX), message))
            else -> return false
        }
        return true
    }

    /**
     * Ban user from server
     */
    suspend fun banUser(target: String) = sendPacket(C2SBanUserPacket(toUUID(target)))

    /**
     * Unban user from server
     */
    suspend fun unbanUser(target: String) = sendPacket(C2SUnbanUserPacket(toUUID(target)))

    /**
     * Convert username or uuid to UUID
     */
    private suspend fun toUUID(target: String): String {
        return try {
            UUID.fromString(target)

            target
        } catch (_: IllegalArgumentException) {
            val incomingUUID = lookupUuidByName(target)
            incomingUUID.toString()
        }
    }

    /**
     * Login with a LiquidBounce Account access token (v2 only)
     */
    fun loginAccount(accessToken: String) {
        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGING_IN))
        sendPacket(C2SLoginAccountPacket(accessToken, allowMessages = allowMessages()))
    }

    /**
     * Send packet to server
     */
    fun sendPacket(packet: AxochatPacket.C2S) {
        channel?.writeAndFlush(TextWebSocketFrame(serializerGson.toJson(packet, AxochatPacket.C2S::class.java)))
    }

    @Suppress("CyclomaticComplexMethod")
    private fun handleFunctionalPacket(packet: AxochatPacket.S2C) {
        when (packet) {
            is S2CHelloPacket -> {
                helloReply?.complete(packet.protocol)
                return
            }

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
                            allowMessages = allowMessages()
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
            is S2CChatMessagePacket -> EventManager.callEvent(ClientChatMessageEvent(
                packet.author.toAxoUser(),
                packet.content,
                chatGroupOf(packet.channel),
                packet.channel,
                packet.author,
                packet.id,
            ))
            is S2CErrorPacket -> EventManager.callEvent(ClientChatErrorEvent(translateError(packet), packet.code))
            is S2CSuccessPacket -> {
                when (packet.reason) {
                    "Login" -> {
                        isLoggedIn = true
                        EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.LOGGED_IN))
                    }

                    // TODO: Replace with translation
                    "Ban" -> chat("§7[§a§lChat§7] §9Successfully banned user!")
                    "Unban" -> chat("§7[§a§lChat§7] §9Successfully unbanned user!")
                }
            }

            else -> {}
        }

        EventManager.callEvent(ClientChatPacketEvent(packet))
    }

    private fun chatGroupOf(channel: String) = when {
        channel == ChatSession.SERVER -> ClientChatMessageEvent.ChatGroup.SERVER_CHAT
        channel == ChatSession.PARTY -> ClientChatMessageEvent.ChatGroup.PARTY_CHAT
        channel.startsWith(ChatSession.GROUP_PREFIX) -> ClientChatMessageEvent.ChatGroup.GROUP_CHAT
        channel.startsWith(ChatSession.USER_PREFIX) -> ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT
        else -> ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT
    }

    private fun translateError(packet: S2CErrorPacket): String {
        val code = packet.code
        if (code !in KNOWN_ERRORS) {
            return listOfNotNull(code, packet.details).joinToString(": ")
        }

        val key = "liquidbounce.liquidchat.error.${code.replaceFirstChar(Char::lowercaseChar)}"
        return translation(key, packet.details ?: "").string
    }

    /**
     * Handle incoming message of websocket
     */
    internal fun handlePlainMessage(message: String) {
        val packet = runCatching {
            deserializerGson.fromJson(message, AxochatPacket.S2C::class.java)
        }.onFailure {
            logger.warn("Malformed LiquidChat packet", it)
        }.getOrNull() ?: return

        handleFunctionalPacket(packet)
    }

    private inner class ChannelHandler(
        private val handshaker: WebSocketClientHandshaker,
    ) : SimpleChannelInboundHandler<Any>() {

        lateinit var handshakeFuture: ChannelPromise

        override fun handlerAdded(ctx: ChannelHandlerContext) {
            handshakeFuture = ctx.newPromise()
        }

        override fun channelActive(ctx: ChannelHandlerContext) {
            handshaker.handshake(ctx.channel())
        }

        override fun channelInactive(ctx: ChannelHandlerContext) {
            isLoggedIn = false
            protocol = 1
            EventManager.callEvent(ClientChatStateChange(ClientChatStateChange.State.DISCONNECTED))
        }

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
                is PingWebSocketFrame -> channel.writeAndFlush(PongWebSocketFrame(msg.content().retain()))
                is CloseWebSocketFrame -> channel.close()
            }
        }
    }

}
