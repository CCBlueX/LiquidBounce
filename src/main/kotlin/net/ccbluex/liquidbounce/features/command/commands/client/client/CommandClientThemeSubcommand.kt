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
package net.ccbluex.liquidbounce.features.command.commands.client.client

import net.ccbluex.liquidbounce.config.ConfigSystem
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandExecutor.suspendHandler
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.util.Formatting
import net.minecraft.util.Util

object CommandClientThemeSubcommand {
    fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(browseSubcommand())
        .subcommand(reloadSubcommand())
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler {
        Util.getOperatingSystem().open(ThemeManager.themesFolder)
        chat(regular("Location: "), clickablePath(ThemeManager.themesFolder))
    }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("theme")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .autocompletedFrom { ThemeManager.themeIds }
                .build()
        )
        .handler {
            val id = args[0] as String
            val theme = ThemeManager.themes.find { it.metadata.id.equals(id, true) } ?:
                throw CommandException("No theme found with name \"$id\"!".asText())

            runCatching {
                ThemeManager.currentTheme = theme.metadata.id
                ConfigSystem.store(ThemeManager)
            }.onFailure {
                chat(markAsError("Failed to switch theme: ${it.message}"))
            }.onSuccess {
                chat(regular("Switched theme to "), variable(theme.metadata.name).copyable(), regular("."))
            }
        }.build()

    private fun listSubcommand() = CommandBuilder.begin("list")
        .pagedQuery(
            pageSize = 8,
            header = {
                "Available themes".asText().withColor(Formatting.RED).bold(true)
            },
            items = {
                ThemeManager.themes
            },
            eachRow = { _, theme ->
                regular("\u2B25 ".asText()
                    .formatted(Formatting.BLUE)
                    .append(variable(theme.metadata.name))
                    .append(regular(" ("))
                    .append(variable(theme.metadata.id))
                    .append(regular(" "))
                    .append(variable(theme.metadata.version))
                    .append(regular(")"))
                    .append(regular(" by "))
                    .append(variable(theme.metadata.authors.joinToString(separator = ", ")).copyable())
                    .append(regular(" from "))
                    .append(variable(theme.origin.choiceName))
                ).onClick(
                    ClickEvent(
                        ClickEvent.Action.SUGGEST_COMMAND,
                        ".client theme set ${theme.metadata.id}"
                    )
                ).onHover(
                    HoverEvent(
                        HoverEvent.Action.SHOW_TEXT,
                        variable("Click to set theme \"${theme.metadata.name}\".")
                    )
                )
            }
        )

    private fun reloadSubcommand() = CommandBuilder.begin("reload")
        .suspendHandler {
            val prevCount = ThemeManager.themes.size

            ThemeManager.load()
            chat(regular("Reloaded themes. "))
            val diff = ThemeManager.themes.size - prevCount
            if (diff > 0) {
                chat(regular("Added "), variable(diff.toString()), regular(" new theme(s)."))
            } else if (diff < 0) {
                chat(regular("Removed "), variable((-diff).toString()), regular(" theme(s)."))
            } else {
                chat(regular("No new themes added."))
            }
        }.build()

}
