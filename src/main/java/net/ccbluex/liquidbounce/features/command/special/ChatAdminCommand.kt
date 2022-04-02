package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat

class ChatAdminCommand : Command("chatadmin") {

    val lChat = LiquidBounce.moduleManager.getModule(LiquidChat::class.java) as LiquidChat

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>) {
        if (!lChat.state) {
            chat("§cError: §7LiquidChat is disabled!")
            return
        }

        if (args.size > 1) {
            when {
                args[1].equals("ban", true) -> {
                    if (args.size > 2) {
                        lChat.client.banUser(args[2])
                    } else
                        chatSyntax("chatadmin ban <username>")
                }

                args[1].equals("unban", true) -> {
                    if (args.size > 2) {
                        lChat.client.unbanUser(args[2])
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
                        .map { it.toLowerCase() }
                        .filter { it.startsWith(args[0], true) }
            }
            else -> emptyList()
        }
    }
}