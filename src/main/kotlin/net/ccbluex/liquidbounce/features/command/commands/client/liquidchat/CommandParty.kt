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
import com.mojang.brigadier.arguments.StringArgumentType
import com.mojang.brigadier.suggestion.SuggestionProvider
import net.ccbluex.axochat.party.PartyMember
import net.ccbluex.axochat.protocol.Serverbound
import net.ccbluex.liquidbounce.features.chat.ChatActions
import net.ccbluex.liquidbounce.features.chat.ChatSession
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
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat

private val partyMembers: SuggestionProvider<ClientCommandSource> = suggestions {
    PartyManager.others.map { it.user.name }
}

private val inviters: SuggestionProvider<ClientCommandSource> = suggestions {
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
            exec {
                PartyStatus.print(this)
                1
            }
            inviteCommand()
            acceptCommand()
            chatCommand()
            memberAction("kick") { Serverbound.Party.Kick(it.user.id) }
            memberAction("leader") { Serverbound.Party.Transfer(it.user.id) }
            inventoryCommand()
            simpleAction("leave") { Serverbound.Party.Leave }
            literal("warp") {
                exec {
                    requireChat()
                    PartyManager.warp()
                    1
                }
            }
            lockCommand()
            simpleAction("disband") { Serverbound.Party.Disband }
        }
    }

    private fun CmdLiteralScope.inviteCommand() = literal("invite") {
        argument("user", ClientStringArgumentType.word(), chatAccounts) { user ->
            exec { ctx ->
                requireChat()
                ChatActions.inviteToParty(ctx.get(user))
                1
            }
        }
    }

    private fun CmdLiteralScope.acceptCommand() = literal("accept") {
        argument("user", ClientStringArgumentType.word(), inviters) { user ->
            exec { ctx ->
                val reference = ctx.get(user)
                val invite = PartyManager.invites.values.firstOrNull {
                    it.from.id == reference || it.from.name.equals(reference, true)
                } ?: throw CommandException(t("accept.unknown", reference))
                sendChatPacket(Serverbound.Party.Accept(invite.party))
                PartyManager.invites.remove(invite.party)
                1
            }
        }
    }

    private fun CmdLiteralScope.chatCommand() = literal("chat") {
        argument("message", StringArgumentType.greedyString()) { message ->
            exec { ctx ->
                GlobalSettingsClientChat.send(ChatSession.PARTY, ctx.get(message))
                1
            }
        }
    }

    private fun CmdLiteralScope.inventoryCommand() = literal("inv", aliases = listOf("inventory")) {
        requires { it.isIngame }
        argument("member", ClientStringArgumentType.word(), partyMembers) { member ->
            exec { ctx ->
                val target = requireMember(ctx.get(member))
                val inventory = PartyMemberStates[target.user.id]?.inventory
                    ?: throw CommandException(t("inv.unknown", target.user.name))
                PartyItems.show(target, inventory)
                1
            }
        }
    }

    private fun CmdLiteralScope.lockCommand() = literal("lock") {
        exec {
            val party = PartyManager.party ?: throw CommandException(t("notInParty"))
            sendChatPacket(Serverbound.Party.Lock(!party.locked))
            1
        }
    }

    private fun CmdLiteralScope.memberAction(name: String, packet: (PartyMember) -> Serverbound.Party) = literal(name) {
        argument("member", ClientStringArgumentType.word(), partyMembers) { member ->
            exec { ctx ->
                sendChatPacket(packet(requireMember(ctx.get(member))))
                1
            }
        }
    }

    private fun CmdLiteralScope.simpleAction(name: String, packet: () -> Serverbound.Party) = literal(name) {
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

}
