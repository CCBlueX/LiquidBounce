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
package net.ccbluex.liquidbounce.utils.network

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.DisconnectEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.events.WorldChangeEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket
import kotlin.math.sqrt

/**
 * Packet traffic of the current connection and the speed of the positions the client sends.
 *
 * Outgoing packets are counted where [net.minecraft.network.Connection] sends them, so
 * [sendPacketSilently] packets are included, before ViaFabricPlus translates them to the server version.
 */
object NetworkTelemetry : EventListener {

    private const val WINDOW_MS = 1_000L

    data class Snapshot(
        val setbacks: Int,
        val packetsOut: Long,
        val movePacketsPerSecond: Int,
        val speedBps: Double,
    )

    private val lock = Any()
    private val movePacketTimes = ArrayDeque<Long>()
    private val positionTrail = ArrayDeque<PositionSample>()
    private var setbacks = 0
    private var packetsOut = 0L
    private var tracking = false

    private data class PositionSample(val time: Long, val x: Double, val z: Double)

    val snapshot: Snapshot
        get() = synchronized(lock) {
            prune(System.currentTimeMillis())
            Snapshot(setbacks, packetsOut, movePacketTimes.size, horizontalDistancePerSecond())
        }

    /**
     * Every packet that goes out, including the ones of [sendPacketSilently].
     */
    fun onOutgoing(packet: Packet<*>) {
        val now = System.currentTimeMillis()
        synchronized(lock) {
            packetsOut++
            if (packet !is ServerboundMovePlayerPacket) {
                return
            }

            movePacketTimes.addLast(now)
            if (packet.hasPosition()) {
                positionTrail.addLast(PositionSample(now, packet.getX(0.0), packet.getZ(0.0)))
            }
            prune(now)
        }
    }

    private fun prune(now: Long) {
        while (movePacketTimes.isNotEmpty() && now - movePacketTimes.first() >= WINDOW_MS) {
            movePacketTimes.removeFirst()
        }

        // The first sample stays as the start of the path, even when it is older than the window
        while (positionTrail.size > 1 && now - positionTrail[1].time >= WINDOW_MS) {
            positionTrail.removeFirst()
        }
    }

    private fun horizontalDistancePerSecond(): Double {
        var distance = 0.0
        var previous: PositionSample? = null
        for (sample in positionTrail) {
            previous?.let {
                distance += sqrt((sample.x - it.x) * (sample.x - it.x) + (sample.z - it.z) * (sample.z - it.z))
            }
            previous = sample
        }
        return distance
    }

    private fun reset() = synchronized(lock) {
        setbacks = 0
        packetsOut = 0L
        movePacketTimes.clear()
        positionTrail.clear()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent> { event ->
        if (event.origin == TransferOrigin.INCOMING && event.packet is ClientboundPlayerPositionPacket) {
            synchronized(lock) { setbacks++ }
        }
    }

    @Suppress("unused")
    private val worldChangeHandler = handler<WorldChangeEvent> { event ->
        when {
            event.world == null -> {
                tracking = false
                reset()
            }

            // Dimension changes keep the session, joining starts it
            !tracking -> {
                tracking = true
                reset()
            }
        }
    }

    @Suppress("unused")
    private val disconnectHandler = handler<DisconnectEvent> {
        tracking = false
        reset()
    }

}
