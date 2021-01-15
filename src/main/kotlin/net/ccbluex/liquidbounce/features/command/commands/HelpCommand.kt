package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.chat

object HelpCommand {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .description("list commands.")
            .handler { args ->

                for (a in 0 .. (CommandManager.commands.size / 2)/*Divide it by 2 so it does not show the commands twice*/) {
                    var command = CommandManager.commands.get(a);
                chat(command.getFullName() + " - " + command.description);
            }
                true
            }
            .build()
    }
}
