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
import net.ccbluex.liquidbounce.features.chat.packet.ChatGroup
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

internal fun CmdLiteralScope.friendCommands() = literal("friend") {
    for (action in listOf("add", "remove", "accept", "decline")) {
        friendAction(action)
    }
    literal("list") {
        exec {
            val header = t("friend.list.header", variable(ChatSession.friends.size.toString()))
            printFriendList(header, t("friend.list.join"))
            for (request in ChatSession.incomingRequests) {
                printLine(regular(t("friend.list.incoming", variable(request.name))))
            }
            1
        }
    }
    literal("join") {
        argument("user", ClientStringArgumentType.word(), chatFriends) { user ->
            exec { ctx ->
                val friend = ChatSession.findFriend(ctx.get(user))
                    ?: throw CommandException(t("friend.join.unknown", ctx.get(user)))
                val server = friend.server ?: throw CommandException(t("friend.join.noServer", friend.user.name))
                ServerJoin.confirm(server, friend.user.name)
                1
            }
        }
    }
}

private fun CmdLiteralScope.friendAction(action: String) = literal(action) {
    argument("user", ClientStringArgumentType.word(), if (action == "add") chatUsers else chatFriends) { user ->
        exec { ctx ->
            val reference = ctx.get(user)
            val target = ChatSession.findFriend(reference)?.user?.id
                ?: ChatSession.incomingRequests.firstOrNull { it.name.equals(reference, true) }?.id
                ?: reference
            sendChatPacket(C2SFriendPacket(if (action == "add") "request" else action, target))
            1
        }
    }
}

private fun printFriendList(header: MutableComponent, joinLabel: MutableComponent) {
    printLine(regular(header))
    for (friend in ChatSession.friends.sortedWith(compareBy({ !it.online }, { it.user.name.lowercase() }))) {
        val server = friend.server
        val join = if (server != null) {
            ChatNotices.button(joinLabel.copy(), ChatFormatting.GREEN) { ServerJoin.confirm(server, friend.user.name) }
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
}

internal fun CmdLiteralScope.blockCommands() {
    literal("block") {
        argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
            exec { ctx ->
                sendChatPacket(C2SBlockPacket(ctx.get(user), true))
                1
            }
        }
    }
    literal("unblock") {
        argument("user", ClientStringArgumentType.word(), chatBlocks) { user ->
            exec { ctx ->
                val reference = ctx.get(user)
                val target = ChatSession.blocks.firstOrNull { it.name.equals(reference, true) }?.id ?: reference
                sendChatPacket(C2SBlockPacket(target, false))
                1
            }
        }
    }
    literal("blocks") {
        exec {
            printLine(regular(t("blocks.header", variable(ChatSession.blocks.size.toString()))))
            ChatSession.blocks.forEach { printLine(regular("- "), variable(it.name)) }
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

internal fun CmdLiteralScope.channelCommands() = literal("channel") {
    for (channel in listOf(ChatSession.GLOBAL, ChatSession.SERVER, ChatSession.PARTY)) {
        literal(channel) {
            exec {
                ChatSession.channel = channel
                printLine(regular(t("channel.set", variable(channel))))
                1
            }
        }
    }
    literal("group") {
        argument("group", ClientStringArgumentType.string(), chatGroups) { group ->
            exec { ctx ->
                val target = group(ctx.get(group))
                ChatSession.channel = ChatSession.GROUP_PREFIX + target.id
                printLine(regular(t("channel.set", variable(target.name))))
                1
            }
        }
    }
    literal("user") {
        argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
            exec { ctx ->
                ChatSession.channel = ChatSession.USER_PREFIX + ctx.get(user)
                printLine(regular(t("channel.set", variable(ctx.get(user)))))
                1
            }
        }
    }
}
