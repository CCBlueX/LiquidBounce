/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.preset.pagedQuery
import net.ccbluex.liquidbounce.integration.theme.Theme
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.asText
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.clickablePath
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.markAsError
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.withColor
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent
import net.minecraft.util.Util
import java.net.URI

object CommandClientThemeSubcommand {
    fun themeCommand() = CommandBuilder.begin("theme")
        .hub()
        .subcommand(listSubcommand())
        .subcommand(setSubcommand())
        .subcommand(browseSubcommand())
        .subcommand(reloadSubcommand())
        .build()

    private fun browseSubcommand() = CommandBuilder.begin("browse").handler {
        Util.getPlatform().openFile(ThemeManager.themesFolder)
        chat(regular("Location: "), clickablePath(ThemeManager.themesFolder))
    }.build()

    private fun setSubcommand() = CommandBuilder.begin("set")
        .parameter(
            ParameterBuilder.begin<String>("theme")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .autocompletedFrom { ThemeManager.themeIds }
                .build()
        )
        .suspendHandler {
            val idOrUrl = args[0] as String
            val theme = try {
                require(idOrUrl.contains("://")) { "Not a URL" }

                val url = URI.create(idOrUrl).toURL()

                // Disallow non-http(s) URLs
                if (!url.protocol.equals("http", true) &&
                    !url.protocol.equals("https", true)) {
                    throw CommandException(("Invalid URL protocol \"${url.protocol}\", " +
                        "only http(s) is allowed.").asText())
                }

                // Disallow non-localhost URLs
                if (!url.host.equals("localhost", true) &&
                    !url.host.equals("127.0.0.1", true)) {
                    throw CommandException("For security reasons, only localhost URLs are allowed.".asText())
                }

                // Loads the theme from the URL (will throw an exception if the theme is invalid)
                Theme.load(url.toString())
            } catch (_: IllegalArgumentException) {
                ThemeManager.themes.find { it.metadata.id.equals(idOrUrl, true) }
                    ?: throw CommandException("No theme found with name \"$idOrUrl\"!".asText())
            }

            runCatching {
                ThemeManager.theme = theme
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
                "Available themes".asText().withColor(ChatFormatting.RED).bold(true)
            },
            items = {
                ThemeManager.themes
            },
            eachRow = { _, theme ->
                regular("\u2B25 ".asText()
                    .withStyle(ChatFormatting.BLUE)
                    .append(variable(theme.metadata.name))
                    .append(regular(" ("))
                    .append(variable(theme.metadata.id))
                    .append(regular(" "))
                    .append(variable(theme.metadata.version))
                    .append(regular(")"))
                    .append(regular(" by "))
                    .append(variable(theme.metadata.authors.joinToString(separator = ", ")).copyable())
                    .append(regular(" from "))
                    .append(variable(theme.origin.tag))
                ).onClick(
                    ClickEvent.SuggestCommand(
                        "${CommandManager.GlobalSettings.prefix}client theme set ${theme.metadata.id}"
                    )
                ).onHover(
                    HoverEvent.ShowText(
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
