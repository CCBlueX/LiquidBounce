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


package net.ccbluex.liquidbounce.features.chat.party

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.ChatNotices
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.ServerJoin
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.PartyInfo
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyInvitePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyWarpPacket
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object PartyManager : EventListener {

    @Volatile
    var party: PartyInfo? = null
        private set

    val invites = ConcurrentHashMap<String, S2CPartyInvitePacket>()

    @Volatile
    private var allyUuids = emptySet<UUID>()

    @Volatile
    private var allyNames = emptySet<String>()

    val members: List<PartyMember>
        get() = party?.members.orEmpty()

    val others: List<PartyMember>
        get() = members.filter { it.relation != "self" }

    val isLeader: Boolean
        get() = party?.leader?.let(ChatSession::isSelf) == true

    fun member(reference: String): PartyMember? = members.firstOrNull { it.user.id == reference }
        ?: members.firstOrNull { it.user.name.equals(reference, true) }
        ?: members.firstOrNull { it.player?.name.equals(reference, true) }

    /**
     * Party members are allies while the party has PvP turned off.
     */
    fun isAlly(uuid: UUID) = uuid in allyUuids

    fun isAlly(name: String) = name.lowercase() in allyNames

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.party.$key", *args)

    private fun notice(vararg parts: Component) = GlobalSettingsClientChat.notice(
        Component.empty().apply { parts.forEach(::append) }
    )

    private fun update(newParty: PartyInfo?) {
        val previous = party
        party = newParty
        ChatSession.remember(newParty?.members.orEmpty().map { it.user })

        val allies = newParty?.takeIf { !it.pvp }?.members.orEmpty().filter { it.relation != "self" }
        allyUuids = allies.mapNotNullTo(hashSetOf()) { it.player?.uuid }
        allyNames = allies.mapNotNullTo(hashSetOf()) { it.player?.name?.lowercase() }

        when {
            newParty == null && previous != null -> notice(regular(t("left")))
            newParty != null && previous?.id != newParty.id -> notice(regular(t("joined")))
            newParty != null && previous != null -> announceChanges(previous, newParty)
        }
    }

    private fun announceChanges(previous: PartyInfo, current: PartyInfo) {
        val before = previous.members.orEmpty().associateBy { it.user.id }
        val after = current.members.orEmpty().associateBy { it.user.id }

        (after.keys - before.keys).forEach { notice(regular(t("memberJoined", variable(after[it]!!.user.name)))) }
        (before.keys - after.keys).forEach { notice(regular(t("memberLeft", variable(before[it]!!.user.name)))) }
        if (previous.pvp != current.pvp) {
            notice(regular(t(if (current.pvp) "pvpEnabled" else "pvpDisabled")))
        }
        if (previous.leader != current.leader) {
            notice(regular(t("newLeader", variable(ChatSession.nameOf(current.leader)))))
        }
    }

    private fun onInvite(packet: S2CPartyInvitePacket) {
        invites[packet.party] = packet
        ChatSession.remember(listOf(packet.from))
        notice(
            regular(t("invite", variable(packet.from.name))),
            ChatNotices.button(translation("liquidbounce.liquidchat.accept"), ChatFormatting.GREEN) {
                GlobalSettingsClientChat.chatClient.sendPacket(C2SPartyPacket("accept", party = packet.party))
            },
            ChatNotices.button(translation("liquidbounce.liquidchat.decline"), ChatFormatting.RED) {
                GlobalSettingsClientChat.chatClient.sendPacket(C2SPartyPacket("decline", party = packet.party))
            },
        )
    }

    private fun onWarp(packet: S2CPartyWarpPacket) = notice(
        regular(t("warp", variable(packet.from.name), variable(packet.server))),
        ChatNotices.button(translation("liquidbounce.liquidchat.join"), ChatFormatting.GREEN) {
            ServerJoin.confirm(packet.server, packet.from.name)
        },
    )

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is S2CPartyPacket -> update(packet.party)
            is S2CPartyInvitePacket -> onInvite(packet)
            is S2CPartyWarpPacket -> onWarp(packet)
            else -> {}
        }

        val now = System.currentTimeMillis()
        invites.values.removeIf { it.expires < now }
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        if (event.state == ClientChatStateChange.State.DISCONNECTED) {
            party = null
            allyUuids = emptySet()
            allyNames = emptySet()
            invites.clear()
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
