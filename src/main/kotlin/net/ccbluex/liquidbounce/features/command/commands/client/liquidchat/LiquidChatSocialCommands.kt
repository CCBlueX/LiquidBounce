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
package net.ccbluex.liquidbounce.features.command.commands.client.liquidchat

import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.liquidbounce.features.chat.ChatNotices
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.ServerJoin
import net.ccbluex.liquidbounce.features.chat.packet.C2SBlockPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SFriendPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SGroupPacket
import net.ccbluex.liquidbounce.features.chat.packet.ChatFriend
import net.ccbluex.liquidbounce.features.chat.packet.ChatGroup
import net.ccbluex.liquidbounce.features.chat.packet.ChatUserRef
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * One command for every step of a friendship: request, accept, withdraw or end it, whichever applies.
 */
internal fun CmdLiteralScope.friendCommand() = literal("friend") {
    argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
        exec { ctx ->
            val reference = ctx.get(user)
            val friend = ChatSession.findFriend(reference)
            val incoming = ChatSession.incomingRequests.firstOrNull { it.name.equals(reference, true) }
            val outgoing = ChatSession.outgoingRequests.firstOrNull { it.name.equals(reference, true) }
            val packet = when {
                friend != null -> C2SFriendPacket("remove", friend.user.id)
                incoming != null -> C2SFriendPacket("accept", incoming.id)
                outgoing != null -> C2SFriendPacket("remove", outgoing.id)
                else -> C2SFriendPacket("request", reference)
            }
            sendChatPacket(packet)
            1
        }
    }
}

internal fun CmdLiteralScope.friendsCommand() = literal("friends") {
    exec {
        printLine(regular(t("friends.header", variable(ChatSession.friends.size.toString()))))
        ChatSession.friends
            .sortedWith(compareBy({ !it.online }, { it.user.name.lowercase() }))
            .forEach { printFriend(it, t("friends.join")) }
        ChatSession.incomingRequests.forEach { printRequest(it, t("friends.incoming", variable(it.name))) }
        1
    }
}

private fun printFriend(friend: ChatFriend, joinLabel: MutableComponent) {
    val server = friend.server
    val join = if (server != null) {
        ChatNotices.button(joinLabel, ChatFormatting.GREEN) { ServerJoin.confirm(server, friend.user.name) }
    } else {
        Component.empty()
    }

    printLine(
        Component.literal(if (friend.online) "● " else "○ ")
            .withStyle(if (friend.online) ChatFormatting.GREEN else ChatFormatting.DARK_GRAY),
        variable(friend.user.name),
        regular(server?.let { " ($it)" } ?: ""),
        join,
    )
}

private fun printRequest(request: ChatUserRef, text: MutableComponent) = printLine(
    regular(text),
    ChatNotices.button(translation("liquidbounce.liquidchat.accept"), ChatFormatting.GREEN) {
        GlobalSettingsClientChat.chatClient.sendPacket(C2SFriendPacket("accept", request.id))
    },
)

/**
 * Blocks or unblocks a user; without one, lists who is blocked.
 */
internal fun CmdLiteralScope.blockCommand() = literal("block") {
    exec {
        printLine(regular(t("block.header", variable(ChatSession.blocks.size.toString()))))
        ChatSession.blocks.forEach { printLine(regular("- "), variable(it.name)) }
        1
    }
    argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
        exec { ctx ->
            val reference = ctx.get(user)
            val blocked = ChatSession.blocks.firstOrNull { it.id == reference || it.name.equals(reference, true) }
            sendChatPacket(C2SBlockPacket(blocked?.id ?: reference, blocked == null))
            1
        }
    }
}

internal fun CmdLiteralScope.serverCommand() = literal("server") {
    argument("message", StringArgumentType.greedyString()) { message ->
        exec { ctx ->
            GlobalSettingsClientChat.send(ChatSession.SERVER, ctx.get(message))
            1
        }
    }
}

private fun group(reference: String): ChatGroup = ChatSession.findGroup(reference)
    ?: throw CommandException(translation("liquidbounce.command.liquidchat.group.unknown", reference))

@Suppress("LongMethod")
internal fun CmdLiteralScope.groupCommands() = literal("group") {
    literal("create") {
        argument("name", StringArgumentType.greedyString()) { name ->
            exec { ctx ->
                sendChatPacket(C2SGroupPacket("create", name = ctx.get(name)))
                1
            }
        }
    }
    for (action in listOf("accept", "decline", "leave", "delete")) {
        literal(action) {
            argument("group", ClientStringArgumentType.string(), chatGroups) { group ->
                exec { ctx ->
                    sendChatPacket(C2SGroupPacket(action, group = group(ctx.get(group)).id))
                    1
                }
            }
        }
    }
    for (action in listOf("invite", "kick", "promote", "demote")) {
        literal(action) {
            argument("group", ClientStringArgumentType.string(), chatGroups) { group ->
                argument("user", ClientStringArgumentType.word(), chatFriends) { user ->
                    exec { ctx ->
                        val packet = when (action) {
                            "promote" -> C2SGroupPacket("promote", group(ctx.get(group)).id,
                                user = ctx.get(user), admin = true)
                            "demote" -> C2SGroupPacket("promote", group(ctx.get(group)).id,
                                user = ctx.get(user), admin = false)
                            else -> C2SGroupPacket(action, group(ctx.get(group)).id, user = ctx.get(user))
                        }
                        sendChatPacket(packet)
                        1
                    }
                }
            }
        }
    }
    literal("rename") {
        argument("group", ClientStringArgumentType.string(), chatGroups) { group ->
            argument("name", StringArgumentType.greedyString()) { name ->
                exec { ctx ->
                    sendChatPacket(C2SGroupPacket("rename", group(ctx.get(group)).id, name = ctx.get(name)))
                    1
                }
            }
        }
    }
    literal("say") {
        argument("group", ClientStringArgumentType.string(), chatGroups) { group ->
            argument("message", StringArgumentType.greedyString()) { message ->
                exec { ctx ->
                    val target = group(ctx.get(group))
                    GlobalSettingsClientChat.send(ChatSession.GROUP_PREFIX + target.id, ctx.get(message))
                    1
                }
            }
        }
    }
    literal("list") {
        exec {
            printLine(regular(t("group.list.header", variable(ChatSession.groups.size.toString()))))
            for (group in ChatSession.groups) {
                val members = group.members.orEmpty()
                printLine(
                    variable(group.name),
                    regular(" (${group.role}) "),
                    regular(t("group.list.members", members.count { it.online }, members.size)),
                    regular(": " + members.joinToString { it.user.name }),
                )
            }
            1
        }
    }
}
