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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.render.screen.EmptyScreen
import net.ccbluex.liquidbounce.render.ultralight.ScreenView
import net.ccbluex.liquidbounce.render.ultralight.UltralightEngine
import net.ccbluex.liquidbounce.render.ultralight.theme.ThemeManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import java.net.MalformedURLException
import java.net.URL

object CommandClient {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("client")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("info")
                    .handler { command, _ ->
                        chat(regular(command.result("clientName", variable(LiquidBounce.CLIENT_NAME))), prefix = false)
                        chat(regular(command.result("clientVersion", variable(LiquidBounce.CLIENT_VERSION))), prefix = false)
                        chat(regular(command.result("clientAuthor", variable(LiquidBounce.CLIENT_AUTHOR))), prefix = false)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("reload")
                    .handler { _, _ ->
                        // todo: reload client
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("ultralight")
                    .hub()
                    .subcommand(
                        CommandBuilder
                            .begin("show")
                            .parameter(
                                ParameterBuilder
                                    .begin<String>("name")
                                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                                    .required()
                                    .build()
                            )
                            .handler { command, args ->
                                val open: (ScreenView) -> Unit = try {
                                    val url = URL(args[0] as String)

                                    (
                                        {
                                            it.loadUrl(url.toString())
                                        }
                                        )
                                } catch (_: MalformedURLException) {
                                    val name = args[0] as String
                                    val page = ThemeManager.page(name)
                                        ?: throw CommandException(command.result("pageNotFound", name))

                                    (
                                        {
                                            it.loadPage(page)
                                        }
                                        )
                                }

                                val emptyScreen = EmptyScreen()
                                open(UltralightEngine.newScreenView(emptyScreen))
                                mc.setScreen(emptyScreen)
                            }
                            .build()
                    )
                    .build()
            )

            // todo: contributors
            // todo: links
            // todo: instructions
            // todo: reset
            // todo: script manager
            // todo: theme manager
            // .. other client base commands
            .build()
    }

}
