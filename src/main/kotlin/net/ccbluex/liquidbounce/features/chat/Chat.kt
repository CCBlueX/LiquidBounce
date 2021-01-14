package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.config.Configurable
import net.ccbluex.liquidbounce.config.boolean
import net.ccbluex.liquidbounce.config.text
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.features.chat.client.Client
import net.ccbluex.liquidbounce.features.chat.client.ClientListener
import net.ccbluex.liquidbounce.features.chat.client.packet.User
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.extensions.asText
import net.ccbluex.liquidbounce.utils.logger
import net.ccbluex.liquidbounce.utils.mc

class Chat : Configurable("chat"), ClientListener, Listenable {

    private val enabled by boolean("enabled", true)
    private var jwtLogin by boolean("JWT", false)
    private var jwtToken by text("JWTToken")

    val client = Client(this)

    private fun createCommand() = CommandBuilder
        .begin("chat")
        .description("Allows you to chat")
        .parameter(
            ParameterBuilder
                .begin<String>("message")
                .description("Message to send")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build())
        .handler { args ->
            client.sendMessage(args[0] as String)
            true
        }
        .build()

    init {
        LiquidBounce.configSystem.root(this)
        LiquidBounce.commandManager.addCommand(createCommand())
    }

    fun connect() {
        if (!enabled)
            return

        client.connect()
        client.loginMojang()
    }

    override fun onConnect() {
        chat("§7[§a§lChat§7] §9Connecting to chat server...")
    }

    override fun onConnected() {
        chat("§7[§a§lChat§7] §9Connected to chat server!")
    }

    override fun onDisconnect() {
        chat("§7[§a§lChat§7] §cDisconnected from chat server!")
    }

    override fun onLogon() {
        chat("§7[§a§lChat§7] §9Logging in...")
    }

    override fun onLoggedIn() {
        chat("§7[§a§lChat§7] §9Logged in!")

        chat("====================================")
        chat("§c>> §l")
        chat("§7Write message: §a.chat <message>")
        chat("§7Write private message: §a.pchat <user> <message>")
        chat("====================================")
    }

    override fun onMessage(user: User, message: String) {
        val player = mc.player

        if (player == null) {
            logger.info("[Chat] ${user.name}: $message")
            return
        }

        player.sendMessage("§7[§a§lChat§7] §9${user.name}: §r$message".asText(), false)
    }

    override fun onPrivateMessage(user: User, message: String) {
        val player = mc.player

        if (player == null) {
            logger.info("[Chat] (P) ${user.name}: $message")
            return
        }

        player.sendMessage("§7[§a§lChat§7] §c(P) §§9${user.name}: $message".asText(), false)
    }

    override fun onError(cause: Throwable) {
        chat("§7[§a§lChat§7] §c§lError: §7${cause.javaClass.name}: ${cause.message}")
    }

    override fun handleEvents() = enabled

}
