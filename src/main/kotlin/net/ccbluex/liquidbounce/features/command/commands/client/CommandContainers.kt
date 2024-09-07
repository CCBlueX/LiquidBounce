/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.itemgroup.ClientItemGroups
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.nbt.StringNbtReader
import net.minecraft.util.Formatting

@IncludeCommand
object CommandContainers : CommandFactory {

    override fun createCommand(): Command {
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
