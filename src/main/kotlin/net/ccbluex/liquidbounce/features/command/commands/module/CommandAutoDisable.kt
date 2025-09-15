/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.command.commands.module

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.modules
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoDisable
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Formatting

/**
 * AutoDisable Command
 *
 * Allows you to manage the list of modules that are automatically disabled.
 * It provides subcommands to add, remove, list and clear modules from the auto-disable list.
 *
 * Module: [ModuleAutoDisable]
 */
object CommandAutoDisable : Command.Factory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("autodisable")
            .hub()
            .subcommand(addSubcommand())
            .subcommand(removeSubcommand())
            .subcommand(listSubcommand())
            .subcommand(clearSubcommand())
            .build()
    }

    private fun clearSubcommand() = CommandBuilder
        .begin("clear")
        .handler {
            ModuleAutoDisable.clear()
            chat(
                command.result("modulesCleared"),
                metadata = MessageMetadata(id = "CAutoDisable#global")
            )
        }
        .build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                result("modules").withColor(Formatting.RED).bold(true)
            },
            items = {
                ModuleAutoDisable.modules
            },
            eachRow = { _, module ->
                "\u2B25 ".asText()
                    .formatted(Formatting.BLUE)
                    .append(variable(module.name).copyable())
                    .append(regular(" ("))
                    .append(variable(module.bind.keyName).copyable())
                    .append(regular(")"))
            }
        )

    private fun removeSubcommand() = CommandBuilder
        .begin("remove")
        .parameter(
            ParameterBuilder.modules(all = ModuleAutoDisable.modules)
                .required()
                .build()
        )
        .handler {
            val modules = args[0] as Set<ClientModule>

            modules.forEach { module ->
                if (!ModuleAutoDisable.remove(module)) {
                    throw CommandException(command.result("moduleNotPresent", module.name))
                }

                chat(
                    regular(
                        command.result(
                            "moduleRemoved",
                            variable(module.name)
                        )
                    ),
                    command
                )
            }
        }
        .build()

    private fun addSubcommand() = CommandBuilder
        .begin("add")
        .parameter(
            ParameterBuilder.modules()
                .required()
                .build()
        )
        .handler {
            val modules = args[0] as Set<ClientModule>

            modules.forEach { module ->
                if (!ModuleAutoDisable.add(module)) {
                    throw CommandException(command.result("moduleIsPresent", module.name))
                }

                chat(regular(command.result("moduleAdded", variable(module.name))), command)
            }
        }
        .build()

}
