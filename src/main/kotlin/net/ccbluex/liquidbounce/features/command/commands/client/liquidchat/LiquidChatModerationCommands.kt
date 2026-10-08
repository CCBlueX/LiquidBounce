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
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.packet.C2SPardonPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SPunishPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SReportPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestPunishmentsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestReportsPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SRequestUserCountPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SResolveReportPacket
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import kotlin.time.Duration

private val PERMANENT = setOf("perm", "permanent", "forever")

/**
 * Parses `30m`, `12h`, `7d` and friends; `perm` means no expiry.
 *
 * @return seconds, or null for permanent
 */
private fun parseDuration(input: String): Long? {
    if (input.lowercase() in PERMANENT) {
        return null
    }

    return Duration.parseOrNull(input)?.inWholeSeconds?.takeIf { it > 0 }
        ?: throw CommandException(translation("liquidbounce.command.liquidchat.mod.invalidDuration", input))
}

internal fun CmdLiteralScope.reportCommand() = literal("report") {
    argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
        argument("reason", StringArgumentType.greedyString()) { reason ->
            exec { ctx ->
                val reference = ctx.get(user)
                val message = ChatSession.findUser(reference)?.id?.let(ChatSession::lastMessageOf)
                sendChatPacket(C2SReportPacket(reference, message, ctx.get(reason)))
                1
            }
        }
    }
}

internal fun CmdLiteralScope.moderationCommands() = literal("mod") {
    requires { ChatSession.isStaff }

    punishCommand("mute", "mute", includeIp = false)
    punishCommand("ban", "ban", includeIp = false)
    punishCommand("ipban", "ban", includeIp = true)
    literal("pardon") {
        argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
            exec { ctx ->
                sendChatPacket(C2SPardonPacket(ctx.get(user), null))
                1
            }
        }
    }
    literal("info") {
        argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
            exec { ctx ->
                sendChatPacket(C2SRequestPunishmentsPacket(ctx.get(user)))
                1
            }
        }
    }
    literal("reports") {
        exec {
            sendChatPacket(C2SRequestReportsPacket())
            1
        }
    }
    literal("resolve") {
        argument("id", ClientStringArgumentType.word()) { id ->
            exec { ctx ->
                sendChatPacket(C2SResolveReportPacket(ctx.get(id)))
                1
            }
        }
    }
    literal("count") {
        exec {
            // also understood by v1 servers
            if (!GlobalSettingsClientChat.checkLoggedIn()) {
                return@exec 0
            }
            GlobalSettingsClientChat.chatClient.sendPacket(C2SRequestUserCountPacket())
            1
        }
    }
}

private val durations = suggestions<ClientCommandSource>("1h", "1d", "7d", "perm")

private fun CmdLiteralScope.punishCommand(name: String, kind: String, includeIp: Boolean) = literal(name) {
    argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
        argument("duration", ClientStringArgumentType.word(), durations) { duration ->
            argument("reason", StringArgumentType.greedyString()) { reason ->
                exec { ctx ->
                    sendChatPacket(C2SPunishPacket(
                        user = ctx.get(user),
                        ip = null,
                        kind = kind,
                        duration = parseDuration(ctx.get(duration)),
                        reason = ctx.get(reason),
                        includeIp = includeIp,
                    ))
                    1
                }
            }
        }
    }
}
