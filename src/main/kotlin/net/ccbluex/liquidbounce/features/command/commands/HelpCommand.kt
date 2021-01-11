package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.chat

object HelpCommand {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .description("list commands.")
            .handler { args ->
                chat("friend");
                chat("toggle");
                chat("bind");
                true
            }
            .build()
    }
}
