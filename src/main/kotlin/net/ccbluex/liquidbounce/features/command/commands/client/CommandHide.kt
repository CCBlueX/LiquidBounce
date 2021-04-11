/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.dot
import net.ccbluex.liquidbounce.utils.regular
import net.ccbluex.liquidbounce.utils.variable
import kotlin.math.ceil
import kotlin.math.roundToInt

object CommandHide {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("hide")
            .description("Allows you to hide modules")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("hide")
                    .description("Hides a module")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the module")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException("Module ${args[1]} not found.")

                        module.hidden = true
                        chat(regular("Module "), variable(module.name), regular(" is now hidden"), dot())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("unhide")
                    .description("Unhides a module")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the module")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException("Module ${args[1]} not found.")

                        module.hidden = false
                        chat(regular("Module "), variable(module.name), regular(" is not hidden anymore"), dot())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .description("Lists the hidden modules")
                    .parameter(
                        ParameterBuilder
                            .begin<Int>("page")
                            .description("The page to show")
                            .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val page = if (args.size > 1) {
                            args[0] as Int
                        }else {
                            1
                        }.coerceAtLeast(1)

                        val hiddens = ModuleManager.sortedBy { it.name }
                            .filter { it.hidden }

                        if (hiddens.isEmpty()) {
                            throw CommandException("There are no hidden modules to be shown.")
                        }

                        // Max page
                        val maxPage = ceil(hiddens.size / 8.0).roundToInt()
                        if (page > maxPage) {
                            throw CommandException("The number you have entered is too big, it must be under $maxPage.")
                        }

                        // Print out bindings
                        val bindingsOut = StringBuilder()
                        bindingsOut.append("§c§lHidden\n")
                        bindingsOut.append("§7> Page: §8$page / $maxPage\n")

                        val iterPage = 8 * page
                        for (module in hiddens.subList(iterPage - 8, iterPage.coerceAtMost(hiddens.size))) {
                            bindingsOut.append("§6> §7${module.name} (§8§lHidden§7)\n")
                        }
                        chat(bindingsOut.toString())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .description("Makes every module visible")
                    .handler { command, args ->
                        ModuleManager.forEach { it.hidden = false }
                        chat("Successfully unhidden all modules.")
                    }
                    .build()
            )
            .build()
    }

}
