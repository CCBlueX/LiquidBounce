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

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder

object CommandFriend {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("friend")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<String>("alias")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { _, _ -> TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { _, _ -> TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("alias")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<String>("alias")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { _, _ -> TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .handler { command, _ -> println(command.result("no_friends")); return@handler }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ -> println(command.result("no_friends")); return@handler }
                    .build()
            )
            .build()
    }
}
