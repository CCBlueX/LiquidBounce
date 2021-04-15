package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.features.chat.client.Client
import net.ccbluex.liquidbounce.features.chat.client.ClientListener
import net.ccbluex.liquidbounce.features.chat.client.packet.User
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.TranslatableText
import net.minecraft.util.Formatting

object Chat : ToggleableConfigurable(null, "chat", true), ClientListener {

    private var jwtLogin by boolean("JWT", false)
    private var jwtToken by text("JWTToken", "")

    val client = Client(this)

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
            client.sendMessage((args[0] as Array<*>).joinToString(" ") { it as String })
        }
        .build()

    init {
        ConfigSystem.root(this)
        CommandManager.addCommand(createCommand())
    }

    fun connect() {
        if (!enabled) {
            return
        }

        client.connect()
        client.loginMojang()
    }

    override fun onConnect() {
        chat("§7[§a§lChat§7]".asText(), TranslatableText("liquidbounce.liquidchat.states.connecting").styled { it.withColor(Formatting.BLUE) })
    }

    override fun onConnected() {
        chat("§7[§a§lChat§7]".asText(), TranslatableText("liquidbounce.liquidchat.states.connected").styled { it.withColor(Formatting.BLUE) })
    }

    override fun onDisconnect() {
        chat("§7[§a§lChat§7]".asText(), TranslatableText("liquidbounce.liquidchat.states.disconnected").styled { it.withColor(Formatting.RED) })
    }

    override fun onLogon() {
        chat("§7[§a§lChat§7]".asText(), TranslatableText("liquidbounce.liquidchat.states.loggingIn").styled { it.withColor(Formatting.BLUE) })
    }

    override fun onLoggedIn() {
        chat("§7[§a§lChat§7]".asText(), TranslatableText("liquidbounce.liquidchat.states.loggedIn").styled { it.withColor(Formatting.BLUE) })

        chat("====================================")
        chat("§c>> §l")
        chat(regular(TranslatableText("liquidbounce.liquidchat.writeMessage", ".chat <message>".asText().styled { it.withColor(Formatting.GREEN) })))
        chat(regular(TranslatableText("liquidbounce.liquidchat.writePrivateMessage", ".pchat <user> <message>".asText().styled { it.withColor(Formatting.GREEN) })))
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
        chat("§7[§a§lChat§7] §c§l${TranslatableText("liquidbounce.generic.error").asString()}: §7${cause.javaClass.name}: ${cause.message}")
    }

}
