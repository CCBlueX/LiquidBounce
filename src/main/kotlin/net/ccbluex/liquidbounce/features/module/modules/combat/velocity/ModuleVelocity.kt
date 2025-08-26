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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.*
import net.minecraft.network.listener.ClientPlayPacketListener
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket
import net.minecraft.network.packet.s2c.play.ExplosionS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

/**
 * Velocity module
 *
 * Modifies the amount of velocity you take.
 */

object ModuleVelocity : ClientModule("Velocity", Category.COMBAT, aliases = arrayOf("AntiKnockBack")) {

    init {
        enableLock()
    }

    val modes = choices(
        "Mode", VelocityModify, arrayOf(
            // Generic modes
            VelocityModify,
            VelocityReversal,
            VelocityStrafe,
            VelocityJumpReset,

            // Server modes
            VelocityHypixel,
            VelocityDexland,
            VelocityHylex,
            VelocityBlocksMC,

            // Anti cheat modes
            VelocityAAC442,
            VelocityExemptGrim117,
            VelocityIntave
        )
    ).apply(::tagBy)

    private val delay by intRange("Delay", 0..0, 0..40, "ticks")
    private val pauseOnFlag by int("PauseOnFlag", 0, 0..20, "ticks")

    internal var pause = 0

    @Suppress("unused")
    private val pauseHandler = handler<GameTickEvent> {
        if (pause > 0) {
            pause--
        }
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent>(priority = 1) { event ->
        val packet = event.packet

        if (!event.original || pause > 0) {
            return@sequenceHandler
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket) {
            // When delay is above 0, we will delay the velocity update
            if (delay.last > 0) {
                event.cancelEvent()

                delay.random().let { ticks ->
                    if (ticks > 0) {
                        val timeToWait = System.currentTimeMillis() + (ticks * 50L)

                        waitUntil { System.currentTimeMillis() >= timeToWait }
                    }
                }

                val packetEvent = PacketEvent(TransferOrigin.INCOMING, packet, false)
                EventManager.callEvent(packetEvent)

                if (!packetEvent.isCancelled) {
                    (packet as Packet<ClientPlayPacketListener>).apply(network)
                }
            }
        } else if (packet is PlayerPositionLookS2CPacket) {
            pause = pauseOnFlag
        }
    }

}
