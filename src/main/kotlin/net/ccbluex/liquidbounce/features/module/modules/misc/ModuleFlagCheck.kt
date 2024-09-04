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

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Listenable
import net.ccbluex.liquidbounce.event.events.NotificationEvent
import net.ccbluex.liquidbounce.event.events.PacketEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.client.chat
import net.ccbluex.liquidbounce.utils.client.notification
import net.ccbluex.liquidbounce.utils.client.regular
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * alerts you about flags
 */

object ModuleFlagCheck: Module("FlagCheck", Category.MISC) {
    private var showInChat by boolean("ShowInChat", true)

    private var flagCount = 0
    private fun clearFlags() {
        flagCount = 0
    }

    private val packetHandler = handler<PacketEvent> { event ->
        when (event.packet) {
            is PlayerPositionLookS2CPacket -> {
                if (player.age > 25) {
                    flagCount++
                    if (!showInChat) {
                        notification("FlagCheck", "Detected LagBack (${flagCount}x)", NotificationEvent.Severity.INFO)
                    } else {
                        chat("§cDetected LagBack §7(${flagCount}x)")
                    }
                }
            }

            is DisconnectS2CPacket -> {
                clearFlags()
            }
        }

        val invalidReason = mutableListOf<String>()
        if (player.health <= 0.0f) invalidReason.add("Health")
        if (player.hungerManager.foodLevel <= 0) invalidReason.add("Hunger")

        if (invalidReason.isNotEmpty()) {
            flagCount++
            val reasonString = invalidReason.joinToString()
            invalidReason.clear()
            if (!showInChat) {
                notification(
                    "FlagCheck",
                    "Detected Invalid $reasonString (${flagCount}x)",
                    NotificationEvent.Severity.INFO
                )
            } else {
                chat("§cDetected Invalid $reasonString §7(${flagCount}x)")
            }
        }
    }

    private class ResetFlags(parent: Listenable?) : ToggleableConfigurable(parent, "ResetFlags", true) {
        private var resetTicks by int("ResetTicks", 600, 100..1000)

        private val repeatable = repeatable {
            if (player.age % (resetTicks * 20) == 0) {
                clearFlags()
            }
        }
    }

    init {
        tree(ResetFlags(this))
    }
}
