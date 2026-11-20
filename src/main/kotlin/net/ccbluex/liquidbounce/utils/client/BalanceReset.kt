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
 * Clears the session's [Timer.balance] after a setback while a module spending it runs, as a value of the module.
 */
class BalanceReset internal constructor(owner: ValueGroup, private val parent: EventListener) : EventListener {

    override fun parent() = parent

    private val resetOnFlag by owner.boolean("ResetBalanceOnFlag", true)

    @Suppress("unused")
    private val flagHandler = handler<PacketEvent> { event ->
        if (resetOnFlag && event.packet is ClientboundPlayerPositionPacket) {
            Timer.resetBalance()
        }
    }

}

fun <T> T.balanceReset() where T : ValueGroup, T : EventListener = BalanceReset(this, this)
