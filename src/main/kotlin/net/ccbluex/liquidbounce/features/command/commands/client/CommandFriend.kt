/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable

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
                            .useMinecraftAutoCompletion()
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
                    .handler { command, args ->
                        val friend = FriendManager.Friend(args[0] as String, args.getOrNull(1) as String?)

                        if (FriendManager.friends.add(friend)) {
                            if (friend.alias == null) {
                                chat(regular(command.result("success", variable(friend.name))))
                            } else {
                                chat(regular(command.result("successAlias", variable(friend.name), variable(friend.alias!!))))
                            }
                        } else {
                            throw CommandException(command.result("alreadyFriends", variable(friend.name)))
                        }

                    }
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
                    .handler { command, args ->
                        val friend = FriendManager.Friend(args[0] as String, null)

                        if (FriendManager.friends.remove(friend)) {
                            chat(regular(command.result("success", variable(friend.name))))
                        } else {
                            throw CommandException(command.result("notFriends", variable(friend.name)))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("alias")
                    .parameter(
                        ParameterBuilder
                            .begin<String>("name")
                            .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                            .autocompletedWith { begin -> FriendManager.friends.filter { it.name.startsWith(begin, true) }.map { it.name } }
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
                    .handler { command, args ->
                        val name = args[0] as String
                        val friend = FriendManager.friends.firstOrNull { it.name == name }

                        if (friend != null) {
                            friend.alias = args[1] as String

                            chat(regular(command.result("success", variable(name), variable(args[1] as String))))
                        } else {
                            throw CommandException(command.result("notFriends", variable(name)))
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("list")
                    .handler { command, _ ->
                        if (FriendManager.friends.isEmpty()) {
                            chat(command.result("noFriends"))
                        } else {
                            FriendManager.friends.forEach {
                                if (it.alias != null) {
                                    chat(regular("- "), variable(it.name), regular(" ("), variable(it.alias!!), regular(")"))
                                } else {
                                    chat(regular("- "), variable(it.name))
                                }
                            }
                        }
                    }
                    .build()
            )
            .subcommand(
                CommandBuilder
                    .begin("clear")
                    .handler { command, _ ->
                        if (FriendManager.friends.isEmpty()) {
                            throw CommandException(command.result("noFriends"))
                        } else {
                            FriendManager.friends.clear()

                            chat(regular(command.result("success")))
                        }
                    }
                    .build()
            )
            .build()
    }
}
