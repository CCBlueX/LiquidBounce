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
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get

/**
 * Attaches the user's latest message as evidence.
 */
internal fun CmdLiteralScope.reportCommand() = literal("report") {
    argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
        argument("reason", StringArgumentType.greedyString()) { reason ->
            exec { ctx ->
                val reference = ctx.get(user)
                val target = ChatSession.findUser(reference)?.id
                val message = target?.let(ChatSession::lastMessageOf)
                sendChatPacket(Serverbound.Report(target ?: reference, message, ctx.get(reason)))
                1
            }
        }
    }
}
