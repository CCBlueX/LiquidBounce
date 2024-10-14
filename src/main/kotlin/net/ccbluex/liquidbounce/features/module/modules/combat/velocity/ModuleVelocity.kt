/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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

import net.ccbluex.liquidbounce.config.Choice
import net.ccbluex.liquidbounce.event.*
import net.ccbluex.liquidbounce.event.events.*
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.*
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityDexland
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityExemptGrim117
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityJumpReset
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

object ModuleVelocity : Module("Velocity", Category.COMBAT) {

    init {
        enableLock()
    }

    val modes = choices<Choice>("Mode", { VelocityModify }) {
        arrayOf(
            VelocityModify,
            VelocityWatchdog,
            VelocityStrafe,
            VelocityAAC442,
            VelocityExemptGrim117,
            VelocityDexland,
            VelocityJumpReset,
            VelocityIntave
        )
    }.apply { tagBy(this) }

    private val delay by intRange("Delay", 0..0, 0..40, "ticks")
    private val pauseOnFlag by int("PauseOnFlag", 0, 0..20, "ticks")

    private var pause = 0

    @Suppress("unused")
    private val countHandler = handler<GameTickEvent>(ignoreCondition = true) {
        if (pause > 0) {
            pause--
        }
    }

    @Suppress("unused")
    private val packetHandler = sequenceHandler<PacketEvent>(priority = 1) {
        val packet = it.packet

        if (!it.original) {
            return@sequenceHandler
        }

        if (packet is EntityVelocityUpdateS2CPacket && packet.entityId == player.id || packet is ExplosionS2CPacket) {
            // When delay is above 0, we will delay the velocity update
            if (delay.last > 0) {
                it.cancelEvent()

                delay.random().let { ticks ->
                    if (ticks > 0) {
                        val timeToWait = System.currentTimeMillis() + (ticks * 50L)

                        waitUntil { System.currentTimeMillis() >= timeToWait }
                    }
                }

                val packetEvent = PacketEvent(TransferOrigin.RECEIVE, packet, false)
                EventManager.callEvent(packetEvent)

                if (!packetEvent.isCancelled) {
                    (packet as Packet<ClientPlayPacketListener>).apply(network)
                }
            }
        } else if (packet is PlayerPositionLookS2CPacket) {
            pause = pauseOnFlag
        }
    }

    override fun handleEvents() = super.handleEvents() && pause == 0

}
