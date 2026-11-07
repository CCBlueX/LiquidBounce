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

import net.ccbluex.axochat.protocol.Channel
import net.ccbluex.axochat.user.UserRef
import net.ccbluex.liquidbounce.event.events.ClientChatMessageEvent
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.text.PlainText
import net.ccbluex.liquidbounce.utils.text.asPlainText
import net.ccbluex.liquidbounce.utils.text.asText
import net.ccbluex.liquidbounce.utils.text.buildText
import net.ccbluex.liquidbounce.utils.text.plus
import net.ccbluex.liquidbounce.utils.text.textOf
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.network.chat.Style

object ChatMessageFormat {

    fun displayName(user: UserRef, color: ChatFormatting = ChatFormatting.GOLD): MutableComponent {
        if (!user.isAccount) {
            return Component.literal(user.name).withStyle(ChatFormatting.GRAY)
                .append(Component.literal(" [MC]").withStyle(ChatFormatting.DARK_GRAY))
        }

        val name = Component.literal(user.name).withStyle(color)
        val minecraft = user.minecraft?.name
        if (minecraft != null && !minecraft.equals(user.name, true)) {
            name.append(Component.literal(" ($minecraft)").withStyle(ChatFormatting.GRAY))
        }
        return name
    }

    fun messagePrefix(event: ClientChatMessageEvent, playerSprite: Component): Component {
        val author = event.author
        val channel = event.channel?.let(Channel::parse)

        if (author != null && channel is Channel.User && ChatSession.isSelf(author.id)) {
            val receiver = channel.user
            val name = ChatSession.userOf(receiver)?.let { displayName(it, ChatFormatting.BLUE) }
                ?: ChatSession.nameOf(receiver).asPlainText(ChatFormatting.BLUE)
            return directMessagePrefix(name)
        }

        val role = author?.roles?.firstOrNull()
        val nameColor = when {
            event.chatGroup == ClientChatMessageEvent.ChatGroup.PRIVATE_CHAT -> ChatFormatting.BLUE
            role?.staff == true -> ChatFormatting.RED
            author?.highlight == true -> ChatFormatting.GOLD
            else -> ChatFormatting.GRAY
        }
        val name = when {
            author == null -> event.user.name.asPlainText(
                Style.EMPTY + nameColor +
                    ClickEvent.CopyToClipboard(event.user.name) +
                    HoverEvent.ShowText(event.user.name.asPlainText())
            )
            ChatSession.isSelf(author.id) -> displayName(author.toUserRef(), nameColor)
            else -> {
                val user = author.toUserRef()
                displayName(user, nameColor)
                    .onHover(HoverEvent.ShowText(translation("liquidbounce.liquidchat.action.hover")))
                    .onClickRun { userActions(user) }
            }
        }

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
            ClientChatMessageEvent.ChatGroup.SERVER_CHAT -> {
                // staff watching every server see where a message comes from
                val watched = (event.channel?.let(Channel::parse) as? Channel.Watched)?.address
                (watched ?: translation("liquidbounce.liquidchat.channel.server").string) to ChatFormatting.DARK_AQUA
            }
            else -> return null
        }

        return bracketed("[", name.asPlainText(color), "] ")
    }

    private fun bracketed(open: String, name: Component, close: String) = textOf(
        open.asPlainText(ChatFormatting.DARK_GRAY),
        name,
        close.asPlainText(ChatFormatting.DARK_GRAY),
    )

    fun directMessagePrefix(receiver: Component) = bracketed("[→ ", receiver, "] ")

    private fun suggestion(text: MutableComponent, color: ChatFormatting, command: String): Component {
        val click = ClickEvent.SuggestCommand(CommandManager.GlobalSettings.prefix + command)
        return Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY)
            .append(text.withStyle(color).onClick(click))
            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
    }

    /**
     * Direct messages are between LiquidBounce Accounts only.
     */
    private fun userActions(user: UserRef) {
        GlobalSettingsClientChat.notice(
            buildText {
                this += displayName(user)
                if (user.isAccount && ChatSession.isAccount) {
                    this += suggestion(
                        translation("liquidbounce.liquidchat.action.message"),
                        ChatFormatting.AQUA,
                        "lc msg ${user.name} ",
                    )
                }
                this += suggestion(
                    translation("liquidbounce.liquidchat.action.report"),
                    ChatFormatting.YELLOW,
                    "lc report ${user.name} ",
                )
            }
        )
    }

}
