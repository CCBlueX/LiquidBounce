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

import com.google.gson.JsonObject
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.PartyPosition
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyMemberStatePacket
import net.ccbluex.liquidbounce.features.chat.packet.S2CPartyPacket
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import java.util.concurrent.ConcurrentHashMap

object PartyMemberStates : EventListener {

    class MemberState {
        @Volatile
        var position: PartyPosition? = null

        @Volatile
        var positionAt = 0L

        @Volatile
        var status: JsonObject? = null

        @Volatile
        var inventory: JsonObject? = null
    }

    private val states = ConcurrentHashMap<String, MemberState>()

    operator fun get(memberId: String): MemberState? = states[memberId]

    val all: Map<String, MemberState>
        get() = states

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is S2CPartyMemberStatePacket -> {
                val state = states.computeIfAbsent(packet.member) { MemberState() }
                packet.position?.let {
                    state.position = it
                    state.positionAt = System.currentTimeMillis()
                }
                packet.status?.let { state.status = it }
                packet.inventory?.let { state.inventory = it }
            }

            is S2CPartyPacket -> {
                val members = packet.party?.members.orEmpty().mapTo(hashSetOf()) { it.user.id }
                states.keys.retainAll(members)
            }

            else -> {}
        }
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        if (event.state == ClientChatStateChange.State.DISCONNECTED) {
            states.clear()
        }
    }

    override fun parent() = GlobalSettingsClientChat

}
