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
package net.ccbluex.liquidbounce.features.module.modules.movement.noslow.modes.shared

import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.event.EventState
import net.ccbluex.liquidbounce.event.events.PlayerNetworkMovementTickEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.InteractionTracker.untracked
import net.ccbluex.liquidbounce.utils.client.sendHeldItemChange

/**
 * @anticheat Grim
 * @anticheatVersion 2.3.64
 */
internal class NoSlowSharedGrim2364MC18(override val parent: ModeValueGroup<*>) : Mode("Grim2364-1.8") {

    @Suppress("unused")
    private val onNetworkTick = handler<PlayerNetworkMovementTickEvent> { event ->
        if (player.isUsingItem && event.state == EventState.PRE) {
            // Switch slots so grim exempts noslow...
            // Introduced with https://github.com/GrimAnticheat/Grim/issues/874
            untracked {
                val slot = player.inventory.selectedSlot
                network.sendHeldItemChange(slot % 8 + 1)
                network.sendHeldItemChange(slot % 7 + 2)
                network.sendHeldItemChange(slot)
            }
        }
    }

}
