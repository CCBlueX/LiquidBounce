/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.misc.antibot.modes

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.misc.antibot.ModuleAntiBot
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.network.packet.s2c.play.PlayerListS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerRemoveS2CPacket
import java.util.*

/**
 * Intave anti-cheat, Heavy bot type, their best bot type.
 *
 * Tested on: gamster.org and a private server with latest Intave as of 7/28/2022.
 */
object IntaveHeavyAntiBotMode : Choice("IntaveHeavy"), ModuleAntiBot.IAntiBotMode {
    override val parent: ChoiceConfigurable<*>
        get() = ModuleAntiBot.modes

    private val suspectList = hashMapOf<UUID, SuspectInfo>()
    private val botList = hashSetOf<UUID>()

    /**
     * ## Ping logic:
     * When you join a server, you always have 0 ping at start. However, if you are on a game like Practice and
     * come back from a duel, you will keep your ping.
     *
     * As for Matrix and Intave, they defy this logic. Intave though decides instead to fix it by sending
     * [PlayerListS2CPacket.Action.UPDATE_LATENCY] to make up for the ping issue. Unfortunately, that leads to
     * even more problems.
     */
    val packetHandler = handler<PacketEvent> {
        when (val packet = it.packet) {
            is PlayerListS2CPacket -> handleListPacket(packet)
            is PlayerRemoveS2CPacket -> handlePlayerRemove(packet)
        }
    }

    private fun handleListPacket(packet: PlayerListS2CPacket) {
        when (packet.actions.first()) {
            PlayerListS2CPacket.Action.ADD_PLAYER -> handlePlayerListAddPlayers(packet.entries)
            PlayerListS2CPacket.Action.UPDATE_LATENCY -> handlePlayerListUpdateLatency(packet.entries)
            else -> {}
        }
    }

    /**
     * When a player is removed from the game, this function forgets about them.
     */
    private fun handlePlayerRemove(packet: PlayerRemoveS2CPacket) {
        for (id in packet.profileIds) {
            suspectList.remove(id)
            botList.remove(id)
        }
    }

    /**
     * On 7/28/2022 and earlier, Intave sent a player list update latency packet and instead of updating every player's
     * ping, they update only their bot's ping. Another flaw they had back then, don't know if they fixed it now,
     * so I'm having this as a comment instead. Minimizes false positives by a large amount.
     */
    private const val INTAVE_BUG_FIX: Boolean = false

    private fun handlePlayerListUpdateLatency(entries: MutableList<PlayerListS2CPacket.Entry>) {
        if (INTAVE_BUG_FIX && entries.size > 1) {
            return
        }

        for (entry in entries) {
            if (!suspectList.containsKey(entry.profileId)) {
                continue
            }

            val pingSinceJoin = suspectList.getValue(entry.profileId).latency

            val deltaPing = pingSinceJoin - entry.latency
            val deltaMS = System.currentTimeMillis() - suspectList.getValue(entry.profileId).timestamp

            // Intave instantly sends this packet, but some servers might lag, so it might be delayed,
            // that's why the difference limit is 15 MS. The less the value, the lower the chances of producing
            // false positives, even though it's highly unlikely.
            if (deltaPing == pingSinceJoin && deltaMS <= 15) {
                botList.add(entry.profileId)
            }

            suspectList.remove(entry.profileId)
        }
    }

    private fun handlePlayerListAddPlayers(entries: MutableList<PlayerListS2CPacket.Entry>) {
        for (entry in entries) {
            val profile = entry.profile ?: continue

            if (entry.latency < 2 || ModuleAntiBot.isGameProfileUnique(profile)) {
                continue
            }

            suspectList[entry.profileId] = SuspectInfo(entry.latency, System.currentTimeMillis())
        }
    }

    override fun isBot(entity: PlayerEntity): Boolean {
        return botList.contains(entity.uuid)
    }

    override fun reset() {
        suspectList.clear()
        botList.clear()
    }

    @JvmRecord
    data class SuspectInfo(val latency: Int, val timestamp: Long)

}
