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

package net.ccbluex.liquidbounce.features.module.modules.movement.fly.modes.polar

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly
import net.ccbluex.liquidbounce.features.module.modules.movement.fly.ModuleFly.modes
import net.ccbluex.liquidbounce.utils.client.handlePacket
import net.minecraft.network.protocol.common.ClientboundPingPacket
import net.minecraft.network.protocol.game.ClientboundDamageEventPacket
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket

/**
 * @anticheat Hycraft (Polar)
 * @anticheat Version 15.05.2024
 * @testedOn mc.hycraft.us
 *
 * @note Tested in Bedwars, Skywars. Pretty much flagless
 */
internal object FlyHycraftDamage : Mode("HycraftDamage") {

    override val parent: ModeValueGroup<*>
        get() = modes

    private var damageTaken = false
    private var release = false
    private var ticks = 0

    override fun enable() {
        ticks = 0
        damageTaken = false
        release = false
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        waitTicks(1)

        if (ticks > 0) {
            ticks--
        }
    }

    /**
     * Used to works on different servers as well but now only Hycraft
     */
    @Suppress("unused")
    private val packetHandler = handler<BlinkPacketEvent> { event ->
        val packet = event.packet

        if (event.origin != TransferOrigin.INCOMING) {
            return@handler
        }

        event.action = when (packet) {
            is ClientboundDamageEventPacket if packet.entityId == player.id && ticks <= 0 -> {
                damageTaken = true
                ticks = 40
                handlePacket(packet)
                BlinkManager.Action.QUEUE
            }

            is ClientboundSetEntityMotionPacket if packet.id == player.id && damageTaken -> {
                damageTaken = false
                release = true
                handlePacket(packet)
                BlinkManager.Action.QUEUE
            }

            is ClientboundPingPacket -> {
                if (ticks <= 0) {
                    if (release) {
                        ModuleFly.enabled = false
                    }
                    return@handler
                }

                ticks--
                BlinkManager.Action.QUEUE
            }

            // Prevent [PacketQueueManager] from flushing queued packets
            else -> BlinkManager.Action.PASS
        }

    }

}
