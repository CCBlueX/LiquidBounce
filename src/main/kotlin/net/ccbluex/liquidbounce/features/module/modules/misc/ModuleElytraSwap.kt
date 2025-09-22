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
 */
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.isChestArmor
import net.ccbluex.liquidbounce.utils.kotlin.EventPriorityConvention
import net.minecraft.item.ItemStack
import net.minecraft.item.Items

/**
 * ModuleElytraSwap
 *
 * Allows you to quickly replace your chestplate with an elytra and vice versa.
 *
 * @author sqlerrorthing
 * @since 2/13/2025
 **/
object ModuleElytraSwap : ClientModule(
    "ElytraSwap",
    Category.PLAYER,
    aliases = listOf("ChestSwap"),
    disableOnQuit = true
) {

    private val constraints = tree(PlayerInventoryConstraints())

    private val slotsToSearch = Slots.Hotbar + Slots.Inventory + Slots.OffHand
    private val chestplateSlot = Slots.Armor[2]

    @Suppress("unused")
    private val scheduleInventoryActionHandler = handler<ScheduleInventoryActionEvent>(
        EventPriorityConvention.CRITICAL_MODIFICATION
    ) { event ->
        val elytraItem = slotsToSearch.findSlot { it.isElytra() && !it.willBreakNextUse() }
        val chestplateItem = slotsToSearch.findSlot { it.isChestArmor }

        val chestplateStack = chestplateSlot.itemStack
        when {
            // put on elytra
            chestplateStack.isEmpty && elytraItem != null -> event.doSwap(elytraItem)

            // replacing of elytra with a chestplate
            chestplateStack.isElytra() && chestplateItem != null -> event.doSwap(chestplateItem)

            // replacing the chestplate with elytra
            chestplateStack.isChestArmor && elytraItem != null -> event.doSwap(elytraItem)
        }

        enabled = false
    }

    private fun ScheduleInventoryActionEvent.doSwap(slot: ItemSlot) {
        var exchange: InventoryAction? = null
        if (!chestplateSlot.itemStack.isEmpty) {
            exchange = InventoryAction.Click.performPickup(slot = slot)
        }

        val actions = listOfNotNull(
            InventoryAction.Click.performPickup(slot = slot),
            InventoryAction.Click.performPickup(slot = chestplateSlot),
            exchange
        )

        schedule(constraints, actions)
    }

    private fun ItemStack.isElytra() = this.item == Items.ELYTRA

}
