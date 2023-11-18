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

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.INTEGRATION_URL
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.clientJcef

object CommandClient {

    fun createCommand(): Command {
        return CommandBuilder.begin("client")
            .hub()
            .subcommand(
                CommandBuilder.begin("info").handler { command, _ ->
                    chat(regular(command.result("clientName", variable(LiquidBounce.CLIENT_NAME))),
                        prefix = false)
                    chat(regular(command.result("clientVersion", variable(LiquidBounce.clientVersion))),
                        prefix = false)
                    chat(regular(command.result("clientAuthor", variable(LiquidBounce.CLIENT_AUTHOR))),
                        prefix = false)
                }.build()
            )
            .subcommand(
                CommandBuilder.begin("browser")
                    .hub()
                    .subcommand(
                        CommandBuilder.begin("open")
                            .parameter(
                                ParameterBuilder.begin<String>("name")
                                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                                    .build()
                            ).handler { command, args ->
                                chat(regular("Opening browser..."))
                                mc.setScreen(BrowserScreen(args[0] as String))
                            }.build()
                    ).subcommand(CommandBuilder.begin("override")
                        .parameter(
                            ParameterBuilder.begin<String>("name")
                                .verifiedBy(ParameterBuilder.STRING_VALIDATOR).required()
                                .build()
                        ).handler { command, args ->
                            chat(regular("Overrides client JCEF browser..."))
                            clientJcef?.loadUrl(args[0] as String)
                        }.build()
                    ).subcommand(CommandBuilder.begin("reset")
                        .handler { command, args ->
                            chat(regular("Resetting client JCEF browser..."))
                            clientJcef?.loadUrl(INTEGRATION_URL)
                        }.build()
                    )

                    .build()
        )
            // TODO: contributors
            // TODO: links
            // TODO: instructions
            // TODO: reset
            // TODO: script manager
            // TODO: theme manager
            // .. other client base commands
            .build()
    }

}
