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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleManager
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.util.Formatting

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
        .pagedQuery(
            pageSize = 8,
            header = {
                result("hidden").withColor(Formatting.RED).bold(true)
            },
            items = {
                ModuleManager.filter { it.hidden }
            },
            eachRow = { _, module ->
                "\u2B25 ".asText()
                    .formatted(Formatting.BLUE)
                    .append(variable(module.name).copyable())
                    .append(regular(" ("))
                    .append(regular(result("hidden"))) // TODO: click to unhide?
                    .append(regular(")"))
            }
        )

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
