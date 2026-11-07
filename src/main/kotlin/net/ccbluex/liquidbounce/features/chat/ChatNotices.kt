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

import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.axochat.protocol.SuccessReason
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

object ChatNotices : EventListener {

    private val timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.$key", *args)

    private fun notice(vararg parts: Component) = GlobalSettingsClientChat.notice(
        Component.empty().apply { parts.forEach(::append) }
    )

    private fun formatTime(millis: Long): String = timeFormat.format(Instant.ofEpochMilli(millis))

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is Clientbound.Punished -> notice(regular(t(
                "punished.${packet.kind}",
                packet.reason,
                packet.expires?.let(::formatTime) ?: t("punished.permanent").string,
            )).withStyle(ChatFormatting.RED))
            is Clientbound.Success -> if (packet.reason == SuccessReason.Report) {
                notice(regular(t("success.report")))
            }

            else -> {}
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
