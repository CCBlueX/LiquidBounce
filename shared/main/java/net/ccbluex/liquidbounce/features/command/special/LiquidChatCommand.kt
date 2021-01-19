package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat
import net.ccbluex.liquidbounce.utils.misc.StringUtils

class LiquidChatCommand : Command("chat", "lc", "irc") {

    private val lChat = LiquidBounce.moduleManager.getModule(LiquidChat::class.java) as LiquidChat

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (args.size > 1) {
            if (!lChat.state) {
                chat("\u00A7cError: \u00A77LiquidChat is disabled!")
                return
            }

            if (!lChat.client.isConnected()) {
                chat("\u00A7cError: \u00A7LiquidChat is currently not connected to the server!")
                return
            }

            val message = StringUtils.toCompleteString(args, 1)

            lChat.client.sendMessage(message)
        } else
            chatSyntax("chat <message>")
    }
}
