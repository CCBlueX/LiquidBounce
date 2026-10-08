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
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.packet.AxochatPacket
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.minecraft.network.chat.Component

internal val LIQUIDCHAT_MESSAGE = MessageMetadata(id = "LiquidChat#command", remove = false)

internal fun requireChat() {
    val client = GlobalSettingsClientChat.chatClient
    val reason = when {
        !client.isConnected -> "liquidbounce.liquidchat.notConnected"
        !client.isLoggedIn -> "liquidbounce.liquidchat.notLoggedIn"
        !client.isModern -> "liquidbounce.liquidchat.requiresV2"
        else -> null
    }
    if (reason != null) {
        throw CommandException(translation(reason))
    }
}

internal fun sendChatPacket(packet: AxochatPacket.C2S) {
    requireChat()
    GlobalSettingsClientChat.chatClient.sendPacket(packet)
}

internal fun printLine(vararg parts: Component) = chat(*parts, metadata = LIQUIDCHAT_MESSAGE)

internal val chatUsers: SuggestionProvider<ClientCommandSource> = suggestions {
    ChatSession.knownNames() + ClientCommandSource.onlinePlayerNames
}

internal val chatAccounts: SuggestionProvider<ClientCommandSource> = suggestions {
    ChatSession.knownNames(accounts = true)
}

internal val chatFriends: SuggestionProvider<ClientCommandSource> = suggestions {
    ChatSession.friends.map { it.user.name }
}

internal val chatGroups: SuggestionProvider<ClientCommandSource> = suggestions {
    ChatSession.groups.map { it.name }
}

/**
 * LiquidChat Command
 *
 * Messages, friends, blocks, groups, server chat and reports.
 */
object CommandLiquidChat : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("liquidchat", aliases = listOf("lc")) {
            msgCommand()
            friendCommand()
            friendsCommand()
            blockCommand()
            unblockCommand()
            groupCommands()
            serverCommand()
            reportCommand()
        }
    }

}
