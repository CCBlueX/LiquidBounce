package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat
import net.ccbluex.liquidbounce.utils.misc.StringUtils

object LiquidChatCommand : Command("chat", "lc", "irc") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        val usedAlias = args[0].lowercase()

        if (args.size <= 1) {
            chatSyntax("$usedAlias <message>")
            return
        }

        if (!LiquidChat.state) {
            chat("§cError: §7LiquidChat is disabled!")
            return
        }

        if (!LiquidChat.client.isConnected()) {
            chat("§cError: §LiquidChat is currently not connected to the server!")
            return
        }

        val message = StringUtils.toCompleteString(args, 1)

        LiquidChat.client.sendMessage(message)
    }
}