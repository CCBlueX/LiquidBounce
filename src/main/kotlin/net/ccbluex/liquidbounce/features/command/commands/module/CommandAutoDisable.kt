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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.moduleParameter
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.features.module.modules.world.ModuleAutoDisable
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.util.Formatting
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * AutoDisable Command
 *
 * Allows you to manage the list of modules that are automatically disabled.
 * It provides subcommands to add, remove, list and clear modules from the auto-disable list.
 *
 * Module: [ModuleAutoDisable]
 */
object CommandAutoDisable : CommandFactory {

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
        .handler { command, _ ->
            ModuleAutoDisable.listOfModules.clear()
            chat(
                command.result("modulesCleared"),
                metadata = MessageMetadata(id = "CAutoDisable#global")
            )
        }
        .build()

    private fun listSubcommand() = CommandBuilder
        .begin("list")
        .parameter(
            pageParameter()
                .optional()
                .build()
        )
        .handler { command, args ->
            val page = (args.firstOrNull() as? Int ?: 1).coerceAtLeast(1)

            val modules = ModuleAutoDisable.listOfModules.sortedBy { it.name }

            if (modules.isEmpty()) {
                throw CommandException(command.result("noModules"))
            }

            // Max page
            val maxPage = ceil(modules.size / 8.0).roundToInt()
            if (page > maxPage) {
                throw CommandException(command.result("pageNumberTooLarge", maxPage))
            }

            mc.inGameHud.chatHud.removeMessage("CAutoDisable#global")
            val data = MessageMetadata(id = "CAutoDisable#global", remove = false)

            // Print out bindings
            chat(
                command.result("modules").styled { it.withColor(Formatting.RED).withBold(true) },
                metadata = data
            )
            chat(
                regular(command.result("page", variable("$page / $maxPage"))),
                metadata = data
            )

            val iterPage = 8 * page
            for (module in modules.subList(iterPage - 8, iterPage.coerceAtMost(modules.size))) {
                chat(
                    "> ".asText()
                        .styled { it.withColor(Formatting.GOLD) }
                        .append(module.name + " (")
                        .styled { it.withColor(Formatting.GRAY) }
                        .append(
                            module.bind.keyName.asText()
                                .styled { it.withColor(Formatting.DARK_GRAY).withBold(true) }
                        )
                        .append(")")
                        .styled { it.withColor(Formatting.GRAY) },
                    metadata = data
                )
            }
        }
        .build()

    private fun removeSubcommand() = CommandBuilder
        .begin("remove")
        .parameter(
            ParameterBuilder
                .begin<String>("module")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                .autocompletedWith { begin, _ ->
                    ModuleAutoDisable.listOfModules.mapNotNull { it.name.takeIf { n -> n.startsWith(begin) } }
                }
                .required()
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String
            val module = ModuleManager.find { it.name.equals(name, true) }
                ?: throw CommandException(command.result("moduleNotFound", name))

            if (!ModuleAutoDisable.listOfModules.remove(module)) {
                throw CommandException(command.result("moduleNotPresent", name))
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
        .build()

    private fun addSubcommand() = CommandBuilder
        .begin("add")
        .parameter(
            moduleParameter()
                .required()
                .build()
        )
        .handler { command, args ->
            val name = args[0] as String
            val module = ModuleManager.find { it.name.equals(name, true) }
                ?: throw CommandException(command.result("moduleNotFound", name))

            if (!ModuleAutoDisable.listOfModules.add(module)) {
                throw CommandException(command.result("moduleIsPresent", name))
            }

            chat(regular(command.result("moduleAdded", variable(module.name))), command)
        }
        .build()

}
