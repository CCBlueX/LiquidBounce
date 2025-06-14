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
package net.ccbluex.liquidbounce.features.command.commands.client

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.command.builder.pageParameter
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * Hide Command
 *
 * Allows you to hide specific modules.
 */
object CommandHide : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("hide")
            .hub()
            .subcommand(hideSubcommand())
            .subcommand(unhideSubommand())
            .subcommand(listSubcommand())
            .subcommand(clearSubcommand())
            .build()
    }

    private fun clearSubcommand() = CommandBuilder
        .begin("clear")
        .handler { command, _ ->
            ModuleManager.forEach { it.hidden = false }
            chat(
                regular(command.result("modulesUnhidden")),
                metadata = MessageMetadata(id = "CHide#info")
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
            chat(
                bindingsOut.toString().asText(),
                metadata = MessageMetadata(id = "CHide#info")
            )
        }
        .build()

    private fun unhideSubommand() = CommandBuilder
        .begin("unhide")
        .parameter(
            Parameters.modules { it.hidden }
                .required()
                .build()
        )
        .handler { command, args ->
            val modules = args[0] as Set<ClientModule>
            modules.forEach { it.hidden = false }

            chat(
                command.result(
                    "moduleUnhidden",
                    modules.map { variable(it.name) }.joinToText(", ".asText())
                ),
                metadata = MessageMetadata(id = "CHide#info")
            )
        }
        .build()

    private fun hideSubcommand() = CommandBuilder
        .begin("hide")
        .parameter(
            Parameters.modules { !it.hidden }
                .required()
                .build()
        )
        .handler { command, args ->
            val modules = args[0] as Set<ClientModule>
            modules.forEach { it.hidden = true }

            chat(
                command.result(
                    "moduleHidden",
                    modules.map { variable(it.name) }.joinToText(", ".asText())
                ),
                metadata = MessageMetadata(id = "CHide#info")
            )
        }
        .build()

}
