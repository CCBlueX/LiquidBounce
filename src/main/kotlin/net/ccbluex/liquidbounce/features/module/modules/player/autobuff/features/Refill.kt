/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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
 *
 *
 */

package net.ccbluex.liquidbounce.features.module.modules.player.autobuff.features

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff.features
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.Priority

object Refill : ToggleableConfigurable(ModuleAutoBuff, "Refill", true) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    fun execute(event: ScheduleInventoryActionEvent) {
        // Check if we have space in the hotbar
        if (!findEmptyHotbarSlot()) {
            return
        }

        val validFeatures = features.filter { it.enabled }

        // Find valid items in the inventory
        val validItems = Slots.Inventory.filter {
            val itemStack = it.itemStack
            validFeatures.any { f -> f.isValidItem(itemStack, false) }
        }

        // Check if we have any valid items
        if (validItems.isEmpty()) {
            return
        }

        // Sort the items by the order of the features
        for (slot in validItems) {
            event.schedule(
                inventoryConstraints, ClickInventoryAction.performQuickMove(slot = slot),
                Priority.IMPORTANT_FOR_USAGE_1
            )
        }
    }

    private fun findEmptyHotbarSlot(): Boolean {
        return Slots.Hotbar.findSlot { it.isNothing() } != null
    }

}
