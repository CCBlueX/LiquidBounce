package net.ccbluex.liquidbounce.features.command.commands

import joptsimple.internal.Strings
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.utils.ClientUtils

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
class HelpCommand : Command("help", emptyArray()) {
    override fun execute(args: Array<String>) {
        var page = 1
        val maxPageDouble = LiquidBounce.commandManager.commands.size.toDouble() / 8.0
        val maxPage = if (maxPageDouble > maxPageDouble.toInt()) maxPageDouble.toInt() + 1 else maxPageDouble.toInt()

        if (args.size > 1) {
            try {
                page = args[1].toInt()
            } catch (e: NumberFormatException) {
                chatSyntaxError()
            }
        }

        if (page <= 0) {
            chat("The number you have entered is too low, it must be over 0")
            return
        }

        if (page > maxPage) {
            chat("The number you have entered is too big, it must be under $maxPage.")
            return
        }

        chat("§c§lHelp")
        ClientUtils.displayChatMessage("§7> Page: §8$page / $maxPage")

        val commands = LiquidBounce.commandManager.commands.sortedBy { it.command }

        var i = 8 * (page - 1)
        while (i < 8 * page && i < commands.size) {
            val command = commands[i]

            ClientUtils.displayChatMessage("§6> §7${LiquidBounce.commandManager.prefix}${command.command}${if (command.alias.isEmpty()) "" else " §7(§8" + Strings.join(command.alias, "§7, §8") + "§7)"}")
            i++
        }

        ClientUtils.displayChatMessage("§a------------\n§7> §c${LiquidBounce.commandManager.prefix}help §8<§7§lpage§8>")
    }
}