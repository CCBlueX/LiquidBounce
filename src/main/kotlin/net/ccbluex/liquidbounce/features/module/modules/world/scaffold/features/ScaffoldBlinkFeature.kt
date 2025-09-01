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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.QueuePacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.ccbluex.liquidbounce.utils.client.PacketQueueManager
import net.minecraft.network.packet.Packet
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket

object ScaffoldBlinkFeature : ToggleableConfigurable(ModuleScaffold, "Blink", false) {

    private val time by intRange("Time", 50..250, 0..3000, "ms")
    private val flushOn by multiEnumChoice<FlushOn>("FlushOn")

    private var pulseTime = 0L
    private val pulseTimer = Chronometer()

    fun onBlockPlacement() {
        pulseTime = time.random().toLong()
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<QueuePacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        if (pulseTimer.hasElapsed(pulseTime) || flushOn.any { it.cond(event.packet) }) {
            pulseTimer.reset()
            return@handler
        }

        if (!player.isOnGround || !pulseTimer.hasElapsed(pulseTime)) {
            event.action = PacketQueueManager.Action.QUEUE
        }
    }

    @Suppress("unused")
    private enum class FlushOn(
        override val choiceName: String,
        val cond: (packet: Packet<*>?) -> Boolean
    ) : NamedChoice {
        PLACE("Place", { packet ->
            packet is PlayerInteractBlockC2SPacket
        }),
        TOWERING("Towering", {
            ModuleScaffold.isTowering
        }),
        SNEAKING("Sneaking", {
            player.isSneaking
        }),
        NOT_SNEAKING("NotSneaking", {
            !player.isSneaking
        }),
        ON_GROUND("OnGround", {
            player.isOnGround
        }),
        IN_AIR("InAir", {
            !player.isOnGround
        })
    }

}
