package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.chat
import net.minecraft.client.util.InputUtil

object CommandBind {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("bind")
            .description("Allows you to set keybinds")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .description("The name of the module")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            ).parameter(
                ParameterBuilder
                    .begin<String>("key")
                    .description("The new key to bind")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { args ->
                val name = args[0] as String
                val key = args[1] as String
                val module = ModuleManager.find { it.name.equals(name, true) }
                    ?: throw CommandException("Module §b§l${args[1]}§c not found.")

                val bindKey = runCatching {
                    InputUtil.fromTranslationKey("key.keyboard.${key.toLowerCase()}")
                }.getOrElse { InputUtil.UNKNOWN_KEY }

                module.bind = bindKey
                chat("Bound module §a§l${module.name}§3 to key §a§l${bindKey.localizedText.toString()}§3.")
                true
            }
            .build()
    }
}
