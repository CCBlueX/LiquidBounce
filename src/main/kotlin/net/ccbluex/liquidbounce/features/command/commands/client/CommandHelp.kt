/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.text.Text
import net.minecraft.util.Formatting
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Help Command
 *
 * Provides a help page for displaying other commands. 
 */
object CommandHelp {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
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

                val commands = CommandManager.sortedBy { it.name }

                // Max page
                val maxPage = ceil(commands.size / 8.0).roundToInt()
                if (page > maxPage) {
                    throw CommandException(command.result("pageNumberTooLarge", maxPage))
                }

                // Print out help page
                chat(command.result("help").styled { it.withColor(Formatting.RED).withBold(true) })
                chat(regular(command.result("pageCount", variable("$page / $maxPage"))))

                val iterPage = 8 * page
                for (cmd in commands.subList(iterPage - 8, iterPage.coerceAtMost(commands.size))) {
                    val aliases = Text.literal("")

                    if (cmd.aliases.isNotEmpty()) {
                        cmd.aliases.forEach { alias -> aliases.append(variable(", ")).append(regular(alias)) }
                    }

                    chat(
                        "- ".asText()
                            .styled { it.withColor(Formatting.BLUE) }
                            .append(CommandManager.Options.prefix + cmd.name)
                            .styled { it.withColor(Formatting.GRAY) }
                            .append(aliases)
                    )
                }

                chat(
                    "--- ".asText()
                        .styled { it.withColor(Formatting.DARK_GRAY) }
                        .append(variable("${CommandManager.Options.prefix}help <"))
                        .append(variable(command.result("page")))
                        .append(variable(">"))
                )
            }
            .build()
    }
}
