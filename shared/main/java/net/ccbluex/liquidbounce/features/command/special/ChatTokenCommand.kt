package net.ccbluex.liquidbounce.features.command.special

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.chat.packet.packets.ServerRequestJWTPacket
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.module.modules.misc.LiquidChat
import net.ccbluex.liquidbounce.utils.misc.StringUtils
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

class ChatTokenCommand : Command("chattoken")
{

    private val lChat = LiquidBounce.moduleManager[LiquidChat::class.java] as LiquidChat

    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        if (args.size > 1)
        {
            when (args[1].toLowerCase())
            {
                "set" ->
                {
                    if (args.size > 2)
                    {
                        LiquidChat.jwtToken = StringUtils.toCompleteString(args, 2)
                        lChat.jwtValue.set(true)

                        if (lChat.state)
                        {
                            lChat.state = false
                            lChat.state = true
                        }
                    }
                    else chatSyntax(thePlayer, "chattoken set <token>")
                }

                "generate" ->
                {
                    if (!lChat.state)
                    {
                        chat(thePlayer, "\u00A7cError: \u00A77LiquidChat is disabled!")
                        return
                    }

                    lChat.client.sendPacket(ServerRequestJWTPacket())
                }

                "copy" ->
                {
                    if (LiquidChat.jwtToken.isEmpty())
                    {
                        chat(thePlayer, "\u00A7cError: \u00A77No token set! Generate one first using '${LiquidBounce.commandManager.prefix}chattoken generate'.")
                        return
                    }
                    val stringSelection = StringSelection(LiquidChat.jwtToken)
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, stringSelection)
                    chat(thePlayer, "\u00A7aCopied to clipboard!")
                }
            }
        }
        else chatSyntax(thePlayer, "chattoken <set/copy/generate>")
    }

    override fun tabComplete(args: Array<String>): List<String>
    {
        if (args.isEmpty()) return emptyList()

        return when (args.size)
        {
            1 -> arrayOf("set", "generate", "copy").filter { it.startsWith(args[0], ignoreCase = true) }
            else -> emptyList()
        }
    }
}
