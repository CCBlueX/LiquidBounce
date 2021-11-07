package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.modules.render.ModuleXRay
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.util.Identifier
import net.minecraft.util.registry.Registry

object CommandXRay {
    fun createCommand(): Command {
        return CommandBuilder
            .begin("xray")
            .parameter(
                ParameterBuilder
                    .begin<String>("option")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .parameter(
                ParameterBuilder
                    .begin<String>("id")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .required()
                    .build()
            )
            .handler { command, args ->
                val option = args[0] as String
                val id = args[1] as String

                if ("list".equals(option, true)) {
                    ModuleXRay.blocks.forEach { chat(it.name.toString()) }
                    return@handler
                }

                if ("add".equals(option, true)) {
                    val block = Registry.BLOCK.get(Identifier.tryParse(id))
                    ModuleXRay.blocks.add(block)
                    chat(regular("Added ${block.name}"))
                    return@handler
                }

                if ("remove".equals(option, true)) {
                    val block = Registry.BLOCK.get(Identifier.tryParse(id))
                    ModuleXRay.blocks.remove(block)
                    chat(regular("Removed ${block.name}"))
                    return@handler
                }

                throw CommandException(command.result("valueNotFound", id))
            }
            .build()
    }
}
