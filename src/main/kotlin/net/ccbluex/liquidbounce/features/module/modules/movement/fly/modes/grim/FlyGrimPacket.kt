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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.grim

import net.ccbluex.liquidbounce.config.types.nesting.Choice
import net.ccbluex.liquidbounce.config.types.nesting.ChoiceConfigurable
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager.Action
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.59 (works on latest)
 * @testedOn eu.loyisa.cn / mc.loyisa.cn
 */
object FlyGrimPacket: Choice("GrimPacket") {
    override val parent: ChoiceConfigurable<*>
        get() = modes

    private val autoLag by boolean("AutoLagInAir", false)
    private val airTick by int("AirTick", 3, 0..12, "ticks")

    private var delay = false
    private var start = false

    override fun enable() {
        start = false
        delay = false
    }

    private fun sendGrimPacket() {
        network.send(
            ServerboundPlayerCommandPacket(
                player,
                ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
            )
        )
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<QueuePacketEvent> { event ->
        val packet = event.packet
        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            delay = true
        }

        if (delay) {
            event.action = when (packet) {
                is ClientboundPingPacket -> Action.QUEUE
                is ClientboundPlayerPositionPacket -> Action.FLUSH
                else -> Action.PASS
            }
        }
    }

    @Suppress("unused")
    private val motionHandler = handler<PlayerNetworkMovementTickEvent> { event ->
        if (event.state == EventState.POST) {
            if (start) {
                sendGrimPacket()
            }
            return@handler
        }

        if (start) {
            return@handler
        }

        if (autoLag && player.airTicks >= airTick || delay) {
            start = true
            sendGrimPacket()
        }
    }
}

