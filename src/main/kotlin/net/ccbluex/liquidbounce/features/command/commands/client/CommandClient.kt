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
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

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
