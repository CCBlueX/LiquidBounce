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

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.config.types.group.ToggleableValueGroup
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.utils.client.SilentHotbar

/**
 * Uses an item by selecting its hotbar slot for real instead of through [SilentHotbar],
 * so the switch shows in the hotbar.
 */
internal class VisibleSlotSwitch(parent: EventListener) : ToggleableValueGroup(parent, "VisibleSwitch", false) {

    private val switchDelay by intRange("SwitchDelay", 1..2, 0..20, "ticks")

    /**
     * Selects [slot], runs [action] once the switch delay passed and selects the previous slot again
     * [switchBackTicks] ticks later, unless the player picked another slot in the meantime.
     */
    suspend fun use(slot: HotbarItemSlot, switchBackTicks: Int, action: () -> Unit) {
        val index = slot.hotbarIndex
        if (index == null) {
            action()
            return
        }

        val inventory = player.inventory
        val previous = inventory.selectedSlot
        inventory.selectedSlot = index
        try {
            waitTicks(switchDelay.random())
            action()
            waitTicks(switchBackTicks)
        } finally {
            if (inventory.selectedSlot == index) {
                inventory.selectedSlot = previous
            }
        }
    }

}
