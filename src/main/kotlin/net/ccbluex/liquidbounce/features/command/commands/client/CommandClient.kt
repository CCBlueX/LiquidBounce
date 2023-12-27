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
import net.ccbluex.liquidbounce.utils.client.*
import net.ccbluex.liquidbounce.web.integration.BrowserScreen
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler
import net.ccbluex.liquidbounce.web.integration.IntegrationHandler.clientJcef
import net.ccbluex.liquidbounce.web.theme.ThemeManager
import net.minecraft.util.Util

/**
 * Client Command
 *
 * Provides subcommands for client management.
 */
object CommandClient {

    /**
     * Creates client command with a variety of subcommands.
     *
     * TODO: contributors
     *  links
     *  instructions
     *  reset
     *  theme manager
     */
    fun createCommand() = CommandBuilder.begin("client")
        .hub()
        .subcommand(infoCommand())
        .subcommand(browserCommand())
        .subcommand(integrationCommand())
        .build()

    private fun infoCommand() = CommandBuilder
        .begin("info")
        .handler { command, _ ->
            chat(regular(command.result("clientName", variable(LiquidBounce.CLIENT_NAME))),
                prefix = false)
            chat(regular(command.result("clientVersion", variable(LiquidBounce.clientVersion))),
                prefix = false)
            chat(regular(command.result("clientAuthor", variable(LiquidBounce.CLIENT_AUTHOR))),
                prefix = false)
        }.build()

    private fun browserCommand() = CommandBuilder.begin("browser")
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
        )
        .build()

    private fun integrationCommand() = CommandBuilder.begin("integration")
        .hub()
        .subcommand(CommandBuilder.begin("url")
            .handler { command, args ->
                chat(regular("Opening integration URL on your default browser..."))
                browseUrl(ThemeManager.integrationUrl)
                chat(regular("Integration URL: ${ThemeManager.integrationUrl}"))
            }.build()
        )
        .subcommand(CommandBuilder.begin("override")
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
                IntegrationHandler.updateIntegrationBrowser()
            }.build()
        )
        .build()

}
