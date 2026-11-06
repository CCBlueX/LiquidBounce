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

package net.ccbluex.liquidbounce.features.inventoryPreset

import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot

/**
 * Represents an inventory preset defining what each slot should contain and how many items of a group to keep.
 *
 * The entries are ordered like [HotbarItemSlot.entries], so the first entry belongs to the off-hand slot and the
 * following ones to hotbar slot 0-8.
 */
class InventoryPreset(
    val itemRules: Array<List<FrontendSlotPreference>> = Array(HotbarItemSlot.entries.size) { emptyList() },
    val itemLimitRules: List<FrontendItemLimitRules> = emptyList(),
) {
    init {
        require(itemRules.size == HotbarItemSlot.entries.size) {
            "The preset must contain one entry per slot"
        }

        require(itemRules.flatMap { it }.none { it == FrontendSlotPreference.AnySlotPreference }) {
            "For an item to be Any, the list must be empty."
        }

        itemRules.forEach { preferences ->
            val ignoreCount = preferences.count { it == FrontendSlotPreference.IgnoreSlotPreference }
            require(ignoreCount == 0 || (ignoreCount == 1 && preferences.size == 1)) {
                "If you use IgnoreSlotPreference, it must be the ONLY element in the list"
            }
        }
    }
}
