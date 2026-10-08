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

private val partyPlayers: SuggestionProvider<ClientCommandSource> = suggestions {
    PartyManager.invites.values.map { it.from.name } + ChatSession.knownNames() + ClientCommandSource.onlinePlayerNames
}

/**
 * Party Command
 *
 * Invites to, manages and warps the LiquidChat party.
 */
object CommandParty : CommandRegistrar {

    override fun register(dispatcher: CommandDispatcher<ClientCommandSource>) {
        dispatcher.register("party", aliases = listOf("p")) {
            exec {
                printParty()
                1
            }
            simpleAction("leave") { C2SPartyPacket("leave") }
            simpleAction("warp") { C2SPartyPacket("warp") }
            simpleAction("disband") { C2SPartyPacket("disband") }
            literal("lock") {
                exec {
                    val party = PartyManager.party ?: throw CommandException(t("notInParty"))
                    sendChatPacket(C2SPartyPacket("lock", locked = !party.locked))
                    1
                }
            }
            memberAction("kick") { C2SPartyPacket("kick", user = it.user.id) }
            memberAction("leader") { C2SPartyPacket("transfer", user = it.user.id) }
            literal("inv", aliases = listOf("inventory")) {
                requires { it.isIngame }
                argument("member", ClientStringArgumentType.word(), partyMembers) { member ->
                    exec { ctx ->
                        showInventory(requireMember(ctx.get(member)))
                        1
                    }
                }
            }
            argument("player", ClientStringArgumentType.word(), partyPlayers) { player ->
                exec { ctx ->
                    inviteOrJoin(ctx.get(player))
                    1
                }
            }
        }
    }

    /**
     * Joins the party of [reference] if they invited us, otherwise invites them.
     */
    private fun inviteOrJoin(reference: String) {
        val invite = PartyManager.invites.values.firstOrNull {
            it.from.id == reference || it.from.name.equals(reference, true)
        }
        if (invite != null) {
            PartyManager.invites.remove(invite.party)
            sendChatPacket(C2SPartyPacket("accept", party = invite.party))
        } else {
            sendChatPacket(C2SPartyPacket("invite", user = reference))
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
            ?: throw CommandException(t("inv.unknown", member.user.name))
        val viewed = PartyItems.viewedPlayer(member, inventory, world)
        mc.schedule {
            mc.gui.setScreen(ViewedInventoryScreen { viewed })
        }
    }

    private fun CmdI18n.printParty() {
        val party = PartyManager.party ?: throw CommandException(t("notInParty"))
        val members = party.members.orEmpty()

        printLine(regular(t(
            "header",
            variable(members.size.toString()),
            t(if (party.locked) "locked" else "open"),
        )))
        for (member in members) {
            printLine(
                Component.literal(if (member.online) "● " else "○ ")
                    .withStyle(if (member.online) ChatFormatting.GREEN else ChatFormatting.DARK_GRAY),
                variable(member.user.name),
                regular(" ${member.role}, ${member.relation}"),
                regular(member.server?.let { " ($it)" } ?: ""),
            )
        }
    }

}
