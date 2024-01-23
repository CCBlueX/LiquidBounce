package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Formatting

object CommandContainers {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("containers")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("tag")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val tag = args[0] as String
                        val nbtCompound = StringNbtReader.parse(tag)

                        if (!nbtCompound.contains("BlockEntityTag")) {
                            throw CommandException(command.result("noBlockEntityTag"))
                        }

                        ClientItemGroups.storeAsContainerItem(nbtCompound)
                        chat(regular(command.result("added")))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(
                        ParameterBuilder.begin<Int>("index")
                            .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val index = args[0] as Int

                        if (index >= ClientItemGroups.containers.size) {
                            throw CommandException(command.result("indexOutOfBounds"))
                        }

                        ClientItemGroups.removeContainer(index)
                        chat(command.result("removed"))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .handler { command, args ->
                        val itemStacks = ClientItemGroups.containersAsItemStacks()

                        if (itemStacks.isEmpty()) {
                            throw CommandException(command.result("noContainers"))
                        }

                        itemStacks.forEachIndexed { index, itemStack ->
                            chat(regular("-> ").append(variable(index.toString()).styled {
                                it.withColor(Formatting.GOLD)
                            }).append(regular(": ")).append(variable(itemStack.name.string)))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        ClientItemGroups.clearContainers()
                        chat(command.result("cleared"))
                    }
                    .build()
            )
            .build()
    }

}
