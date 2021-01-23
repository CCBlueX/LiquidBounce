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
import net.ccbluex.liquidbounce.utils.chat
import net.ccbluex.liquidbounce.utils.regular
import net.ccbluex.liquidbounce.utils.variable

object CommandClient {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("client")
            .description("Allows you to manage your client")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("info")
                    .description("Shows information about the client")
                    .handler {
                        chat(regular("Client name: "), variable(LiquidBounce.CLIENT_NAME), prefix = false)
                        chat(regular("Client version: "), variable(LiquidBounce.CLIENT_VERSION), prefix = false)
                        chat(regular("Client author: "), variable(LiquidBounce.CLIENT_AUTHOR), prefix = false)
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("reload")
                    .description("Reload your client")
                    .handler {
                        // todo: reload client
                    }
                    .build()
            )

            // todo: contributers
            // todo: links
            // todo: instructions
            // todo: reset
            // todo: script manager
            // todo: theme manager
            // .. other client base commands
            .build()
    }

}
