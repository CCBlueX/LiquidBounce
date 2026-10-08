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
import net.ccbluex.liquidbounce.event.events.ClientChatStateChange
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.chat.packet.C2SLocationPacket
import net.ccbluex.liquidbounce.features.chat.packet.C2SSightingsPacket
import net.ccbluex.liquidbounce.features.chat.packet.LocationWorld
import net.ccbluex.liquidbounce.features.chat.packet.PartyMember
import net.ccbluex.liquidbounce.features.chat.packet.PartyPlayer
import net.ccbluex.liquidbounce.features.global.GlobalSettingsClientChat
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.text.isSensitiveAddress
import net.minecraft.network.protocol.game.ClientboundLoginPacket
import net.minecraft.network.protocol.game.ClientboundRespawnPacket
import net.minecraft.network.protocol.game.ClientboundSetTimePacket

private const val SPAWN_DEBOUNCE = 1000L
private const val DRIFT_TICKS = 40L
private const val DRIFT_RESEND_INTERVAL = 10_000L
private const val AGE_STATE_RESEND_INTERVAL = 2000L
private const val SIGHTINGS_INTERVAL = 1000L

// The server forgets entity sightings after 10 seconds
private const val SIGHTINGS_REFRESH = 5000L

object LocationReporter : EventListener {

    private val ageTracker = WorldAgeTracker()

    @Volatile
    private var seed = 0L

    @Volatile
    private var pendingAt = 0L

    private var sent: C2SLocationPacket? = null
    private var sentAt = 0L

    private var sightings: C2SSightingsPacket? = null
    private var sightingsSentAt = 0L
    private var sightingsCheckedAt = 0L

    private val client
        get() = GlobalSettingsClientChat.chatClient

    private fun schedule() {
        pendingAt = System.currentTimeMillis() + SPAWN_DEBOUNCE
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        when (val packet = event.packet) {
            is ClientboundLoginPacket -> {
                seed = packet.commonPlayerSpawnInfo().seed()
                ageTracker.reset()
                schedule()
            }

            is ClientboundRespawnPacket -> {
                seed = packet.commonPlayerSpawnInfo().seed()
                ageTracker.reset()
                schedule()
            }

            is ClientboundSetTimePacket -> ageTracker.update(packet.gameTime(), System.currentTimeMillis())
        }
    }

    @Suppress("unused")
    private val worldHandler = handler<WorldChangeEvent> {
        schedule()
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        seed = 0L
        ageTracker.reset()
        schedule()
    }

    @Suppress("unused")
    private val stateHandler = handler<ClientChatStateChange> { event ->
        when (event.state) {
            ClientChatStateChange.State.LOGGED_IN -> schedule()
            ClientChatStateChange.State.DISCONNECTED -> {
                sent = null
                sightings = null
            }

            else -> {}
        }
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!client.isLoggedIn || !client.isModern) {
            return@handler
        }

        val now = System.currentTimeMillis()
        if (pendingAt != 0L && now >= pendingAt || shouldResend(now)) {
            pendingAt = 0L
            send(current(now), now)
        }

        if (now - sightingsCheckedAt >= SIGHTINGS_INTERVAL) {
            sightingsCheckedAt = now
            reportSightings(now)
        }
    }

    private fun current(now: Long): C2SLocationPacket {
        val level = mc.level ?: return C2SLocationPacket(null, null, null)
        // a LiquidProxy route works as its owner's subscription, so it never leaves the client
        val server = mc.currentServer?.ip?.takeUnless { mc.hasSingleplayerServer() || it.isSensitiveAddress() }

        return C2SLocationPacket(
            server,
            LocationWorld(level.dimension().identifier().toString(), seed, ageTracker.current(now)),
            mc.player?.let { PartyPlayer(it.uuid, it.gameProfile.name) },
        )
    }

    private fun shouldResend(now: Long): Boolean {
        val last = sent ?: return false
        val lastWorld = last.world ?: return false
        val lastAge = lastWorld.age
        val age = ageTracker.current(now)

        return if ((lastAge == null) != (age == null)) {
            now - sentAt >= AGE_STATE_RESEND_INTERVAL
        } else if (lastAge != null) {
            now - sentAt >= DRIFT_RESEND_INTERVAL && (ageTracker.drift(lastAge, sentAt, now) ?: 0) > DRIFT_TICKS
        } else {
            false
        }
    }

    private fun send(packet: C2SLocationPacket, now: Long) {
        sent = packet
        sentAt = now
        client.sendPacket(packet)
    }

    private fun reportSightings(now: Long) {
        val members = PartyManager.others
        val packet = C2SSightingsPacket(
            members.filter(::isLoaded).map { it.user.id }.sorted(),
            members.filter(::isListed).map { it.user.id }.sorted(),
        )

        val refresh = packet.entities.isNotEmpty() && now - sightingsSentAt >= SIGHTINGS_REFRESH
        if (packet != sightings || refresh) {
            sightings = packet
            sightingsSentAt = now
            client.sendPacket(packet)
        }
    }

    private fun isLoaded(member: PartyMember): Boolean {
        val player = member.player ?: return false
        val level = mc.level ?: return false
        return level.getPlayerByUUID(player.uuid) != null
            || level.players().any { it.gameProfile.name.equals(player.name, true) }
    }

    private fun isListed(member: PartyMember): Boolean {
        val player = member.player ?: return false
        val connection = mc.connection ?: return false
        return connection.getPlayerInfo(player.uuid) != null || connection.getPlayerInfoIgnoreCase(player.name) != null
    }

    override fun parent() = GlobalSettingsClientChat

}
