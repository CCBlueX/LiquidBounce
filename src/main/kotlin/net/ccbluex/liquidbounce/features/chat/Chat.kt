package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.features.chat.client.Client
import net.ccbluex.liquidbounce.features.chat.client.packet.*

import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.TranslatableText
import net.minecraft.util.Util
import java.util.*

object Chat : ToggleableConfigurable(null, "chat", true) {

    // Chat options

    private var jwtLogin by boolean("JWT", false)
    private var jwtToken by text("JWTToken", "")

    // Chat client

    internal val client = Client()

    var loggedIn = false
    val retryChronometer = Chronometer()

    private fun createCommand() = CommandBuilder
        .begin("chat")
        .parameter(
            ParameterBuilder
                .begin<String>("message")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .vararg()
                .build()
        )
        .handler { _, args ->
            sendMessage((args[0] as Array<*>).joinToString(" ") { it as String })
        }
        .build()

    init {
        ConfigSystem.root(this)
        CommandManager.addCommand(createCommand())
    }

    private val gameTick = handler<GameTickEvent> {
        if (retryChronometer.hasElapsed()) {
            return@handler
        }

        if (!client.connected) {
            connectAsync()
            retryChronometer.waitFor(1000 * 60) // wait for 60 seconds to retry.
        }
    }

    fun connectAsync() {
        if (!enabled) {
            return
        }

        // Async connecting using IO worker from Minecraft
        Util.getIoWorkerExecutor().execute {
            client.connect()
        }
    }

    /**
     * Disconnect from chat server
     */
    fun disconnect() {
        client.channel?.close()
        client.channel = null
    }

    fun reconnect(async: Boolean = false) {
        disconnect()
        connectAsync()
    }

    internal fun onConnect() {
        logger.info("Connecting to LiquidChat...")
        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.states.connecting"), NotificationEvent.Severity.INFO)
    }

    internal fun onConnected() {
        logger.info("Successfully connected to LiquidChat!")

        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.states.connected"), NotificationEvent.Severity.INFO)

        if (jwtLogin) {
            logger.info("Logging in via JWT...")
            loginViaJWT(jwtToken)
        } else {
            logger.info("Requesting to login into Mojang...")
            requestMojangLogin()
        }
    }

    internal fun onDisconnect() {
        client.channel = null
        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.states.disconnected"), NotificationEvent.Severity.INFO)
    }

    internal fun onLogon() {
        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.states.loggingIn"), NotificationEvent.Severity.INFO)
    }

    internal fun onLoggedIn() {
        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.states.loggedIn"), NotificationEvent.Severity.SUCCESS)
    }

    internal fun onClientError(packet: ClientErrorPacket) {
        // add translation support
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

        EventManager.callEvent(ClientChatErrorEvent(message))

        // todo: remove when ultralight chat has been implemented
        val player = mc.player

        if (player == null) {
            logger.info("[Chat] $message")
            return
        }

        player.sendMessage("§7[§a§lChat§7] §9$message".asText(), false)
    }

    internal fun onMessage(user: User, message: String) {
        EventManager.callEvent(ClientChatMessageEvent(user, message, ClientChatMessageEvent.ChatGroup.PUBLIC_CHAT))

        // todo: remove when ultralight chat has been implemented
        val player = mc.player

        if (player == null) {
            logger.info("[Chat] ${user.name}: $message")
            return
        }

        player.sendMessage("§7[§a§lChat§7] §9${user.name}: §r$message".asText(), false)
    }

    internal fun onPrivateMessage(user: User, message: String) {
        EventManager.callEvent(ClientChatMessageEvent(user, message, ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT))

        // todo: remove when ultralight chat has been implemented
        val player = mc.player

        if (player == null) {
            logger.info("[Chat] ${user.name}: $message")
            return
        }

        player.sendMessage("§7[§a§lChat§7] §9${user.name}: §r$message".asText(), false)
    }

    internal fun onError(cause: Throwable) {
        notification("LiquidChat", TranslatableText("liquidbounce.generic.notifyDeveloper"), NotificationEvent.Severity.ERROR)
        logger.error("LiquidChat error", cause)
    }

    internal fun onReceivedJwtToken(jwt: String) {
        notification("LiquidChat", TranslatableText("liquidbounce.liquidchat.jwtTokenReceived"), NotificationEvent.Severity.SUCCESS)

        // Set jwt token
        jwtLogin = true
        jwtToken = jwt

        // Reconnect to chat server
        reconnect(async = true)
    }



    /**
     * Request Mojang authentication details for login
     */
    internal fun requestMojangLogin() = client.sendPacket(ServerRequestMojangInfoPacket())

    /**
     * Login to web socket via JWT
     */
    internal fun loginViaJWT(token: String) {
        onLogon()
        client.sendPacket(ServerLoginJWTPacket(token, allowMessages = true))
    }

    /**
     * Handle incoming packets from chat client
     */
    internal fun onPacket(packet: Packet) {
        when (packet) {
            is ClientMojangInfoPacket -> {
                onLogon()

                try {
                    val sessionHash = packet.sessionHash

                    mc.sessionService.joinServer(mc.session.profile, mc.session.accessToken, sessionHash)
                    client.sendPacket(ServerLoginMojangPacket(mc.session.username, mc.session.profile.id, allowMessages = true))
                } catch (throwable: Throwable) {
                    onError(throwable)
                }
                return
            }
            is ClientMessagePacket -> onMessage(packet.user, packet.content)
            is ClientPrivateMessagePacket -> onPrivateMessage(packet.user, packet.content)
            is ClientErrorPacket -> onClientError(packet)
            is ClientSuccessPacket -> {
                when (packet.reason) {
                    "Login" -> {
                        onLoggedIn()
                        loggedIn = true
                    }
                    "Ban" -> chat("§7[§a§lChat§7] §9Successfully banned user!")
                    "Unban" -> chat("§7[§a§lChat§7] §9Successfully unbanned user!")
                }
            }
            is ClientNewJWTPacket -> onReceivedJwtToken(packet.token)
        }
    }



    /**
     * Send chat message to server
     */
    fun sendMessage(message: String) = client.sendPacket(ServerMessagePacket(message))

    /**
     * Send private chat message to server
     */
    fun sendPrivateMessage(username: String, message: String) = client.sendPacket(ServerPrivateMessagePacket(username, message))

    /**
     * Ban user from server
     */
    fun banUser(target: String) = client.sendPacket(ServerBanUserPacket(toUUID(target)))

    /**
     * Unban user from server
     */
    fun unbanUser(target: String) = client.sendPacket(ServerUnbanUserPacket(toUUID(target)))

    /**
     * Convert username or uuid to UUID
     */
    private fun toUUID(target: String): String {
        return try {
            UUID.fromString(target)

            target
        } catch (_: IllegalArgumentException) {
            val incomingUUID = MojangApi.getUUID(target)

            if (incomingUUID.isBlank()) return ""

            val uuid = StringBuffer(incomingUUID)
                .insert(20, '-')
                .insert(16, '-')
                .insert(12, '-')
                .insert(8, '-')

            uuid.toString()
        }
    }

}
