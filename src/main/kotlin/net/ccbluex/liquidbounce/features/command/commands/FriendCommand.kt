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

package net.ccbluex.liquidbounce.features.command.commands

import net.ccbluex.liquidbounce.features.command.Command
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder

object FriendCommand {

    fun createCommand(): Command {
        return CommandBuilder
            .begin("friend")
            .description("Allows you to manage your friend list")
            .hub()
            .subcommand(
                CommandBuilder
                    .begin("add")
                    .description("Adds a name to the friend list")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the friend")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<String>("alias")
                            .description("An optional alias of the friend")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .optional()
                            .build()
                    )
                    .handler { TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("remove")
                    .description("Removes a name to the friend list")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the friend")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("alias")
                    .description("Changes the alias of a friend list entry")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .description("The name of the friend")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .parameter(
                        ParameterBuilder
                            .begin<String>("alias")
                            .description("The new alias")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .required()
                            .build()
                    )
                    .handler { TODO() }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .description("Lists the friend list")
                    .handler { println("You have no friends"); return@handler true }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .description("Clears the friend list")
                    .handler { println("You have no friends"); return@handler true }
                    .build()
            )
            .build()
    }
}
