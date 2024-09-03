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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.notification
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket

object ModuleFlagCheck: Module("FlagCheck", Category.MISC) {
    private var resetFlagCounterTicks by int("ResetCounterTicks", 600, 100..1000)

    private var flagCount = 0
    private fun clearFlags() {
        flagCount = 0
    }

    private val packetHandler = handler<PacketEvent> { event ->
        when (event.packet) {
            is PlayerPositionLookS2CPacket -> {
                if (player.age > 25) {
                    flagCount++
                    notification("FlagCheck", "Detected LagBack (${flagCount}x)", NotificationEvent.Severity.INFO)
                }
            }

            is DisconnectS2CPacket -> {
                clearFlags()
            }
        }

        val invalidReason = mutableListOf<String>()
        if (player.health <= 0.0f) invalidReason.add("health")
        if (player.hungerManager.foodLevel <= 0) invalidReason.add("hunger")

        if (invalidReason.isNotEmpty()) {
            flagCount++
            val reasonString = invalidReason.joinToString()
            notification("FlagCheck", "Detected invalid $reasonString (${flagCount}x)", NotificationEvent.Severity.INFO)
            invalidReason.clear()
        }

        if (player.age % (resetFlagCounterTicks * 20) == 0) {
            clearFlags()
        }
    }
}
