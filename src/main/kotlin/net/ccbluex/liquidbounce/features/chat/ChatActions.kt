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

import net.ccbluex.axochat.user.UserRef
import net.ccbluex.liquidbounce.features.command.CommandManager
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.onClick
import net.ccbluex.liquidbounce.utils.text.buildText
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.ClickEvent
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent

object ChatActions {

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.$key", *args)

    private fun suggestion(text: MutableComponent, color: ChatFormatting, command: String): Component {
        val click = ClickEvent.SuggestCommand(CommandManager.GlobalSettings.prefix + command)
        return Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY)
            .append(text.withStyle(color).onClick(click))
            .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))
    }

    /**
     * Direct messages are between LiquidBounce Accounts only.
     */
    fun userActions(user: UserRef) {
        GlobalSettingsClientChat.notice(
            buildText {
                this += ChatMessageFormat.displayName(user)
                if (user.isAccount && ChatSession.isAccount) {
                    this += suggestion(t("action.message"), ChatFormatting.AQUA, "lc msg ${user.name} ")
                }
                this += suggestion(t("action.report"), ChatFormatting.YELLOW, "lc report ${user.name} ")
            }
        )
    }

}
