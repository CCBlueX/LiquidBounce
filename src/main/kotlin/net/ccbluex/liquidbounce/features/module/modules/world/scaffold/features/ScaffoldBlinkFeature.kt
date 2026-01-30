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
package net.ccbluex.liquidbounce.features.module.modules.world.scaffold.features

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.event.events.BlinkPacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.blink.BlinkManager
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.utils.client.Chronometer
import net.minecraft.network.protocol.Packet
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket

object ScaffoldBlinkFeature : ToggleableValueGroup(ModuleScaffold, "Blink", false) {

    private val time by intRange("Time", 50..250, 0..3000, "ms")
    private val flushOn by multiEnumChoice<FlushOn>("FlushOn")

    private var pulseTime = 0L
    private val pulseTimer = Chronometer()

    fun onBlockPlacement() {
        pulseTime = time.random().toLong()
    }

    @Suppress("unused")
    private val fakeLagHandler = handler<BlinkPacketEvent> { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }

        if (pulseTimer.hasElapsed(pulseTime) || flushOn.any { it.cond(event.packet) }) {
            pulseTimer.reset()
            return@handler
        }

        if (!player.onGround() || !pulseTimer.hasElapsed(pulseTime)) {
            event.action = BlinkManager.Action.QUEUE
        }
    }

    @Suppress("unused")
    private enum class FlushOn(
        override val tag: String,
        val cond: (packet: Packet<*>?) -> Boolean
    ) : Tagged {
        PLACE("Place", { packet ->
            packet is ServerboundUseItemOnPacket
        }),
        TOWERING("Towering", {
            ModuleScaffold.isTowering
        }),
        SNEAKING("Sneaking", {
            player.isShiftKeyDown
        }),
        NOT_SNEAKING("NotSneaking", {
            !player.isShiftKeyDown
        }),
        ON_GROUND("OnGround", {
            player.onGround()
        }),
        IN_AIR("InAir", {
            !player.onGround()
        })
    }

}
