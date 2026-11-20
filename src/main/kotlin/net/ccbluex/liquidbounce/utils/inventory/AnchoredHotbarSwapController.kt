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
import net.ccbluex.liquidbounce.event.requires
import net.ccbluex.liquidbounce.utils.client.SilentHotbar
import net.minecraft.world.item.ItemStack
import java.util.function.IntSupplier

/**
 * Reusable anchored hotbar swap state machine:
 * - keep the first swap as restore anchor
 * - swap selected inventory items into one fixed hotbar slot
 * - restore anchor after no active switching for [swapDelayProvider] ticks
 */
class AnchoredHotbarSwapController(
    private val owner: EventListener,
    private val inventoryConstraints: InventoryConstraints,
    private val swapDelayProvider: IntSupplier,
    private val anchorHotbarSlotResolver: () -> HotbarItemSlot = {
        Slots.Hotbar.findSlot { it.isEmpty } ?: Slots.Hotbar[SilentHotbar.serversideSlot]
    },
) : EventListener {

    private var requestedSourceSlot: ItemSlot? = null
    private var anchorSwapAction: InventoryAction? = null
    private var anchorHotbarSlot: HotbarItemSlot? = null
    private var idleTicks = 0

    override fun parent() = owner

    /**
     * Request swapping [sourceSlot] into the anchored hotbar slot as soon as the inventory allows it.
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
        idleTicks = 0
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
        idleTicks = 0
    }

    @Suppress("unused")
    private val swap = inventoryPlan(inventoryConstraints) {
        val source = requestedSourceSlot
        if (source != null) {
            val anchor = anchorHotbarSlot ?: anchorHotbarSlotResolver()
            val swapAction = InventoryAction.Click.performSwap(from = source, to = anchor)

            val stack = source.itemStack.copy()
            // The request is withdrawn once the item is in the hotbar, the run has to remember the swap then
            requires { requestedSourceSlot == source || !ItemStack.matches(source.itemStack, stack) }
            perform(swapAction)

            anchorHotbarSlot = anchor
            if (anchorSwapAction == null) {
                anchorSwapAction = swapAction
            }
            idleTicks = 0
            requestedSourceSlot = null
            return@inventoryPlan
        }

        val restoreAction = anchorSwapAction ?: return@inventoryPlan
        val anchor = anchorHotbarSlot ?: return@inventoryPlan

        // A run that has nothing to do ends at once, so this counts ticks
        if (++idleTicks <= swapDelayProvider.asInt) {
            return@inventoryPlan
        }

        val stack = anchor.itemStack.copy()
        requires {
            idleTicks > swapDelayProvider.asInt && requestedSourceSlot == null ||
                !ItemStack.matches(anchor.itemStack, stack)
        }
        perform(restoreAction)
        reset()
    }
}
