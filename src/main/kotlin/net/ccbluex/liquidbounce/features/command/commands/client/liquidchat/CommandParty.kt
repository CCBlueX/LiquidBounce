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
import net.ccbluex.liquidbounce.features.chat.packet.C2SPartyPacket
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.features.chat.party.PartyItems
import net.ccbluex.liquidbounce.features.chat.party.PartyManager
import net.ccbluex.liquidbounce.features.chat.party.PartyMemberStates
import net.ccbluex.liquidbounce.features.command.CommandException
import net.ccbluex.liquidbounce.features.command.CommandRegistrar
import net.ccbluex.liquidbounce.features.command.arguments.BooleanArgumentType
import net.ccbluex.liquidbounce.features.command.arguments.ClientStringArgumentType
import net.ccbluex.liquidbounce.features.command.brigadier.ClientCommandSource
import net.ccbluex.liquidbounce.features.command.brigadier.CmdI18n
import net.ccbluex.liquidbounce.features.command.brigadier.CmdLiteralScope
import net.ccbluex.liquidbounce.features.command.brigadier.get
import net.ccbluex.liquidbounce.features.command.brigadier.register
import net.ccbluex.liquidbounce.features.command.brigadier.suggestions
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.regular
import net.ccbluex.liquidbounce.utils.client.variable
import net.ccbluex.liquidbounce.utils.client.world
import net.ccbluex.liquidbounce.utils.inventory.ViewedInventoryScreen
import net.minecraft.ChatFormatting
import net.minecraft.network.chat.Component

private val partyMembers: SuggestionProvider<ClientCommandSource> = suggestions {
    PartyManager.others.map { it.user.name }
}

private val partyInviters: SuggestionProvider<ClientCommandSource> = suggestions {
    PartyManager.invites.values.map { it.from.name }
}

/**
 * Party Command
 *
 * Invites to, manages and warps the LiquidChat party.
 */
object CommandParty : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("party", aliases = listOf("p")) {
            literal("invite") {
                argument("user", ClientStringArgumentType.word(), chatUsers) { user ->
                    exec { ctx ->
                        sendChatPacket(C2SPartyPacket("invite", user = ctx.get(user)))
                        1
                    }
                }
            }
            inviteAnswer("join", "accept")
            inviteAnswer("decline", "decline")
            memberAction("kick") { C2SPartyPacket("kick", user = it.user.id) }
            memberAction("promote") { C2SPartyPacket("promote", user = it.user.id, admin = true) }
            memberAction("demote") { C2SPartyPacket("promote", user = it.user.id, admin = false) }
            memberAction("setleader") { C2SPartyPacket("transfer", user = it.user.id) }
            memberAction("mute") { C2SPartyPacket("mute", user = it.user.id, muted = true) }
            memberAction("unmute") { C2SPartyPacket("mute", user = it.user.id, muted = false) }
            simpleAction("leave") { C2SPartyPacket("leave") }
            simpleAction("lock") { C2SPartyPacket("lock", locked = true) }
            simpleAction("unlock") { C2SPartyPacket("lock", locked = false) }
            simpleAction("warp") { C2SPartyPacket("warp") }
            simpleAction("disband") { C2SPartyPacket("disband") }
            literal("pvp") {
                optional("enabled", BooleanArgumentType("enabled")) { enabled ->
                    exec { ctx ->
                        val party = PartyManager.party ?: throw CommandException(t("notInParty"))
                        sendChatPacket(C2SPartyPacket("pvp", enabled = ctx.get(enabled) ?: !party.pvp))
                        1
                    }
                }
            }
            literal("chat") {
                exec {
                    ChatSession.channel = if (ChatSession.channel == ChatSession.PARTY) {
                        ChatSession.GLOBAL
                    } else {
                        ChatSession.PARTY
                    }
                    printLine(regular(t("chat.${ChatSession.channel}")))
                    1
                }
            }
            literal("inventory", aliases = listOf("inv")) {
                requires { it.isIngame }
                argument("member", ClientStringArgumentType.word(), partyMembers) { member ->
                    exec { ctx ->
                        showInventory(requireMember(ctx.get(member)))
                        1
                    }
                }
            }
            literal("list", aliases = listOf("status")) {
                exec {
                    printParty()
                    1
                }
            }
        }
    }

    private fun CmdLiteralScope.inviteAnswer(name: String, action: String) = literal(name) {
        argument("user", ClientStringArgumentType.word(), partyInviters) { user ->
            exec { ctx ->
                val reference = ctx.get(user)
                val invite = PartyManager.invites[reference]
                    ?: PartyManager.invites.values.firstOrNull { it.from.name.equals(reference, true) }
                    ?: throw CommandException(t("noInvite", reference))
                PartyManager.invites.remove(invite.party)
                sendChatPacket(C2SPartyPacket(action, party = invite.party))
                1
            }
        }
    }

    private fun CmdLiteralScope.memberAction(name: String, packet: (PartyMember) -> C2SPartyPacket) = literal(name) {
        argument("member", ClientStringArgumentType.word(), partyMembers) { member ->
            exec { ctx ->
                sendChatPacket(packet(requireMember(ctx.get(member))))
                1
            }
        }
    }

    private fun CmdLiteralScope.simpleAction(name: String, packet: () -> C2SPartyPacket) = literal(name) {
        exec {
            sendChatPacket(packet())
            1
        }
    }

    private fun CmdI18n.requireMember(reference: String): PartyMember {
        if (PartyManager.party == null) {
            throw CommandException(t("notInParty"))
        }

        return PartyManager.member(reference) ?: throw CommandException(t("unknownMember", reference))
    }

    private fun CmdI18n.showInventory(member: PartyMember) {
        val inventory = PartyMemberStates[member.user.id]?.inventory
            ?: throw CommandException(t("inventory.unknown", member.user.name))
        val viewed = PartyItems.viewedPlayer(member, inventory, world)
        mc.schedule {
            mc.gui.setScreen(ViewedInventoryScreen { viewed })
        }
    }

    private fun CmdI18n.printParty() {
        val party = PartyManager.party ?: throw CommandException(t("notInParty"))
        val members = party.members.orEmpty()

        printLine(regular(t(
            "list.header",
            variable(members.size.toString()),
            t(if (party.pvp) "list.pvpOn" else "list.pvpOff"),
            t(if (party.locked) "list.locked" else "list.open"),
        )))
        for (member in members) {
            printLine(
                Component.literal(if (member.online) "● " else "○ ")
                    .withStyle(if (member.online) ChatFormatting.GREEN else ChatFormatting.DARK_GRAY),
                variable(member.user.name),
                regular(" ${member.role}, ${member.relation}"),
                regular(member.server?.let { " ($it)" } ?: ""),
                regular(if (member.muted) " [muted]" else ""),
            )
        }
    }

}
