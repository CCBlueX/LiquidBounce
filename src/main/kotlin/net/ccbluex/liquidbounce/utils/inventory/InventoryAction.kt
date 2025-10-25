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

package net.ccbluex.liquidbounce.utils.inventory

import net.ccbluex.liquidbounce.utils.client.interaction
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.client.player
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.client.gui.screen.ingame.HandledScreen
import net.minecraft.item.ItemStack
import net.minecraft.screen.slot.SlotActionType

sealed interface InventoryAction {

    fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean

    fun performAction(): Boolean

    fun requiresPlayerInventoryOpen(): Boolean

    @JvmRecord
    data class Click(
        val screen: HandledScreen<*>? = null,
        val slot: ItemSlot,
        val button: Int,
        val actionType: SlotActionType,
    ) : InventoryAction {

        companion object {

            @JvmStatic
            fun performThrow(
                screen: HandledScreen<*>? = null,
                slot: ItemSlot
            ) = Click(
                screen,
                slot = slot,
                button = 1,
                actionType = SlotActionType.THROW
            )

            @JvmStatic
            fun performQuickMove(
                screen: HandledScreen<*>? = null,
                slot: ItemSlot
            ) = Click(
                screen,
                slot = slot,
                button = 0,
                actionType = SlotActionType.QUICK_MOVE
            )

            @JvmStatic
            fun performSwap(
                screen: HandledScreen<*>? = null,
                from: ItemSlot,
                to: HotbarItemSlot
            ) = Click(
                screen,
                slot = from,
                button = to.hotbarSlotForServer,
                actionType = SlotActionType.SWAP
            )

            @JvmStatic
            fun performPickupAll(
                screen: HandledScreen<*>? = null,
                slot: ItemSlot
            ) = Click(
                screen,
                slot = slot,
                button = 0,
                actionType = SlotActionType.PICKUP_ALL
            )

            @JvmStatic
            fun performPickup(
                screen: HandledScreen<*>? = null,
                slot: ItemSlot
            ) = Click(
                screen,
                slot = slot,
                button = 0,
                actionType = SlotActionType.PICKUP
            )

            /**
             * pickup -> pickup all -> pickup to handle remaining items
             */
            @JvmStatic
            fun performMergeStack(
                screen: HandledScreen<*>? = null,
                slot: ItemSlot,
            ) = listOf(
                performPickup(screen, slot = slot),
                performPickupAll(screen, slot = slot),
                performPickup(screen, slot = slot),
            )

        }

        override fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean {
            // Check constrains
            if (!inventoryConstraints.passesRequirements(this)) {
                return false
            }

            // Screen is null, which means we are targeting the player inventory
            if (requiresPlayerInventoryOpen() && player.currentScreenHandler.isPlayerInventory &&
                !interaction.hasRidingInventory()
            ) {
                return true
            }

            // Check if current screen is the same as the screen we want to interact with
            val screen = mc.currentScreen as? HandledScreen<*> ?: return false
            return screen.syncId == this.screen.syncId
        }

        override fun performAction(): Boolean {
            val slotId = slot.getIdForServer(screen) ?: return false
            interaction.clickSlot(screen?.syncId ?: 0, slotId, button, actionType, player)
            InventoryManager.lastClickedSlot = slotId

            return true
        }

        fun performMissClick(): Boolean {
            if (slot !is ContainerItemSlot || screen == null) {
                return false
            }

            val itemsInContainer = screen.getSlotsInContainer()
            // Find the closest item to the slot which is empty
            val closestEmptySlot = itemsInContainer
                .filter { it.itemStack.isEmpty }
                .minByOrNull { slot.distance(it) } ?: return false

            val slotId = closestEmptySlot.getIdForServer(screen)
            interaction.clickSlot(screen.syncId, slotId, 0, SlotActionType.PICKUP, player)
            InventoryManager.lastClickedSlot = slotId
            return true
        }

        override fun requiresPlayerInventoryOpen() = screen == null

    }

    @JvmRecord
    data class UseItem(
        val hotbarItemSlot: HotbarItemSlot,
    ) : InventoryAction {

        override fun canPerformAction(inventoryConstraints: InventoryConstraints) =
            !InventoryManager.isInventoryOpen && !isInContainerScreen && !isInInventoryScreen

        override fun performAction(): Boolean {
            useHotbarSlotOrOffhand(hotbarItemSlot)
            return true
        }

        override fun requiresPlayerInventoryOpen() = false

    }

    @JvmRecord
    data class CloseScreen(
        val screen: HandledScreen<*>,
    ) : InventoryAction {

        // Check if current handler is the same as the screen we want to close
        override fun canPerformAction(inventoryConstraints: InventoryConstraints) =
            player.currentScreenHandler.syncId == screen.syncId

        override fun performAction(): Boolean {
            player.closeHandledScreen()
            return true
        }

        override fun requiresPlayerInventoryOpen() = false

    }

    @JvmRecord
    data class Creative(
        val itemStack: ItemStack,
        val slot: ItemSlot? = null,
    ) : InventoryAction {

        companion object {
            @JvmStatic
            fun performThrow(itemStack: ItemStack) = Creative(itemStack)

            @JvmStatic
            fun performFillSlot(itemStack: ItemStack, slot: ItemSlot) = Creative(itemStack, slot)
        }

        override fun canPerformAction(inventoryConstraints: InventoryConstraints): Boolean {
            // Check constrains
            if (!inventoryConstraints.passesRequirements(this)) {
                return false
            }

            // Screen is null, which means we are targeting the player inventory
            if (requiresPlayerInventoryOpen() && player.currentScreenHandler.isPlayerInventory &&
                !interaction.hasRidingInventory()
            ) {
                return true
            }

            return player.isCreative
        }

        override fun performAction(): Boolean {
            val slot = slot

            if (slot != null) {
                val slotId = slot.getIdForServer(null) ?: return false
                interaction.clickCreativeStack(itemStack, slotId)
                InventoryManager.lastClickedSlot = slotId
            } else {
                interaction.dropCreativeStack(itemStack)
            }
            return true
        }

        override fun requiresPlayerInventoryOpen() = false

    }

    /**
     * A chained inventory action is a list of inventory actions that have to be executed in order
     * and CANNOT be stopped in between
     */
    @JvmRecord
    data class Chain(
        val inventoryConstraints: InventoryConstraints,
        val actions: List<InventoryAction>,
        val priority: Priority,
    ) {

        fun canPerformAction(): Boolean {
            return actions.all { action -> action.canPerformAction(inventoryConstraints) }
        }

        fun requiresInventoryOpen() = actions.any { it is Click && it.screen == null }

    }

}
