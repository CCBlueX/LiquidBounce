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

package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenHandlerTypeValueGroup
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenTitleValueGroup
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.PlayerInventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.findItemsInContainer
import net.ccbluex.liquidbounce.utils.inventory.inventoryPlan
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * ChestCleaner module
 *
 * Automatically drops unwanted items from a chest.
 *
 */
object ModuleChestCleaner : ClientModule(
    "ChestCleaner", ModuleCategories.PLAYER,
    aliases = listOf("ContainerCleaner")
) {
    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val itemsList by items("Items", itemSortedSetOf())
    private val autoClose by boolean("AutoClose", true)

    private val inventoryConstraints = tree(PlayerInventoryConstraints())
    private val checkScreenHandlerType = tree(CheckScreenHandlerTypeValueGroup(this))
    private val checkScreenTitle = tree(CheckScreenTitleValueGroup(this))

    @Suppress("unused")
    private val clean = inventoryPlan(inventoryConstraints) {
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: return@inventoryPlan
        if (screen is InventoryScreen) return@inventoryPlan
        if (!checkScreenHandlerType.isValid(screen) || !checkScreenTitle.isValid(screen)) return@inventoryPlan

        val slots = screen.findItemsInContainer()
        val selectedSlots = slots.filter { !it.itemStack.isEmpty && filter(it.itemStack.item, itemsList) }

        for (slot in selectedSlots) {
            perform(InventoryAction.Click.performThrow(screen, slot))
        }

        if (autoClose) {
            perform(InventoryAction.CloseScreen(screen))
        }
    }
}
