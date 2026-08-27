/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import com.mojang.brigadier.CommandDispatcher
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.FriendArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.onlinePlayers
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.misc.FriendManager
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.bold
import net.ccbluex.liquidbounce.utils.client.bypassNameProtection
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.copyable
import net.ccbluex.liquidbounce.utils.client.italic
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.removeMessage
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.HoverEvent

private const val MESSAGE_ID = "CFriend#info"

/**
 * Friend Command
 *
 * Provides subcommands related to managing friends, such as adding, removing, aliasing, listing, and clearing friends.
 */
object CommandFriend : CommandRegistrar {
    @Suppress("detekt:LongMethod", "detekt:ThrowsCount")
    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("friend") {
            literal("clear") {
                exec {
                    if (FriendManager.friends.isEmpty()) {
                        throw CommandException(t("clear.noFriends"))
                    } else {
                        FriendManager.friends.clear()

                        chat(
                            regular(t("clear.success")),
                            metadata = MessageMetadata(id = MESSAGE_ID)
                        )
                    }
                    1
                }
            }
            literal("list") {
                exec {
                    if (FriendManager.friends.isEmpty()) {
                        chat(
                            t("list.noFriends"),
                            metadata = MessageMetadata(id = MESSAGE_ID)
                        )
                    } else {
                        mc.gui.hud.chat.removeMessage(MESSAGE_ID)
                        val data = MessageMetadata(id = MESSAGE_ID, remove = false)

                        FriendManager.friends.forEachIndexed { index, friend ->
                            val alias = friend.alias ?: friend.getDefaultName(index)

                            val friendTextWithEvent = variable(friend.name)
                                .bypassNameProtection()
                                .copyable(copyContent = friend.name)
                                .italic(true)

                            val removeCommand = CommandManager.GlobalSettings.prefix + "friend remove ${friend.name}"
                            val removeText = regular("Remove ${friend.name}")

                            val removeButton = regular("[X]")
                                .withStyle(ChatFormatting.RED)
                                .bold(true)
                                .onHover(HoverEvent.ShowText(removeText))
                                .onClick(ClickEvent.SuggestCommand(removeCommand))

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
                    1
                }
            }
            literal("alias") {
                argument("name", FriendArgumentType) { name ->
                    argument("alias", ClientStringArgumentType.word()) { alias ->
                        exec { ctx ->
                            val friend = ctx.get(name)

                            friend.alias = ctx.get(alias)

                            chat(
                                regular(
                                    t("alias.success",
                                        variable(friend.name),
                                        variable(ctx.get(alias))
                                    )
                                ),
                                metadata = MessageMetadata(id = MESSAGE_ID)
                            )
                            1
                        }
                    }
                }
            }
            literal("remove") {
                argument("name", FriendArgumentType) { name ->
                    exec { ctx ->
                        val friend = ctx.get(name)

                        FriendManager.friends.remove(friend)
                        chat(
                            regular(
                                t("remove.success",
                                    variable(friend.name)
                                )
                            ),
                            metadata = MessageMetadata(id = MESSAGE_ID)
                        )
                        1
                    }
                }
            }
            literal("add") {
                argument("name", ClientStringArgumentType.word(), onlinePlayers()) { name ->
                    optional("alias", ClientStringArgumentType.word(), default = null) { alias ->
                        exec { ctx ->
                            addFriend(ctx.get(name), ctx.get(alias))
                            1
                        }
                    }
                }
            }
        }
    }

    private fun CmdI18n.addFriend(name: String, alias: String?) {
        val friend = FriendManager.Friend(name, alias)

        if (FriendManager.friends.add(friend)) {
            if (friend.alias == null) {
                chat(
                    regular(t("add.success", variable(friend.name))),
                    metadata = MessageMetadata(id = MESSAGE_ID)
                )
            } else {
                chat(
                    regular(
                        t("add.successAlias",
                            variable(friend.name),
                            variable(friend.alias!!)
                        )
                    ),
                    metadata = MessageMetadata(id = MESSAGE_ID)
                )
            }
        } else {
            throw CommandException(t("add.alreadyFriends", variable(friend.name)))
        }
    }

}
