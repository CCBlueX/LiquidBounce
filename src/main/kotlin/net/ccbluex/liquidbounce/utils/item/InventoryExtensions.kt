/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.config.Configurable

fun convertClientSlotToServerSlot(slot: Int): Int {
    return when (slot) {
        in 0..8 -> 36 + slot
        in 9..35 -> slot
        in 36..39 -> 39 - slot + 5
        40 -> 45
        else -> throw IllegalArgumentException()
    }
}

/**
 * Configurable to configure the dynamic rotation engine
 */
class InventoryConstraintsConfigurable : Configurable("InventoryConstraints") {
    internal var delay by intRange("Delay", 2..4, 0..20)
    internal val invOpen by boolean("InvOpen", false)
    internal val simulateInventory by boolean("SimulateInventory", true)
    internal val noMove by boolean("NoMove", false)
}
