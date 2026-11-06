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
package net.ccbluex.liquidbounce.features.module.modules.player.autodeposit

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.ModuleCategories
import net.ccbluex.liquidbounce.utils.collection.Filter
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenHandlerTypeValueGroup
import net.ccbluex.liquidbounce.utils.inventory.CheckScreenTitleValueGroup
import net.ccbluex.liquidbounce.utils.inventory.InventoryAction
import net.ccbluex.liquidbounce.utils.inventory.InventoryConstraints
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.inventory.getSlotsInContainer
import net.ccbluex.liquidbounce.utils.inventory.mergeableCapacityFor
import net.ccbluex.liquidbounce.utils.inventory.syncId
import net.minecraft.client.gui.screens.Screen
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.gui.screens.inventory.InventoryScreen

/**
 * AutoDeposit module
 *
 * Automatically deposits configured items from the player inventory into containers.
 */
object ModuleAutoDeposit : ClientModule(
    "AutoDeposit", ModuleCategories.PLAYER,
    aliases = listOf("AutoStore", "ContainerStorer")
) {

    private val filter by enumChoice("Filter", Filter.WHITELIST)
    private val itemsList by items("Items", itemSortedSetOf())
    private val autoClose by boolean("AutoClose", true)

    private val inventoryConstraints = tree(InventoryConstraints())
    private val checkScreenHandlerType = tree(CheckScreenHandlerTypeValueGroup(this))
    private val checkScreenTitle = tree(CheckScreenTitleValueGroup(this))

    init {
        tree(PunchToDeposit)
    }

    /**
     * Whether the player inventory currently holds any item matching the configured filter.
     *
     * Used by features to only act (e.g. click containers) when there is something to deposit.
     */
    fun matchingSlots(inventory: Boolean) = (if (inventory) Slots.HotbarAndInventory else Slots.Hotbar)
        .filter { !it.itemStack.isEmpty && filter(it.itemStack.item, itemsList) }

    fun canBeStored(screen: Screen): Boolean {
        return running && screen is AbstractContainerScreen<*> && screen !is InventoryScreen &&
            checkScreenHandlerType.isValid(screen) && checkScreenTitle.isValid(screen)
    }

    // Session state so we only auto-close containers we actually deposited into.
    private var currentSyncId = -1
    private var depositedAnything = false

    override fun onDisabled() {
        super.onDisabled()
        currentSyncId = -1
        depositedAnything = false
    }

    @Suppress("unused")
    private val scheduleInventoryAction = handler<ScheduleInventoryActionEvent> { event ->
        val screen = mc.gui.screen() as? AbstractContainerScreen<*> ?: run {
            currentSyncId = -1
            depositedAnything = false
            return@handler
        }
        if (!canBeStored(screen)) {
            return@handler
        }

        if (screen.syncId != currentSyncId) {
            currentSyncId = screen.syncId
            depositedAnything = false
        }

        val matching = matchingSlots(inventory = true)
        if (matching.isNotEmpty()) {
            depositedAnything = true
        }

        // Only click items the container can still absorb; a full container would otherwise be
        // re-clicked forever since the server rejects the move without changing the inventory.
        val containerSlots = screen.getSlotsInContainer()
        val targets = matching.filter { containerSlots.mergeableCapacityFor(it.itemStack) > 0 }

        if (targets.isNotEmpty()) {
            for (slot in targets) {
                event.schedule(inventoryConstraints, InventoryAction.Click.performQuickMove(screen, slot))
            }
        } else if (autoClose && depositedAnything) {
            event.schedule(inventoryConstraints, InventoryAction.CloseScreen(screen))
            currentSyncId = -1
        }
    }

}
