package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.defaultColor
import net.ccbluex.liquidbounce.utils.statusColor
import net.ccbluex.liquidbounce.utils.variableColor

object ToggleCommand {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("toggle")
            .description("Allows you to toggle modules")
            .parameter(ParameterBuilder
                .begin<String>("name")
                .description("The name of the module")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .required()
                .build())
            .handler { args ->
                val name = args[0] as String
                val module = LiquidBounce.moduleManager.find { it.name.equals(name, true) } ?: return@handler false

                module.state = !module.state
                chat("$variableColor${module.name} ${defaultColor}has been $statusColor${if (module.state) "enabled" else "disabled"}$variableColor.")
                true
            }
            .build()
    }

}
