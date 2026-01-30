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
package net.ccbluex.liquidbounce.features.module.modules.combat.velocity

import net.ccbluex.liquidbounce.event.EventManager
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.sequenceHandler
import net.ccbluex.liquidbounce.event.tickUntil
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityAAC442
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityBlocksMC
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityDexland
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityGrim2344
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityGrim2371
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityHylex
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityHypixel
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityIntave
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityJumpReset
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityLag
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityModify
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityReversal
import net.ccbluex.liquidbounce.features.module.modules.combat.velocity.mode.VelocityStrafe
import net.ccbluex.liquidbounce.utils.network.isLocalPlayerVelocity
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ClientGamePacketListener
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket

/**
 * Velocity module
 *
 * Modifies the amount of velocity you take.
 */

object ModuleVelocity : ClientModule("Velocity", ModuleCategories.COMBAT, aliases = listOf("AntiKnockBack")) {

    val modes = choices(
        "Mode", VelocityModify, arrayOf(
            // Generic modes
            VelocityModify,
            VelocityReversal,
            VelocityStrafe,
            VelocityJumpReset,
            VelocityLag,

            // Server modes
            VelocityHypixel,
            VelocityDexland,
            VelocityHylex,
            VelocityBlocksMC,

            // Anti cheat modes
            VelocityGrim2371,
            VelocityGrim2344,
            VelocityAAC442,
            VelocityIntave,
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

        if (packet.isLocalPlayerVelocity()) {
            // When delay is above 0, we will delay the velocity update
            if (delay.last > 0) {
                event.cancelEvent()

                delay.random().let { ticks ->
                    if (ticks > 0) {
                        val timeToWait = System.currentTimeMillis() + (ticks * 50L)

                        tickUntil { System.currentTimeMillis() >= timeToWait }
                    }
                }

                val packetEvent = PacketEvent(TransferOrigin.INCOMING, packet, false)
                EventManager.callEvent(packetEvent)

                if (!packetEvent.isCancelled) {
                    @Suppress("UNCHECKED_CAST")
                    (packet as Packet<ClientGamePacketListener>).handle(network)
                }
            }
        } else if (packet is ClientboundPlayerPositionPacket) {
            pause = pauseOnFlag
        }
    }

}
