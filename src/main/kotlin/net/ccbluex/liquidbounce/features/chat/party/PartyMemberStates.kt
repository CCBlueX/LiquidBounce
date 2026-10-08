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

import com.google.gson.JsonParser
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.floatOrNull
import net.ccbluex.axochat.party.PartyInfo
import net.ccbluex.axochat.party.Position
import net.ccbluex.axochat.protocol.Clientbound
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.ClientChatPacketEvent
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.PartyUpdateEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import java.util.concurrent.ConcurrentHashMap

private const val PUBLISH_INTERVAL = 1000L

// beyond any health a player can have
private const val MAX_AMOUNT = 1024f

/**
 * Members send whatever they like.
 */
fun JsonObject.amount(key: String): Float? = (this[key] as? JsonPrimitive)?.takeUnless { it.isString }?.floatOrNull
    ?.takeIf { it.isFinite() }?.coerceIn(0f, MAX_AMOUNT)

object PartyMemberStates : EventListener {

    class MemberState {
        @Volatile
        var position: Position? = null

        @Volatile
        var positionAt = 0L

        @Volatile
        var status: JsonObject? = null

        @Volatile
        var inventory: JsonObject? = null
    }

    data class MemberView(val position: Position?, val status: com.google.gson.JsonObject?)

    private val states = ConcurrentHashMap<String, MemberState>()

    private var publishedAt = 0L

    operator fun get(memberId: String): MemberState? = states[memberId]

    // the theme reads Gson's JSON
    fun views(): Map<String, MemberView> = states.mapValues { (_, state) ->
        MemberView(state.position, state.status?.let { JsonParser.parseString(it.toString()).asJsonObject })
    }

    fun publish(party: PartyInfo?) {
        publishedAt = System.currentTimeMillis()
        EventManager.callEvent(PartyUpdateEvent(party, views()))
    }

    @Suppress("unused")
    private val packetHandler = handler<ClientChatPacketEvent> { event ->
        when (val packet = event.packet) {
            is Clientbound.PartyMemberState -> {
                val state = states.computeIfAbsent(packet.member) { MemberState() }
                packet.position?.let {
                    state.position = it
                    state.positionAt = System.currentTimeMillis()
                }
                packet.status?.let { state.status = it }
                packet.inventory?.let { state.inventory = it }

                // position alone changes several times a second
                if (packet.status != null || System.currentTimeMillis() - publishedAt >= PUBLISH_INTERVAL) {
                    publish(PartyManager.party)
                }
            }

            is Clientbound.Party -> {
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
