/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.features.command.commands

import joptsimple.internal.Strings
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils

class HelpCommand : Command("help")
{
    /**
     * Execute commands with provided [args]
     */
    override fun execute(args: Array<String>)
    {
        val thePlayer = mc.thePlayer

        var page = 1

        if (args.size > 1)
        {
            try
            {
                page = args[1].toInt()
            }
            catch (e: NumberFormatException)
            {
                chatSyntaxError(thePlayer)
            }
        }

        if (page <= 0)
        {
            chat(thePlayer, "The number you have entered is too low, it must be over 0")
            return
        }

        val maxPageDouble = LiquidBounce.commandManager.commands.size * 0.125f
        val maxPage = if (maxPageDouble > maxPageDouble.toInt()) maxPageDouble.toInt() + 1
        else maxPageDouble.toInt()

        if (page > maxPage)
        {
            chat(thePlayer, "The number you have entered is too big, it must be under $maxPage.")
            return
        }


        chat(thePlayer, "\u00A7c\u00A7lHelp")
        ClientUtils.displayChatMessage(thePlayer, "\u00A77> Page: \u00A78$page / $maxPage")

        val commands = LiquidBounce.commandManager.commands.sortedBy(Command::command)

        var i = 8 * (page - 1)
        while (i < 8 * page && i < commands.size)
        {
            val command = commands[i]

            ClientUtils.displayChatMessage(thePlayer, "\u00A76> \u00A77${LiquidBounce.commandManager.prefix}${command.command}${if (command.alias.isEmpty()) "" else " \u00A77(\u00A78" + Strings.join(command.alias, "\u00A77, \u00A78") + "\u00A77)"}")
            i++
        }

        ClientUtils.displayChatMessage(thePlayer, "\u00A7a------------\n\u00A77> \u00A7c${LiquidBounce.commandManager.prefix}help \u00A78<\u00A77\u00A7lpage\u00A78>")
    }
}
