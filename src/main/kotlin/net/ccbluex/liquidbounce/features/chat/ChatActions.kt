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

import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.axochat.user.UserRef
import net.ccbluex.liquidbounce.features.chat.party.PartyManager
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

/**
 * The server answers the same whether the other user exists or not, and so do these confirmations.
 */
object ChatActions {

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.$key", *args)

    private fun send(packet: Serverbound) = GlobalSettingsClientChat.chatClient.sendPacket(packet)

    private fun shown(user: String): Component =
        ChatSession.findUser(user)?.let { ChatMessageFormat.displayName(it) } ?: variable(user)

    private fun suggestion(text: MutableComponent, color: ChatFormatting, command: String): Component {
        val click = ClickEvent.SuggestCommand(CommandManager.GlobalSettings.prefix + command)
        return buttonOf(text.withStyle(color).onClick(click))
    }

    /**
     * Messages, friends and parties are between LiquidBounce Accounts only.
     */
    fun userActions(user: UserRef) {
        val actions = mutableListOf<Component>()
        if (user.isAccount && ChatSession.isAccount) {
            actions += suggestion(t("action.message"), ChatFormatting.AQUA, "lc msg ${user.name} ")
            actions += ChatNotices.button(t("action.party"), ChatFormatting.LIGHT_PURPLE) { inviteToParty(user.id) }
            if (ChatSession.findFriend(user.id) == null) {
                actions += ChatNotices.button(t("action.friend"), ChatFormatting.GREEN) { requestFriend(user.id) }
            }
        }
        actions += ChatNotices.button(t("action.block"), ChatFormatting.RED) { block(user.id, true) }
        actions += suggestion(t("action.report"), ChatFormatting.YELLOW, "lc report ${user.name} ")
        GlobalSettingsClientChat.notice(Component.empty().append(ChatMessageFormat.displayName(user)).apply {
            actions.forEach(::append)
        })
    }

    fun inviteToParty(user: String) {
        send(Serverbound.Party.Invite(user))
        PartyManager.notice(regular(t("party.invited", shown(user))))
    }

    fun requestFriend(user: String) {
        send(Serverbound.Friend(Serverbound.FriendAction.Request, user))
        GlobalSettingsClientChat.notice(regular(t("friend.requested", shown(user))))
    }

    fun block(user: String, blocked: Boolean) {
        // the server finds Minecraft accounts by id only
        send(Serverbound.Block(ChatSession.findUser(user)?.id ?: user, blocked))
        GlobalSettingsClientChat.notice(regular(t(if (blocked) "blocked" else "unblocked", shown(user))))
    }

}
