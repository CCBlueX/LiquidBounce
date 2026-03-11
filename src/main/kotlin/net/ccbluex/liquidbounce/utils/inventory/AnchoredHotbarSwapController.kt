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

import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.event.events.GameTickEvent
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.utils.client.SilentHotbar

/**
 * Reusable anchored hotbar swap state machine:
 * - keep the first swap as restore anchor
 * - swap selected inventory items into one fixed hotbar slot
 * - restore anchor after no active switching for [swapDelayProvider] ticks
 */
class AnchoredHotbarSwapController(
    private val owner: EventListener,
    private val inventoryConstraints: InventoryConstraints,
    private val swapDelayProvider: () -> Int,
    private val anchorHotbarSlotResolver: () -> HotbarItemSlot = {
        Slots.Hotbar.findSlot { it.isEmpty } ?: Slots.Hotbar[SilentHotbar.serversideSlot]
    },
) : EventListener {

    private var requestedSourceSlot: ItemSlot? = null
    private var anchorSwapAction: InventoryAction? = null
    private var anchorHotbarSlot: HotbarItemSlot? = null
    private var pendingRestore = false
    private var restoreDue = false
    private var waitingTicks = 0
    private var lastSwitchScheduledEvent: ScheduleInventoryActionEvent? = null

    override fun parent() = owner

    /**
     * Request swapping [sourceSlot] into the anchored hotbar slot on next inventory scheduling pass.
     *
     * Call this whenever the desired item is currently in inventory (not hotbar).
     */
    fun requestSwapFromInventory(sourceSlot: ItemSlot) {
        requestedSourceSlot = sourceSlot
        touchActiveSwitching()
    }

    /**
     * Clear a pending swap request when no inventory swap is needed this tick.
     */
    fun clearRequestedSwap() {
        requestedSourceSlot = null
    }

    /**
     * Mark switching as still active, delaying restore countdown.
     *
     * Call this while your module is actively using the temporarily swapped item.
     */
    fun touchActiveSwitching() {
        if (!pendingRestore) return
        waitingTicks = 0
        restoreDue = false
    }

    /**
     * Drop all controller state immediately without restoring.
     *
     * Intended for module/feature disable paths.
     */
    fun reset() {
        requestedSourceSlot = null
        anchorSwapAction = null
        anchorHotbarSlot = null
        pendingRestore = false
        restoreDue = false
        waitingTicks = 0
        lastSwitchScheduledEvent = null
    }

    @Suppress("unused")
    private val inventorySwapHandler = handler<ScheduleInventoryActionEvent> { event ->
        val source = requestedSourceSlot ?: return@handler
        val slotToSwap = anchorHotbarSlot ?: anchorHotbarSlotResolver().also { anchorHotbarSlot = it }

        val swapAction = InventoryAction.Click.performSwap(
            from = source,
            to = slotToSwap,
        )

        event.schedule(inventoryConstraints, swapAction)
        lastSwitchScheduledEvent = event

        if (anchorSwapAction == null) {
            anchorSwapAction = swapAction
            pendingRestore = true
        }

        requestedSourceSlot = null
    }

    @Suppress("unused")
    private val restoreHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (lastSwitchScheduledEvent === event) return@handler
        if (!restoreDue || requestedSourceSlot != null) return@handler

        val restoreAction = anchorSwapAction ?: run {
            reset()
            return@handler
        }

        if (!restoreAction.canPerformAction(inventoryConstraints)) {
            return@handler
        }

        event.schedule(inventoryConstraints, restoreAction)
        reset()
    }

    @Suppress("unused")
    private val tickHandler = handler<GameTickEvent> {
        if (!pendingRestore || restoreDue) return@handler

        waitingTicks++
        if (waitingTicks <= swapDelayProvider()) return@handler

        restoreDue = true
    }
}
