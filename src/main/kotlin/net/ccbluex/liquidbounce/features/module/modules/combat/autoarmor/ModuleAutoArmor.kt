/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2024 CCBlueX
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
package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ArmorItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.minecraft.item.Items

/**
 * AutoArmor module
 *
 * Automatically put on the best armor.
 */
object ModuleAutoArmor : Module("AutoArmor", Category.COMBAT) {

    private val inventoryConstraints = tree(PlayerInventoryConstraints())

    /**
     * Should the module use the hotbar to equip armor pieces.
     * If disabled, it will only use inventory moves.
     */
    private val useHotbar by boolean("Hotbar", true)

    private val scheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        // Filter out already equipped armor pieces
        val armorToEquip = ArmorEvaluation.findBestArmorPieces().values.filterNotNull().filter {
            !it.isAlreadyEquipped
        }

        for (armorPiece in armorToEquip) {
            event.schedule(inventoryConstraints, equipArmorPiece(armorPiece) ?: continue)
        }
    }

    /**
     * Tries to move the given armor piece in the target slot in the inventory. There are two possible behaviors:
     * 1. If there is no free space in the target slot, it will make space in that slot (see [performMoveOrHotbarClick])
     * 2. If there is free space, it will move the armor piece there
     *
     * @return false if a move was not possible, true if a move occurred
     */
    private fun equipArmorPiece(armorPiece: ArmorPiece): InventoryAction? {
        val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

        if (stackInArmor.item == Items.ELYTRA) {
            return null
        }

        val inventorySlot = armorPiece.itemSlot
        val armorPieceSlot = ArmorItemSlot(armorPiece.entitySlotId)

        return if (!stackInArmor.isNothing()) {
            // Clear current armor
            performMoveOrHotbarClick(armorPieceSlot, isInArmorSlot = true)
        } else {
            // Equip new armor
            performMoveOrHotbarClick(inventorySlot, isInArmorSlot = false)
        }
    }

    /**
     * Central move-function of this module. There are following options:
     * 1. If the slot is in the hotbar, we do a right-click on it (if possible)
     * 2. If the slot is in inventory, we shift+left click it
     * 3. If the slot is an armor slot and there is free space in inventory, we shift+left click it otherwise
     * throw it out.
     *
     * @param isInArmorSlot True if the slot is an armor slot.
     * @return True if a move occurred.
     */
    private fun performMoveOrHotbarClick(
        slot: ItemSlot,
        isInArmorSlot: Boolean
    ): InventoryAction {
        val canTryHotbarMove = !isInArmorSlot && useHotbar && !InventoryManager.isInventoryOpenServerSide
        if (slot is HotbarItemSlot && canTryHotbarMove) {
            return UseInventoryAction(slot)
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && !hasInventorySpace()

        return if (shouldThrow) {
            ClickInventoryAction.performThrow(screen = null, slot)
        } else {
            ClickInventoryAction.performQuickMove(screen = null, slot)
        }
    }

}
