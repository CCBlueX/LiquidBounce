/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2024 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.features

import net.ccbluex.liquidbounce.config.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.Sequence
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.ModuleAutoBuff
import net.ccbluex.liquidbounce.features.module.modules.player.autoBuff.ModuleAutoBuff.features
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

object Refill : ToggleableConfigurable(ModuleAutoBuff, "Refill", true) {

    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())

    suspend fun execute(sequence: Sequence<*>) {
        // Check if we have space in the hotbar
        if (!findEmptyHotbarSlot()) {
            return
        }

        // Find valid items in the inventory
        val validItems = INVENTORY_ITEMS.filter {
            it.itemStack.let {
                itemStack -> features.any {
                    f -> f.isValidItem(itemStack, false)
                }
            }
        }

        // Check if we have any valid items
        if (validItems.isEmpty()) {
            return
        }

        // Sort the items by the order of the features
        // TODO: Schedule the items
        performInventoryClick(sequence, validItems.first())
    }

    private fun findEmptyHotbarSlot(): Boolean {
        return ALL_SLOTS_IN_INVENTORY.find {
            it.slotType == ItemSlotType.HOTBAR && it.itemStack.isNothing()
        } != null
    }

    private fun shouldCancelInvMove(): Boolean {
        // Check if we violate the no move constraint and close the inventory
        if (inventoryConstraints.violatesNoMove) {
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        // Check if the inventory is open and the current screen is not the player inventory
        if (inventoryConstraints.invOpen && !isInInventoryScreen) {
            return true
        }

        // Check if the current screen is not the player inventory
        if (!player.currentScreenHandler.isPlayerInventory) {
            return true
        }

        return false
    }

    suspend fun performInventoryClick(sequence: Sequence<*>, slot: ItemSlot): Boolean {
        if (shouldCancelInvMove()) {
            return false
        }

        if (!isInInventoryScreen) {
            openInventorySilently()
        }

        val startDelay = inventoryConstraints.startDelay.random()

        if (startDelay > 0) {
            if (!sequence.waitConditional(startDelay) { shouldCancelInvMove() }) {
                return false
            }
        }

        interaction.performQuickMove(slot, screen = null)

        if (canCloseMainInventory) {
            sequence.waitConditional(inventoryConstraints.closeDelay.random()) { shouldCancelInvMove() }

            // Can it still be closed?
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }
        }

        return true
    }

}
