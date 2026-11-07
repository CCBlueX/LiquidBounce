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
package net.ccbluex.liquidbounce.utils.client

import net.ccbluex.liquidbounce.config.types.group.ValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket

/**
 * The limits a module spends the session's [Timer.balance] within.
 */
class BalanceValueGroup(private val parent: EventListener) : ValueGroup("Balance"), EventListener {

    override fun parent() = parent

    /**
     * Acceleration may spend the balance down to the start, which is below 0 for anticheats that let the client
     * run ahead. Holding back stops paying off at the end.
     */
    val range by intRange("Range", 0..20, -100..200, "ticks")
    private val resetOnFlag by boolean("ResetOnFlag", true)

    @Suppress("unused")
    private val flagHandler = handler<PacketEvent> { event ->
        if (resetOnFlag && event.packet is ClientboundPlayerPositionPacket) {
            Timer.resetBalance()
        }
    }

}
