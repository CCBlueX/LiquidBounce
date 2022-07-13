package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat

class ChatAdminCommand : Command("chatadmin")
{

    private val lChat = LiquidBounce.moduleManager[LiquidChat::class.java] as LiquidChat

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (!lChat.state)
        {
            chat(thePlayer, "\u00A7cError: \u00A77LiquidChat is disabled!")
            return
        }

        if (args.size > 1)
        {
            when (args[1].lowercase())
            {
                "ban" ->
                {
                    if (args.size > 2)
                    {
                        lChat.client.banUser(args[2])
                    }
                    else chatSyntax(thePlayer, "chatadmin ban <username>")
                }

                "unban" ->
                {
                    if (args.size > 2)
                    {
                        lChat.client.unbanUser(args[2])
                    }
                    else chatSyntax(thePlayer, "chatadmin unban <username>")
                }
            }
        }
        else chatSyntax(thePlayer, "chatadmin <ban/unban>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        return when (args.size)
        {
            1 -> arrayOf("ban", "unban").filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
