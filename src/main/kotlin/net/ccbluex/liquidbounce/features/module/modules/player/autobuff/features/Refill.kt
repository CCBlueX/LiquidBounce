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
import net.ccbluex.liquidbounce.event.tickHandler
import net.ccbluex.liquidbounce.event.waitTicks
import net.ccbluex.liquidbounce.features.module.modules.player.autobuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.SlotGroup
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.InventoryScreen

object Refill : ToggleableConfigurable(ModuleAutoBuff, "Refill", true) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    private object AutoOpenInventory : ToggleableConfigurable(this, "AutoOpenInventory", true)
    private object AutoCloseInventory : ToggleableConfigurable(this, "AutoCloseInventory", true) {
        val wait by intRange("Wait", 1..2, 1..20, "ticks")
    }

    // Variable to prevent AutoClose when no items were refilled.
    private var stealAnythingInTheCurrentScreenSession = false;

    init {
        tree(AutoOpenInventory)
        tree(AutoCloseInventory)
    }

    @Suppress("unused")
    private val tickHandler = tickHandler {
        // Find valid items in the hotbar
        val validItemsInHotbar = findValidItems(Slots.Hotbar)
        // Find valid items in the inventory
        val validItemsInInventory = findValidItems(Slots.Inventory)

        if (mc.currentScreen != null) {
            if (AutoCloseInventory.enabled &&
                mc.currentScreen is InventoryScreen &&
                stealAnythingInTheCurrentScreenSession &&
                (validItemsInInventory.isEmpty() || !findEmptyHotbarSlot())
            ) {
                waitTicks(AutoCloseInventory.wait.random())

                // again, the current screen might change while the module is waiting
                if (mc.currentScreen != null) {
                    player.closeHandledScreen()
                }
            }
        } else {
            if (validItemsInHotbar.isEmpty() && validItemsInInventory.isNotEmpty() &&
                findEmptyHotbarSlot()
            ) {
                waitTicks(1)

                // again, the current screen might change while the module is waiting
                if (mc.currentScreen == null) {
                    mc.setScreen(InventoryScreen(player))
                }
            }

            // Reset variable due to inventory was closed.
            stealAnythingInTheCurrentScreenSession = false
        }
    }

    fun execute(event: ScheduleInventoryActionEvent) {
        // Check if we have space in the hotbar
        if (!findEmptyHotbarSlot()) {
            return
        }

        // Find valid items in the inventory
        val validItems = findValidItems(Slots.Inventory)

        // Check if we have any valid items
        if (validItems.isEmpty()) {
            return
        }

        // Sort the items by the order of the features
        for (slot in validItems) {
            event.schedule(
                inventoryConstraints, InventoryAction.Click.performQuickMove(slot = slot),
                Priority.IMPORTANT_FOR_USAGE_1
            )

            stealAnythingInTheCurrentScreenSession = true
        }
    }

    private fun findEmptyHotbarSlot(): Boolean {
        return Slots.OffhandWithHotbar.findSlot { it.isEmpty } != null
    }

    private fun findValidItems(container: SlotGroup<out ItemSlot>): List<ItemSlot> {
        val validFeatures = ModuleAutoBuff.activeFeatures

        return container.filter {
            val itemStack = it.itemStack
            validFeatures.any { f -> f.isValidItem(itemStack, false) }
        }
    }
}
