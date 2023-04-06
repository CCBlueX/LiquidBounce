package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat

class ChatAdminCommand : Command("chatadmin") {

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (!LiquidChat.state) {
            chat("§cError: §7LiquidChat is disabled!")
            return
        }

        if (args.size > 1) {
            when {
                args[1].equals("ban", true) -> {
                    if (args.size > 2) {
                        LiquidChat.client.banUser(args[2])
                    } else
                        chatSyntax("chatadmin ban <username>")
                }

                args[1].equals("unban", true) -> {
                    if (args.size > 2) {
                        LiquidChat.client.unbanUser(args[2])
                    } else
                        chatSyntax("chatadmin unban <username>")
                }
            }
        } else
            chatSyntax("chatadmin <ban/unban>")
    }

    override fun tabComplete(args: Array<String>): List<String> {
        if (args.isEmpty())
            return emptyList()

        return when (args.size) {
            1 -> {
                arrayOf("ban", "unban")
                        .map { it.lowercase() }
                        .filter { it.startsWith(args[0], true) }
            }
            else -> emptyList()
        }
    }
}