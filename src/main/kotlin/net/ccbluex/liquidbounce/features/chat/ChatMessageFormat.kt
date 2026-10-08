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

package net.ccbluex.liquidbounce.features.chat

import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.plus
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.Style

/**
 * Builds the prefixes of LiquidChat messages: channel, head, role and name.
 */
object ChatMessageFormat {

    fun messagePrefix(event: ClientChatMessageEvent, playerSprite: Component): Component {
        val author = event.author
        val channel = event.channel

        if (author != null && channel != null && ChatSession.isSelf(author.id)
            && channel.startsWith(ChatSession.USER_PREFIX)) {
            return directMessagePrefix(ChatSession.nameOf(channel.removePrefix(ChatSession.USER_PREFIX)))
        }

        val role = author?.roles?.firstOrNull()
        val nameColor = when {
            event.chatGroup == ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT -> ChatFormatting.BLUE
            role?.staff == true -> ChatFormatting.RED
            author?.highlight == true -> ChatFormatting.GOLD
            else -> ChatFormatting.GRAY
        }
        val name = event.user.name.asPlainText(
            Style.EMPTY + nameColor +
                ClickEvent.CopyToClipboard(event.user.name) +
                HoverEvent.ShowText(event.user.name.asPlainText())
        )

        val parts = mutableListOf<Component>()
        channelTag(event)?.let(parts::add)
        if (event.chatGroup == ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT) {
            parts += "[".asPlainText(ChatFormatting.DARK_GRAY)
        }
        parts += playerSprite
        parts += PlainText.SPACE
        if (role != null) {
            parts += "[".asPlainText(ChatFormatting.DARK_GRAY)
            parts += role.name.asPlainText(if (role.staff) ChatFormatting.RED else ChatFormatting.GOLD)
            parts += "] ".asPlainText(ChatFormatting.DARK_GRAY)
        }
        parts += name
        parts += if (event.chatGroup == ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT) {
            "] ".asPlainText(ChatFormatting.DARK_GRAY)
        } else {
            " ▸ ".asPlainText(ChatFormatting.DARK_GRAY)
        }
        return parts.asText()
    }

    private fun channelTag(event: ClientChatMessageEvent): Component? {
        val (name, color) = when (event.chatGroup) {
            ClientChatMessageEvent.ChatGroup.SERVER_CHAT ->
                translation("liquidbounce.liquidchat.channel.server").string to ChatFormatting.DARK_AQUA
            ClientChatMessageEvent.ChatGroup.PARTY_CHAT ->
                translation("liquidbounce.liquidchat.channel.party").string to ChatFormatting.LIGHT_PURPLE
            ClientChatMessageEvent.ChatGroup.GROUP_CHAT ->
                ChatSession.channelName(event.channel ?: return null) to ChatFormatting.GREEN
            else -> return null
        }

        return bracketed("[", name, "] ", color)
    }

    private fun bracketed(open: String, name: String, close: String, color: ChatFormatting) = listOf(
        open.asPlainText(ChatFormatting.DARK_GRAY),
        name.asPlainText(color),
        close.asPlainText(ChatFormatting.DARK_GRAY),
    ).asText()

    fun directMessagePrefix(receiver: String) = bracketed("[→ ", receiver, "] ", ChatFormatting.BLUE)

}
