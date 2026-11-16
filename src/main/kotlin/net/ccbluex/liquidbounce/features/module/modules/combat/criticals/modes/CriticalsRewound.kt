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

package net.ccbluex.liquidbounce.features.module.modules.combat.criticals.modes

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.events.TransferOrigin
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.modules.combat.criticals.ModuleCriticals
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention.READ_FINAL_STATE
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket

/**
 * @anticheat NoCheatPlus
 * @testedOn rewound.cc
 */
object CriticalsRewound : Mode("Rewound") {

    override val parent: ModeValueGroup<Mode>
        get() = ModuleCriticals.modes

    private var ticks = 0

    override fun enable() {
        ticks = 0
        super.enable()
    }

    @Suppress("unused")
    private val packetHandler = handler<PacketEvent>(priority = READ_FINAL_STATE) { event ->
        if (event.origin != TransferOrigin.OUTGOING) {
            return@handler
        }
        val packet = event.packet as? ServerboundMovePlayerPacket ?: return@handler

        ticks = if (player.onGround() && player.y <= player.yo) ticks + 1 else 0
        when (ticks) {
            0, 1 -> return@handler
            2 -> if (packet.hasPos) packet.y += 0.003
            else -> if (packet.hasPos) packet.y += (0.001 - 1e-10 * ticks).coerceAtLeast(0.0)
        }
        packet.onGround = false
    }
}
