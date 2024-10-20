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
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.moduleParameter
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.input.inputByName
import net.ccbluex.liquidbounce.utils.input.keyList
import net.ccbluex.liquidbounce.utils.input.mouseList
import net.minecraft.client.util.InputUtil
import net.minecraft.util.Formatting
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Binds Command
 *
 * Allows you to manage the bindings of modules to keys.
 * It provides subcommands to add, remove, list and clear bindings.
 */
object CommandBinds {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("binds")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(
                        moduleParameter()
                            .required()
                            .build()
                    ).parameter(
                        ParameterBuilder
                            .begin<String>("key")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .autocompletedWith { begin -> (keyList + mouseList).filter { it.startsWith(begin) } }
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val keyName = args[1] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException(command.result("moduleNotFound", name))

                        val bindKey = inputByName(keyName)
                        if (bindKey == InputUtil.UNKNOWN_KEY) {
                            throw CommandException(command.result("unknownKey"))
                        }

                        module.bind.boundKey = bindKey
                        chat(regular(command.result("moduleBound", variable(module.name),
                            variable(module.bind.keyName))))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(
                        moduleParameter { mod -> !mod.bind.isUnbound }
                            .required()
                            .build()
                    )
                    .handler { command, args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException(command.result("moduleNotFound", name))

                        if (module.bind.isUnbound) {
                            throw CommandException(command.result("moduleNotBound"))
                        }

                        module.bind.unbind()
                        chat(regular(command.result("bindRemoved", variable(module.name))))
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .parameter(
                        pageParameter()
                            .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { command, args ->
                        val page = if (args.size > 1) {
                            args[0] as Int
                        } else {
                            1
                        }.coerceAtLeast(1)

                        val bindings = ModuleManager.sortedBy { it.name }
                            .filter { !it.bind.isUnbound }

                        if (bindings.isEmpty()) {
                            throw CommandException(command.result("noBindings"))
                        }

                        // Max page
                        val maxPage = ceil(bindings.size / 8.0).roundToInt()
                        if (page > maxPage) {
                            throw CommandException(command.result("pageNumberTooLarge", maxPage))
                        }

                        // Print out bindings
                        chat(command.result("bindings").styled { it.withColor(Formatting.RED).withBold(true) })
                        chat(regular(command.result("page", variable("$page / $maxPage"))))

                        val iterPage = 8 * page
                        for (module in bindings.subList(iterPage - 8, iterPage.coerceAtMost(bindings.size))) {
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
                                    .styled { it.withColor(Formatting.GRAY) }
                            )
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        ModuleManager.forEach { it.bind.unbind() }
                        chat(command.result("bindsCleared"))
                    }
                    .build()
            )
            .build()
    }

}
