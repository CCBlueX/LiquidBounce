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
import net.ccbluex.liquidbounce.features.command.builder.moduleParameter
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.register.IncludeCommand
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Hide Command
 *
 * Allows you to hide specific modules.
 */
@IncludeCommand
object CommandHide : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("hide")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("hide")
                    .parameter(
                        moduleParameter { mod -> !mod.hidden }
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException(command.result("moduleNotFound"))

                        module.hidden = true
                        chat(command.result("moduleHidden"), variable(module.name))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("unhide")
                    .parameter(
                        moduleParameter { mod -> mod.hidden }
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException(command.result("moduleNotFound", name))

                        module.hidden = false
                        chat(regular(command.result("moduleUnhidden", variable(module.name))))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .parameter(
                        pageParameter()
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val page = if (args.size > 1) {
                            args[0] as Int
                        } else {
                            1
                        }.coerceAtLeast(1)

                        val hiddenModules = ModuleManager.sortedBy { it.name }
                            .filter { it.hidden }

                        if (hiddenModules.isEmpty()) {
                            throw CommandException(command.result("noHiddenModules"))
                        }

                        // Max page
                        val maxPage = ceil(hiddenModules.size / 8.0).roundToInt()
                        if (page > maxPage) {
                            throw CommandException(command.result("pageNumberTooLarge", maxPage))
                        }

                        // Print out bindings
                        val bindingsOut = StringBuilder()
                        bindingsOut.append("§c§l${command.result("hidden")}\n")
                        bindingsOut.append("§7> ${command.result("page")}: §8$page / $maxPage\n")

                        val iterPage = 8 * page
                        for (module in hiddenModules.subList(iterPage - 8, iterPage.coerceAtMost(hiddenModules.size))) {
                            bindingsOut.append("§6> §7${module.name} (§8§l${command.result("hidden")}§7)\n")
                        }
                        chat(bindingsOut.toString())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        ModuleManager.forEach { it.hidden = false }
                        chat(regular(command.result("modulesUnhidden")))
                    }
                    .build()
            )
            .build()
    }

}
