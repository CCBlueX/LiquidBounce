package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat
import net.ccbluex.liquidbounce.utils.misc.StringUtils

object LiquidChatCommand : Command("chat", "lc", "irc") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (!LiquidChat.state) {
                chat("§cError: §7LiquidChat is disabled!")
                return
            }

            if (!LiquidChat.client.isConnected()) {
                chat("§cError: §7LiquidChat is currently not connected to the server!")
                return
            }

            val message = StringUtils.toCompleteString(args, 1)

            LiquidChat.client.sendMessage(message)
        } else
            chatSyntax("chat <message>")
    }
}