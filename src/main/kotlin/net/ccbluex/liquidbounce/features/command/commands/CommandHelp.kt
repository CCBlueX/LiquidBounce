package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import kotlin.math.ceil
import kotlin.math.roundToInt

object CommandHelp {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .description("Shows a list of commands")
            .parameter(
                ParameterBuilder
                    .begin<Int>("page")
                    .description("The page to show")
                    .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                    .optional()
                    .build()
            )
            .handler { args ->
                val page = if (args.size > 1) {
                    args[0] as Int
                }else {
                    1
                }.coerceAtLeast(1)

                // Max page
                val maxPage = ceil(CommandManager.commands.size / 8.0).roundToInt()
                if (page > maxPage) {
                    throw CommandException("The number you have entered is too big, it must be under $maxPage.")
                }

                // Print out help page
                val helpOut = StringBuilder()
                helpOut.append("§c§lHelp\n")
                helpOut.append("§7> Page: §8$page / $maxPage\n")

                val commands = CommandManager.commands.sortedBy { it.name }

                val iterPage = 8 * page
                for (command in commands.subList(iterPage - 8, iterPage.coerceAtMost(commands.size))){
                    val aliases = if (command.aliases.isEmpty()) "" else " §7(§8${command.aliases.joinToString("§7, §8")}§7)"
                    helpOut.append("§6> §7${CommandManager.prefix}${command.name}$aliases\n")
                }

                helpOut.append("§a------------\n§7> §c${CommandManager.prefix}help §8<§7§lpage§8>")
                chat(helpOut.toString())
                true
            }
            .build()
    }
}
