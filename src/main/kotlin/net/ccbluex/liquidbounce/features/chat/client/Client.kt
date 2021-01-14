/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
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
import net.ccbluex.liquidbounce.features.chat.client.packet.*
import net.ccbluex.liquidbounce.utils.ProfileUtils
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.mc
import java.net.URI
import java.util.*

class Client(val listener: ClientListener) {

    internal var channel: Channel? = null

    var username = ""
    var jwt = false
    var loggedIn = false

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

    /**
     * Connect to chat server
     */
    fun connect() {
        listener.onConnect()

        val uri = URI("wss://chat.liquidbounce.net:7886/ws")

        val ssl = uri.scheme.equals("wss", true)
        val sslContext = if(ssl) SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE) else null

        val group = NioEventLoopGroup()
        val handler = ChannelHandler(this, WebSocketClientHandshakerFactory.newHandshaker(
                uri, WebSocketVersion.V13, null,
                true, DefaultHttpHeaders()))

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

                        if(sslContext != null) {
                            pipeline.addLast(sslContext.newHandler(ch.alloc()))
                        }

                        pipeline.addLast(HttpClientCodec(), HttpObjectAggregator(8192), handler)
                    }

                })

        channel = bootstrap.connect(uri.host, uri.port).sync().channel()
        handler.handshakeFuture.sync()

        if (isConnected()) {
            listener.onConnected()
        }
    }

    /**
     * Disconnect from chat server
     */
    fun disconnect() {
        channel?.close()
        channel = null
        username = ""
        jwt = false
    }

    /**
     * Login to web socket
     */
    fun loginMojang() = sendPacket(ServerRequestMojangInfoPacket())

    /**
     * Login to web socket
     */
    fun loginJWT(token: String) {
        listener.onLogon()
        sendPacket(ServerLoginJWTPacket(token, allowMessages = true))
        jwt = true
    }

    fun isConnected() = channel != null && channel!!.isOpen

    /**
     * Handle incoming message of websocket
     */
    internal fun onMessage(message: String) {
        val gson = GsonBuilder()
                .registerTypeAdapter(Packet::class.java, deserializer)
                .create()

        val packet = gson.fromJson(message, Packet::class.java)
        when(packet) {
            is ClientMojangInfoPacket -> {
                listener.onLogon()

                try {
                    val sessionHash = packet.sessionHash

                    mc.sessionService.joinServer(mc.session.profile, mc.session.accessToken, sessionHash)
                    username = mc.session.username
                    jwt = false

                    sendPacket(ServerLoginMojangPacket(mc.session.username, mc.session.profile.id, allowMessages = true))
                }catch (throwable: Throwable) {
                    listener.onError(throwable)
                }
                return
            }
            is ClientMessagePacket -> listener.onMessage(packet.user, packet.content)
            is ClientPrivateMessagePacket -> listener.onPrivateMessage(packet.user, packet.content)
            is ClientErrorPacket -> {
                val message = when (packet.message) {
                    "NotSupported" -> "This method is not supported!"
                    "LoginFailed" -> "Login Failed!"
                    "NotLoggedIn" -> "You must be logged in to use the chat! Enable LiquidChat."
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

                chat("§7[§a§lChat§7] §cError: §7$message")
            }
            is ClientSuccessPacket -> {
                when (packet.reason) {
                    "Login" -> {
                        listener.onLoggedIn()
                        loggedIn = true
                    }
                    "Ban" -> chat("§7[§a§lChat§7] §9Successfully banned user!")
                    "Unban" -> chat("§7[§a§lChat§7] §9Successfully unbanned user!")
                }
            }

            is ClientNewJWTPacket -> {

            }
        }
    }

    /**
     * Send packet to server
     */
    fun sendPacket(packet: Packet) {
        val gson = GsonBuilder()
                .registerTypeAdapter(Packet::class.java, serializer)
                .create()

        channel?.writeAndFlush(TextWebSocketFrame(gson.toJson(packet, Packet::class.java)))
    }

    /**
     * Send chat message to server
     */
    fun sendMessage(message: String) = sendPacket(ServerMessagePacket(message))

    /**
     * Send private chat message to server
     */
    fun sendPrivateMessage(username: String, message: String) = sendPacket(ServerPrivateMessagePacket(username, message))

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
        }catch (_: IllegalArgumentException) {
            val incomingUUID = ProfileUtils.getUUID(target)

            if(incomingUUID.isBlank()) return ""

            val uuid = StringBuffer(incomingUUID)
                    .insert(20, '-')
                    .insert(16, '-')
                    .insert(12, '-')
                    .insert(8, '-')

            uuid.toString()
        }
    }

}

interface ClientListener {
    fun onConnect()
    fun onConnected()
    fun onDisconnect()
    fun onLogon()
    fun onLoggedIn()
    fun onMessage(user: User, message: String)
    fun onPrivateMessage(user: User, message: String)
    fun onError(cause: Throwable)
}
