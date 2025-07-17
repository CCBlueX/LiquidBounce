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

import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.integration.IntegrationListener
import net.ccbluex.liquidbounce.integration.IntegrationListener.browser
import net.ccbluex.liquidbounce.integration.VirtualScreenType
import net.ccbluex.liquidbounce.integration.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent

object CommandClientIntegrationSubcommand {
    fun integrationCommand() = CommandBuilder.begin("integration")
        .hub()
        .subcommand(menuSubcommand())
        .subcommand(overrideSubcommand())
        .subcommand(resetSubcommand())
        .build()

    private fun resetSubcommand() = CommandBuilder.begin("reset")
        .handler { _, _ ->
            chat(regular("Resetting client JCEF browser..."))
            IntegrationListener.update()
        }.build()

    private fun overrideSubcommand() = CommandBuilder.begin("override")
        .parameter(
            ParameterBuilder.begin<String>("name")
                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                .build()
        ).handler { _, args ->
            chat(regular("Overrides client JCEF browser..."))
            browser.url = args[0] as String
        }.build()

    private fun menuSubcommand() = CommandBuilder.begin("menu")
        .alias("url")
        .handler { _, _ ->
            chat(variable("Client Integration"))
            val baseUrl = ThemeManager.route().url

            chat(
                regular("Base URL: ")
                    .append(
                        variable(baseUrl)
                            .underline(true)
                            .onClick(ClickEvent(ClickEvent.Action.OPEN_URL, baseUrl))
                            .onHover(
                                HoverEvent(
                                    HoverEvent.Action.SHOW_TEXT,
                                    regular("Click to open the integration URL in your browser.")
                                )
                            )
                    ),
                metadata = MessageMetadata(
                    prefix = false
                )
            )

            chat(metadata = MessageMetadata(prefix = false))
            chat(regular("Integration Menu:"))
            for (screenType in VirtualScreenType.entries) {
                val url = runCatching {
                    ThemeManager.route(screenType, true)
                }.getOrNull()?.url ?: continue
                val upperFirstName = screenType.routeName.replaceFirstChar { it.uppercase() }

                chat(
                    regular("-> $upperFirstName (")
                        .append(
                            variable("Browser")
                                .underline(true)
                                .onClick(ClickEvent(ClickEvent.Action.OPEN_URL, url))
                                .onHover(
                                    HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        regular("Click to open the URL in your browser.")
                                    )
                                )
                        )
                        .append(regular(", "))
                        .append(
                            variable("Clipboard")
                                .copyable(
                                    copyContent = url, hover = HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        regular("Click to copy the URL to your clipboard.")
                                    )
                                )
                                .underline(true)
                        )
                        .append(regular(")")),
                    metadata = MessageMetadata(
                        prefix = false
                    )
                )
            }

            chat(variable("Hint: You can also access the integration from another device.").italic(true))
        }.build()
}
