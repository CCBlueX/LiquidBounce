/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
import net.ccbluex.liquidbounce.features.command.CommandFactory
import net.ccbluex.liquidbounce.features.command.builder.CommandBuilder
import net.ccbluex.liquidbounce.features.command.builder.ParameterBuilder
import net.ccbluex.liquidbounce.features.command.builder.Parameters
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.utils.client.*
import net.minecraft.text.ClickEvent
import net.minecraft.text.HoverEvent
import net.minecraft.util.Formatting

private const val MSG_NO_FRIENDS = "noFriends"
private const val MSG_SUCCESS = "success"
private const val MESSAGE_ID = "CFriend#info"

/**
 * Friend Command
 *
 * Provides subcommands related to managing friends, such as adding, removing, aliasing, listing, and clearing friends.
 */
object CommandFriend : CommandFactory {

    override fun createCommand(): Command {
        return CommandBuilder
            .begin("friend")
            .hub()
            .subcommand(createAddSubcommand())
            .subcommand(createRemoveSubcommand())
            .subcommand(createAliasSubcommand())
            .subcommand(createListSubcommand())
            .subcommand(createClearSubcommand())
            .build()
    }

    private fun createClearSubcommand(): Command {
        return CommandBuilder
            .begin("clear")
            .handler { command, _ ->
                if (FriendManager.friends.isEmpty()) {
                    throw CommandException(command.result(MSG_NO_FRIENDS))
                } else {
                    FriendManager.friends.clear()

                    chat(
                        regular(command.result(MSG_SUCCESS)),
                        metadata = MessageMetadata(id = MESSAGE_ID)
                    )
                }
            }
            .build()
    }

    private fun createListSubcommand(): Command {
        return CommandBuilder
            .begin("list")
            .handler { command, _ ->
                if (FriendManager.friends.isEmpty()) {
                    chat(
                        command.result(MSG_NO_FRIENDS),
                        metadata = MessageMetadata(id = MESSAGE_ID)
                    )
                } else {
                    mc.inGameHud.chatHud.removeMessage(MESSAGE_ID)
                    val data = MessageMetadata(id = MESSAGE_ID, remove = false)

                    FriendManager.friends.forEachIndexed { index, friend ->
                        val alias = friend.alias ?: friend.getDefaultName(index)

                        val friendTextWithEvent = variable(friend.name)
                            .bypassNameProtection()
                            .copyable(copyContent = friend.name)
                            .italic(true)

                        val removeCommand = ".friend remove ${friend.name}"
                        val removeText = regular("Remove ${friend.name}")

                        val removeButton = regular("[X]")
                            .formatted(Formatting.RED)
                            .bold(true)
                            .onHover(HoverEvent(HoverEvent.Action.SHOW_TEXT, removeText))
                            .onClick(ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, removeCommand))

                        chat(
                            regular("- "),
                            friendTextWithEvent,
                            regular(" ("),
                            variable(alias),
                            regular(") "),
                            removeButton,
                            metadata = data
                        )
                    }
                }
            }
            .build()
    }

    private fun createAliasSubcommand(): Command {
        return CommandBuilder
            .begin("alias")
            .parameter(
                ParameterBuilder
                    .begin<String>("name")
                    .verifiedBy(ParameterBuilder.STRING_VALIDATOR)
                    .autocompletedWith { begin, _ ->
                        FriendManager.friends.filter {
                            it.name.startsWith(
                                begin,
                                true
                            )
                        }.map { it.name }
                    }
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

                    chat(
                        regular(command.result(MSG_SUCCESS, variable(name), variable(args[1] as String))),
                        metadata = MessageMetadata(id = MESSAGE_ID)
                    )
                } else {
                    throw CommandException(command.result("notFriends", variable(name)))
                }
            }
            .build()
    }

    private fun createRemoveSubcommand(): Command {
        return CommandBuilder
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
                    chat(
                        regular(command.result(MSG_SUCCESS, variable(friend.name))),
                        metadata = MessageMetadata(id = MESSAGE_ID)
                    )
                } else {
                    throw CommandException(command.result("notFriends", variable(friend.name)))
                }
            }
            .build()
    }

    private fun createAddSubcommand(): Command {
        return CommandBuilder
            .begin("add")
            .parameter(
                Parameters.playerName()
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
                        chat(
                            regular(command.result(MSG_SUCCESS, variable(friend.name))),
                            metadata = MessageMetadata(id = MESSAGE_ID)
                        )
                    } else {
                        chat(
                            regular(
                                command.result(
                                    "successAlias",
                                    variable(friend.name),
                                    variable(friend.alias!!)
                                )
                            ),
                            metadata = MessageMetadata(id = MESSAGE_ID)
                        )
                    }
                } else {
                    throw CommandException(command.result("alreadyFriends", variable(friend.name)))
                }

            }
            .build()
    }
}
