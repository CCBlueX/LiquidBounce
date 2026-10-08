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

import net.ccbluex.liquidbounce.features.chat.ChatMessageFormat
import net.ccbluex.liquidbounce.features.chat.ChatNotices
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.PartyInfo
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.features.chat.party.PartyItems
import net.ccbluex.liquidbounce.features.chat.party.PartyManager
import net.ccbluex.liquidbounce.features.chat.party.PartyMemberStates
import net.ccbluex.liquidbounce.features.chat.party.amount
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.lang.translation
import net.ccbluex.liquidbounce.utils.client.MessageMetadata
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.onHover
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.HoverEvent
import net.minecraft.network.chat.MutableComponent
import net.minecraft.world.phys.Vec3
import kotlin.math.roundToInt

private val ROLES = listOf("leader", "admin", "member")

private val ROLE_ICONS = mapOf(
    "leader" to ("★" to ChatFormatting.GOLD),
    "admin" to ("✦" to ChatFormatting.YELLOW),
    "member" to ("•" to ChatFormatting.GRAY),
)

internal object PartyStatus {

    // showing the party again replaces the previous list
    private val message = MessageMetadata(id = "LiquidChat#party")

    fun print(i18n: CmdI18n) = with(i18n) {
        val party = PartyManager.party ?: throw CommandException(t("notInParty"))
        val members = party.members.orEmpty()
            .sortedWith(compareBy({ ROLES.indexOf(it.role) }, { !it.online }, { it.user.name.lowercase() }))
        val ownRole = members.firstOrNull { it.relation == "self" }?.role

        val lines = listOf(header(party, members.size)) + members.map { memberLine(it, ownRole) } +
            actions(party, ownRole)
        val text = Component.empty()
        lines.forEachIndexed { index, line ->
            if (index > 0) {
                text.append("\n")
            }
            text.append(line)
        }
        chat(text, metadata = message)
    }

    private fun button(text: MutableComponent, color: ChatFormatting, packet: C2SPartyPacket) =
        ChatNotices.button(text, color) { GlobalSettingsClientChat.chatClient.sendPacket(packet) }

    private fun separator() = Component.literal(" · ").withStyle(ChatFormatting.DARK_GRAY)

    private fun CmdI18n.header(party: PartyInfo, size: Int) = Component.empty().apply {
        append(ChatMessageFormat.partyTag())
        append(regular(t("members", variable(size.toString()))))
        if (party.locked) {
            append(separator())
            append(t("locked").withStyle(ChatFormatting.YELLOW))
        }
        if (party.pvp) {
            append(separator())
            append(t("pvp").withStyle(ChatFormatting.RED))
        }
    }

    private fun CmdI18n.memberLine(member: PartyMember, ownRole: String?) = Component.empty().apply {
        val (icon, color) = ROLE_ICONS[member.role] ?: ("•" to ChatFormatting.GRAY)
        val nameColor = if (member.online) ChatFormatting.GOLD else ChatFormatting.GRAY
        append(Component.literal(" $icon ").withStyle(if (member.online) color else ChatFormatting.DARK_GRAY))
        append(ChatMessageFormat.displayName(member.user, nameColor))
        append(separator())
        append(regular(whereabouts(member)))
        health(member)?.let { append(Component.literal(" ❤ $it").withStyle(ChatFormatting.RED)) }
        if (member.relation == "self") {
            return@apply
        }

        val inventory = PartyMemberStates[member.user.id]?.inventory
        if (inventory != null && mc.level != null) {
            append(ChatNotices.button(t("button.inventory"), ChatFormatting.AQUA) {
                PartyItems.show(member, inventory)
            })
        }
        if (ownRole != null && ownRole != "member" && ROLES.indexOf(ownRole) < ROLES.indexOf(member.role)) {
            append(button(t("button.kick"), ChatFormatting.RED, C2SPartyPacket("kick", user = member.user.id)))
        }
    }

    private fun whereabouts(member: PartyMember): MutableComponent {
        val server = member.server
        if (member.relation == "elsewhere" && server != null) {
            return Component.literal(server)
        }

        val relation = translation("liquidbounce.liquidchat.party.relation.${member.relation}")
        val distance = distanceTo(member) ?: return relation
        return relation.append(", $distance m")
    }

    private fun distanceTo(member: PartyMember): Int? {
        if (member.relation != "nearby" && member.relation != "world") {
            return null
        }
        val player = mc.player ?: return null
        val dimension = mc.level?.dimension()?.identifier()?.toString() ?: return null
        val position = PartyMemberStates[member.user.id]?.position?.takeIf { it.dimension == dimension }
            ?: return null
        return player.position().distanceTo(Vec3(position.x, position.y, position.z)).roundToInt()
    }

    private fun health(member: PartyMember): Int? {
        if (!member.online || member.relation == "self") {
            return null
        }
        val status = PartyMemberStates[member.user.id]?.status ?: return null
        val health = status.amount("health") ?: return null
        return (health + (status.amount("absorption") ?: 0f)).roundToInt()
    }

    private fun CmdI18n.explained(key: String) = t("button.$key").onHover(HoverEvent.ShowText(t("button.$key.hover")))

    private fun CmdI18n.actions(party: PartyInfo, ownRole: String?) = Component.empty().apply {
        if (ownRole == "leader") {
            append(ChatNotices.button(explained("warp"), ChatFormatting.GREEN) { PartyManager.warp() })
            val lock = explained(if (party.locked) "unlock" else "lock")
            append(button(lock, ChatFormatting.YELLOW, C2SPartyPacket("lock", locked = !party.locked)))
            append(button(t("button.disband"), ChatFormatting.RED, C2SPartyPacket("disband")))
        }
        append(button(t("button.leave"), ChatFormatting.RED, C2SPartyPacket("leave")))
    }

}
