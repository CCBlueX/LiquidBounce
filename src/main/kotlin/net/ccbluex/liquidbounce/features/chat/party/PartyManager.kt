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

import net.ccbluex.axochat.party.PartyInfo
import net.ccbluex.axochat.party.PartyMember
import net.ccbluex.axochat.party.Relation
import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.TagEntityEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.ChatMessageFormat
import net.ccbluex.liquidbounce.features.chat.ChatNotices
import net.ccbluex.liquidbounce.features.chat.ChatSession
import net.ccbluex.liquidbounce.features.chat.ServerJoin
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.warning
import net.ccbluex.liquidbounce.utils.text.isSensitiveAddress
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.player.Player
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val SAME_SERVER = setOf(Relation.Nearby, Relation.World, Relation.Instance, Relation.Server)

object PartyManager : EventListener {

    @Volatile
    var party: PartyInfo? = null
        private set

    val invites = ConcurrentHashMap<String, Clientbound.PartyInvite>()

    @Volatile
    private var memberUuids = emptySet<UUID>()

    @Volatile
    private var memberNames = emptySet<String>()

    val members: List<PartyMember>
        get() = party?.members.orEmpty()

    val others: List<PartyMember>
        get() = members.filter { it.relation != Relation.Self }

    val isLeader: Boolean
        get() = party?.leader?.let(ChatSession::isSelf) == true

    fun member(reference: String): PartyMember? = members.firstOrNull { it.user.id == reference }
        ?: members.firstOrNull { it.user.name.equals(reference, true) }
        ?: members.firstOrNull { it.player?.name.equals(reference, true) }

    fun isMember(entity: Entity) =
        entity is Player && (entity.uuid in memberUuids || entity.gameProfile.name.lowercase() in memberNames)

    private fun t(key: String, vararg args: Any?) = translation("liquidbounce.liquidchat.party.$key", *args)

    internal fun notice(vararg parts: Component) = GlobalSettingsClientChat.notice(
        Component.empty().append(ChatMessageFormat.partyTag()).apply { parts.forEach(::append) }
    )

    private fun update(newParty: PartyInfo?) {
        val previous = party
        party = newParty
        ChatSession.remember(newParty?.members.orEmpty().map { it.user })
        PartyMemberStates.publish(newParty)

        val others = newParty?.members.orEmpty().filter { it.relation != Relation.Self }
        memberUuids = others.mapNotNullTo(hashSetOf()) { it.player?.uuid }
        memberNames = others.mapNotNullTo(hashSetOf()) { it.player?.name?.lowercase() }

        when {
            newParty == null && previous != null -> notice(regular(t("left")))
            newParty != null && previous?.id != newParty.id -> notice(regular(t("joined")))
            newParty != null && previous != null -> announceChanges(previous, newParty)
        }
    }

    private fun announceChanges(previous: PartyInfo, current: PartyInfo) {
        val before = previous.members.orEmpty().associateBy { it.user.id }
        val after = current.members.orEmpty().associateBy { it.user.id }

        val name = { member: PartyMember -> ChatMessageFormat.displayName(member.user) }
        (after.keys - before.keys).forEach { notice(regular(t("memberJoined", name(after.getValue(it))))) }
        (before.keys - after.keys).forEach { notice(regular(t("memberLeft", name(before.getValue(it))))) }
        if (previous.leader != current.leader) {
            val leader = after[current.leader]?.let(name) ?: variable(ChatSession.nameOf(current.leader))
            notice(regular(t("newLeader", leader)))
        }
        if (previous.locked != current.locked) {
            notice(regular(t(if (current.locked) "locked" else "unlocked")))
        }
        if (previous.pvp != current.pvp) {
            notice(regular(t(if (current.pvp) "pvpOn" else "pvpOff")))
        }
    }

    private fun onInvite(packet: Clientbound.PartyInvite) {
        invites[packet.party] = packet
        ChatSession.remember(listOf(packet.from))
        notice(
            regular(t("invite", ChatMessageFormat.displayName(packet.from))),
            ChatNotices.button(translation("liquidbounce.liquidchat.accept"), ChatFormatting.GREEN) {
                GlobalSettingsClientChat.chatClient.sendPacket(Serverbound.Party.Accept(packet.party))
            },
            ChatNotices.button(translation("liquidbounce.liquidchat.decline"), ChatFormatting.RED) {
                GlobalSettingsClientChat.chatClient.sendPacket(Serverbound.Party.Decline(packet.party))
            },
        )
    }

    fun warp() {
        val reason = when {
            mc.hasSingleplayerServer() || mc.currentServer == null -> "warp.noServer"
            mc.currentServer?.ip?.isSensitiveAddress() == true -> "warp.route"
            others.isEmpty() -> "warp.alone"
            others.all { it.relation in SAME_SERVER } -> "warp.allThere"
            else -> null
        }
        if (reason != null) {
            notice(warning(t(reason)))
        } else {
            GlobalSettingsClientChat.chatClient.sendPacket(Serverbound.Party.Warp)
        }
    }

    // the leader gets their own warp back as confirmation
    private fun onWarp(packet: Clientbound.PartyWarp) = if (ChatSession.isSelf(packet.from.id)) {
        notice(regular(t("warped", variable(packet.server))))
    } else {
        notice(
            regular(t("warp", ChatMessageFormat.displayName(packet.from), variable(packet.server))),
            ChatNotices.button(translation("liquidbounce.liquidchat.join"), ChatFormatting.GREEN) {
                ServerJoin.confirm(packet.server, packet.from.name)
            },
        )
    }

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is Clientbound.Party -> update(packet.party)
            is Clientbound.PartyInvite -> onInvite(packet)
            is Clientbound.PartyWarp -> onWarp(packet)
            else -> {}
        }

        val now = System.currentTimeMillis()
        invites.values.removeIf { it.expires < now }
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        if (event.state == ClientChatStateChange.State.DISCONNECTED) {
            if (party != null) {
                party = null
                PartyMemberStates.publish(null)
            }
            memberUuids = emptySet()
            memberNames = emptySet()
            invites.clear()
        }
    }

    @Suppress("unused")
    private val tagHandler = handler<TagEntityEvent> { event ->
        if (isMember(event.entity)) {
            event.assumePartyMember()
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
