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

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager.Action
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.entity.airTicks
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.73-b7a719d
 *   https://modrinth.com/plugin/grimac/version/Eq05CMZ9
 *   January 15, 2026
 * @testedOn test.ccbluex.net
 */
object FlyGrim2373Jan15 : Mode("Grim2373Jan15") {

    override val parent: ModeValueGroup<*>
        get() = modes

    private val autoLag by boolean("AutoLagInAir", true)
    private val airTick by int("AirTick", 3, 0..12, "ticks")

    private var isStarted = false
    private var shouldDelay = false

    override fun disable() {
        isStarted = false
        shouldDelay = false
        super.disable()
    }

    @Suppress("unused")
    private val queuePacketHandler = handler<BlinkPacketEvent> { event ->
        val packet = event.packet
        if (packet is ClientboundSetEntityMotionPacket && packet.id == player.id) {
            shouldDelay = true
        }

        if (shouldDelay) {
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
            if (isStarted) {
                sendFallFlying()
            }
            return@handler
        }

        if (isStarted) {
            return@handler
        }

        if (autoLag && player.airTicks >= airTick || shouldDelay) {
            isStarted = true
            sendFallFlying()
        }
    }

    private fun sendFallFlying() {
        val packet = ServerboundPlayerCommandPacket(
            player,
            ServerboundPlayerCommandPacket.Action.START_FALL_FLYING
        )

        network.send(packet)
    }

}

