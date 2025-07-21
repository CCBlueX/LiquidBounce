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
package net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor

import net.ccbluex.liquidbounce.config.types.nesting.ToggleableConfigurable
import net.ccbluex.liquidbounce.event.events.ScheduleInventoryActionEvent
import net.ccbluex.liquidbounce.event.handler
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.ClientModule
import net.ccbluex.liquidbounce.features.module.modules.combat.autoarmor.AutoArmorSaveArmor.durabilityThreshold
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.*
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.minecraft.item.Items

/**
 * AutoArmor module
 *
 * Automatically puts on the best armor.
 */
object ModuleAutoArmor : ClientModule("AutoArmor", Category.COMBAT) {

    val inventoryConstraints = tree(PlayerInventoryConstraints())

    /**
     * Should the module use the hotbar to equip armor pieces?
     * If disabled, it will only use inventory moves.
     */
    object UseHotbar : ToggleableConfigurable(this, "Hotbar", true) {
        /**
         * Defines whether the [UseHotbar] option supports the armor swap from MC 1.19.4+.
         */
        val canSwapArmor by boolean("CanSwapArmor", false)
    }

    init {
        tree(UseHotbar)
        tree(AutoArmorSaveArmor)
    }

    @Suppress("unused")
    private val scheduleHandler = handler<ScheduleInventoryActionEvent> { event ->
        if (player.isSpectator) {
            return@handler
        }

        // Filter out already equipped armor pieces
        val durabilityThreshold = if (AutoArmorSaveArmor.enabled) durabilityThreshold else Int.MIN_VALUE

        val armorToEquip = ArmorEvaluation
            .findBestArmorPieces(durabilityThreshold = durabilityThreshold)
            .values.filterNotNull().filter { !it.isAlreadyEquipped }

        for (armorPiece in armorToEquip) {
            event.schedule(
                inventoryConstraints,
                equipArmorPiece(armorPiece) ?: continue,
                Priority.IMPORTANT_FOR_PLAYER_LIFE
            )
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

        return performMoveOrHotbarClick(armorPiece, isInArmorSlot = !stackInArmor.isNothing())
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
        armorPiece: ArmorPiece,
        isInArmorSlot: Boolean
    ): InventoryAction {
        val inventorySlot = armorPiece.itemSlot
        val armorPieceSlot = if (isInArmorSlot) Slots.Armor[armorPiece.entitySlotId] else inventorySlot

        val canTryHotbarMove = booleanArrayOf(
            UseHotbar.enabled,
            !InventoryManager.isInventoryOpen,
            (!isInArmorSlot || UseHotbar.canSwapArmor)
        ).all { it }

        if (inventorySlot is HotbarItemSlot && canTryHotbarMove) {
            return UseInventoryAction(inventorySlot)
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && !hasInventorySpace()

        return if (shouldThrow) {
            ClickInventoryAction.performThrow(screen = null, armorPieceSlot)
        } else {
            ClickInventoryAction.performQuickMove(screen = null, armorPieceSlot)
        }
    }

}
