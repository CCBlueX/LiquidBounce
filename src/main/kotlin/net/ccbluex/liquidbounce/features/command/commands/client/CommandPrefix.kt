package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat

object CommandPrefix {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("prefix")
            .description("Allows you to change the command prefix")
            .parameter(
                ParameterBuilder
                .begin<String>("prefix")
                .description("The new prefix")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build())
            .handler { args ->
                val prefix = args[0] as String
                CommandManager.prefix = prefix
                chat("Successfully changed prefix to '$prefix'.")
            }
            .build()
    }

}
