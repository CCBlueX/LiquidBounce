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
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat

internal fun CmdLiteralScope.msgCommand() = literal("msg") {
    argument("user", ClientStringArgumentType.word(), chatAccounts) { user ->
        argument("message", StringArgumentType.greedyString()) { message ->
            exec { ctx ->
                GlobalSettingsClientChat.send(ChatSession.USER_PREFIX + ctx.get(user), ctx.get(message))
                1
            }
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
