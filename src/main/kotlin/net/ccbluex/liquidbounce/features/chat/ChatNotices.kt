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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.AxochatPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SFriendPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SGroupPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SResolveReportPacket
import net.ccbluex.liquidbounce.features.chat.packet.ChatPunishment
import net.ccbluex.liquidbounce.features.chat.packet.ChatReport
import net.ccbluex.liquidbounce.features.chat.packet.S2CFriendsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CGroupsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPresencePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPunishedPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPunishmentsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CReportCreatedPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CReportsPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CSuccessPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CUserCountPacket
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.onClickRun
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.MutableComponent
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

internal fun buttonOf(text: MutableComponent): Component =
    Component.literal(" [").withStyle(ChatFormatting.DARK_GRAY)
        .append(text)
        .append(Component.literal("]").withStyle(ChatFormatting.DARK_GRAY))

/**
 * Prints what the chat server reports besides messages: requests, presence and moderation.
 */
object ChatNotices : EventListener {

    private val timeFormat = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).withZone(ZoneId.systemDefault())

    private var knownRequests = emptySet<String>()
    private var knownFriends: Set<String>? = null
    private var knownInvites = emptySet<String>()

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.$key", *args)

    private fun notice(vararg parts: Component) = GlobalSettingsClientChat.notice(
        Component.empty().apply { parts.forEach(::append) }
    )

    fun button(text: MutableComponent, color: ChatFormatting, action: () -> Unit): Component =
        buttonOf(text.withStyle(color).onClickRun(action))

    private fun send(packet: AxochatPacket.C2S) =
        GlobalSettingsClientChat.chatClient.sendPacket(packet)

    fun formatTime(millis: Long): String = timeFormat.format(Instant.ofEpochMilli(millis))

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is S2CFriendsPacket -> onFriends(packet)
            is S2CPresencePacket -> onPresence(packet)
            is S2CGroupsPacket -> onGroups(packet)
            is S2CPunishedPacket -> notice(regular(t(
                "punished.${packet.kind}",
                packet.reason,
                packet.expires?.let(::formatTime) ?: t("punished.permanent").string,
            )).withStyle(ChatFormatting.RED))
            is S2CSuccessPacket -> if (packet.reason in setOf("Report", "Punish", "Pardon", "Resolve")) {
                notice(regular(t("success.${packet.reason.lowercase()}")))
            }
            is S2CUserCountPacket -> notice(regular(t("userCount", variable(packet.connections.toString()),
                variable(packet.loggedIn.toString()))))
            is S2CReportCreatedPacket -> printReport(packet.report)
            is S2CReportsPacket -> {
                val reports = packet.reports.orEmpty()
                notice(regular(t("reports", variable(reports.size.toString()))))
                reports.forEach(::printReport)
            }
            is S2CPunishmentsPacket -> {
                notice(regular(t("punishments", ChatMessageFormat.displayName(packet.user))))
                packet.punishments.orEmpty().forEach(::printPunishment)
            }

            else -> {}
        }
    }

    private fun onFriends(packet: S2CFriendsPacket) {
        val friends = packet.friends.orEmpty().map { it.user }
        knownFriends?.let { known ->
            friends.filter { it.id !in known }
                .forEach { notice(regular(t("friend.added", ChatMessageFormat.displayName(it)))) }
        }
        knownFriends = friends.mapTo(hashSetOf()) { it.id }

        val incoming = packet.incoming.orEmpty()
        for (user in incoming) {
            if (user.id in knownRequests) {
                continue
            }

            notice(
                regular(t("friend.request", ChatMessageFormat.displayName(user))),
                button(t("accept"), ChatFormatting.GREEN) { send(C2SFriendPacket("accept", user.id)) },
                button(t("decline"), ChatFormatting.RED) { send(C2SFriendPacket("decline", user.id)) },
            )
        }
        knownRequests = incoming.mapTo(hashSetOf()) { it.id }
    }

    private fun onPresence(packet: S2CPresencePacket) {
        val friend = ChatSession.findFriend(packet.user) ?: return
        val name = ChatMessageFormat.displayName(friend.user)
        val server = packet.server

        when {
            !packet.online -> notice(regular(t("presence.offline", name)))
            server == null -> notice(regular(t("presence.online", name)))
            else -> notice(
                regular(t("presence.playing", name, variable(server))),
                button(t("join"), ChatFormatting.GREEN) { ServerJoin.confirm(server, friend.user.name) },
            )
        }
    }

    private fun onGroups(packet: S2CGroupsPacket) {
        val invites = packet.groups.orEmpty().filter { it.role == "invited" }
        for (group in invites) {
            if (group.id in knownInvites) {
                continue
            }

            notice(
                regular(t("group.invite", variable(group.name))),
                button(t("accept"), ChatFormatting.GREEN) { send(C2SGroupPacket("accept", group = group.id)) },
                button(t("decline"), ChatFormatting.RED) { send(C2SGroupPacket("decline", group = group.id)) },
            )
        }
        knownInvites = invites.mapTo(hashSetOf()) { it.id }
    }

    private fun printReport(report: ChatReport) = notice(
        regular(t(
            "report",
            ChatMessageFormat.displayName(report.reporter),
            ChatMessageFormat.displayName(report.target),
            report.reason,
            formatTime(report.time),
        )),
        regular(report.content?.let { " \"$it\"" } ?: ""),
        button(t("resolve"), ChatFormatting.GREEN) {
            send(C2SResolveReportPacket(report.id))
        },
    )

    private fun printPunishment(punishment: ChatPunishment) = notice(regular(t(
        "punishment",
        punishment.kind,
        punishment.reason,
        punishment.ip ?: "-",
        punishment.issuedBy?.name ?: "-",
        punishment.expires?.let(::formatTime) ?: t("punished.permanent").string,
    )))

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        if (event.state == ClientChatStateChange.State.DISCONNECTED) {
            knownRequests = emptySet()
            knownFriends = null
            knownInvites = emptySet()
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
