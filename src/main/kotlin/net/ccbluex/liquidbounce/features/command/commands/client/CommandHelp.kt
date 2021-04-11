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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.chat
import kotlin.math.ceil
import kotlin.math.roundToInt

object CommandHelp {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("help")
            .description("Shows a list of commands")
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

                val commands = CommandManager.sortedBy { it.name }

                // Max page
                val maxPage = ceil(commands.size / 8.0).roundToInt()
                if (page > maxPage) {
                    throw CommandException("The page number you have entered exceeds the maximum number of available help pages, it may at most be $maxPage.")
                }

                // Print out help page
                val helpOut = StringBuilder()
                helpOut.append("§c§lHelp\n")
                helpOut.append("§7> Page: §8$page / $maxPage\n")

                val iterPage = 8 * page
                for (command in commands.subList(iterPage - 8, iterPage.coerceAtMost(commands.size))){
                    val aliases = if (command.aliases.isEmpty()) "" else " §7(§8${command.aliases.joinToString("§7, §8")}§7)"
                    helpOut.append("§6> §7${CommandManager.Options.prefix}${command.name}$aliases\n")
                }

                helpOut.append("§a------------\n§7> §c${CommandManager.Options.prefix}help §8<§7§lpage§8>")
                chat(helpOut.toString())
            }
            .build()
    }
}
