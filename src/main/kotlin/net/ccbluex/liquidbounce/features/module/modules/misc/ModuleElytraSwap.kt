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
package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.inventory.ArmorItemSlot
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.ccbluex.liquidbounce.utils.item.isChestArmor
import net.ccbluex.liquidbounce.utils.item.isGlider

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
    ModuleCategories.PLAYER,
    aliases = listOf("ChestSwap"),
    disableOnQuit = true
) {

    private val constraints = tree(PlayerInventoryConstraints())

    private val slotsToSearch = Slots.Hotbar + Slots.Inventory + HotbarItemSlot.OFFHAND

    @Suppress("unused")
    private val swap = inventoryPlan(constraints) {
        val elytraItem = slotsToSearch.findSlot { it.isGlider && !it.nextDamageWillBreak() }
        val chestplateItem = slotsToSearch.findSlot { it.isChestArmor }

        val chestplateStack = ArmorItemSlot.CHEST.itemStack
        val slot = when {
            // put on elytra
            chestplateStack.isEmpty && elytraItem != null -> elytraItem

            // replacing of elytra with a chestplate
            chestplateStack.isGlider && chestplateItem != null -> chestplateItem

            // replacing the chestplate with elytra
            chestplateStack.isChestArmor && elytraItem != null -> elytraItem

            else -> null
        }

        if (slot != null) {
            perform(
                listOfNotNull(
                    InventoryAction.Click.performPickup(slot = slot),
                    InventoryAction.Click.performPickup(slot = ArmorItemSlot.CHEST),
                    InventoryAction.Click.performPickup(slot = slot).takeUnless { chestplateStack.isEmpty },
                )
            )
        }

        enabled = false
    }

}
