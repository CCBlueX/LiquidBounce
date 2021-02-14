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
import net.ccbluex.liquidbounce.utils.*
import net.ccbluex.liquidbounce.utils.extensions.asText
import org.lwjgl.glfw.GLFW
import kotlin.math.ceil
import kotlin.math.roundToInt

object CommandBinds {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("binds")
            .description("Allows you to manage your binds")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .description("Adds a new bind")
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
                        val keyName = args[1] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException("Module $name not found.")

                        val bindKey = key(keyName)
                        if (bindKey == GLFW.GLFW_KEY_UNKNOWN) {
                            throw CommandException("Unknown key.")
                        }

                        module.bind = bindKey
                        chat(regular("Bound module "), variable(module.name), regular(" to key "), variable(keyName(bindKey)), dot())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .description("Removes a name to the friend list")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the module")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { args ->
                        val name = args[0] as String
                        val module = ModuleManager.find { it.name.equals(name, true) }
                            ?: throw CommandException("Module $name not found.")

                        if (module.bind == GLFW.GLFW_KEY_UNKNOWN) {
                            throw CommandException("Module is not bound to any key.")
                        }

                        module.bind = GLFW.GLFW_KEY_UNKNOWN
                        chat(regular("The bind for module "), variable(module.name), regular(" has been removed"), dot())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .description("Lists all bindings")
                    .parameter(
                        ParameterBuilder
                            .begin<Int>("page")
                            .description("The page to show")
                            .verifiedBy(ParameterBuilder.INTEGER_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { args ->
                        val page = if (args.size > 1) {
                            args[0] as Int
                        }else {
                            1
                        }.coerceAtLeast(1)

                        val bindings = ModuleManager.sortedBy { it.name }
                            .filter { it.bind != GLFW.GLFW_KEY_UNKNOWN }

                        if (bindings.isEmpty()) {
                            throw CommandException("There are no bindings to be shown.")
                        }

                        // Max page
                        val maxPage = ceil(bindings.size / 8.0).roundToInt()
                        if (page > maxPage) {
                            throw CommandException("The number you have entered is too big, it must be under $maxPage.")
                        }

                        // Print out bindings
                        val bindingsOut = StringBuilder()
                        bindingsOut.append("§c§lBindings\n")
                        bindingsOut.append("§7> Page: §8$page / $maxPage\n")

                        val iterPage = 8 * page
                        for (module in bindings.subList(iterPage - 8, iterPage.coerceAtMost(bindings.size))) {
                            bindingsOut.append("§6> §7${module.name} (§8§l${keyName(module.bind).asText()}§7)\n")
                        }
                        chat(bindingsOut.toString())
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .description("Clears your binds")
                    .handler {
                        ModuleManager.forEach { it.bind = GLFW.GLFW_KEY_UNKNOWN }
                        chat("Cleared all binds.")
                    }
                    .build()
            )
            .build()
    }

}
