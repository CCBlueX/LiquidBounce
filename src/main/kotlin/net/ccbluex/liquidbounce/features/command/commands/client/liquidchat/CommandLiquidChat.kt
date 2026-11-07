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

import com.mojang.brigadier.CommandDispatcher
import com.mojang.brigadier.arguments.StringArgumentType
import net.ccbluex.axochat.AxochatSession
import net.ccbluex.axochat.protocol.Channel
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.LiquidChatClient
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation

object CommandLiquidChat : CommandRegistrar {

    private val chatUsers = suggestions<ClientCommandSource> {
        ChatSession.knownNames() + ClientCommandSource.onlinePlayerNames
    }

    private val chatAccounts = suggestions<ClientCommandSource> {
        ChatSession.knownNames(accounts = true)
    }

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("liquidchat", aliases = listOf("lc")) {
            literal("msg") {
                argument("user", ClientStringArgumentType.word(), chatAccounts) { user ->
                    argument("message", StringArgumentType.greedyString()) { message ->
                        exec { ctx ->
                            GlobalSettingsClientChat.send(Channel.User(ctx.get(user)), ctx.get(message))
                            1
                        }
                    }
                }
            }
            literal("server") {
                argument("message", StringArgumentType.greedyString()) { message ->
                    exec { ctx ->
                        GlobalSettingsClientChat.send(Channel.Server, ctx.get(message))
                        1
                    }
                }
            }
            literal("report") {
                argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
                    argument("reason", StringArgumentType.greedyString()) { reason ->
                        exec { ctx ->
                            val reference = ctx.get(user)
                            val target = ChatSession.findUser(reference)?.id
                            val message = target?.let(ChatSession::lastMessageOf)
                            modernSession().send(Serverbound.Report(target ?: reference, message, ctx.get(reason)))
                            1
                        }
                    }
                }
            }
        }
    }

    private fun modernSession(): AxochatSession {
        val session = LiquidChatClient.session
        val reason = when {
            session?.isOpen != true -> "notConnected"
            !LiquidChatClient.isLoggedIn -> "notLoggedIn"
            !session.isModern -> "requiresV2"
            else -> return session
        }
        throw CommandException(translation("liquidbounce.liquidchat.$reason"))
    }

}
