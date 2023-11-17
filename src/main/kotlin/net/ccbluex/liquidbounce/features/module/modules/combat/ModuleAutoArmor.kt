/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2023 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.combat

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ArmorItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoArmor module
 *
 * Automatically put on the best armor.
 */
object ModuleAutoArmor : Module("AutoArmor", Category.COMBAT) {
    private val inventoryConstraints = tree(InventoryConstraintsConfigurable())
    //private val startDelay by intRange("StartDelay", 1..2, 0..20)
    //private val closeDelay by intRange("CloseDelay", 1..2, 0..20)
    private val hotbar by boolean("Hotbar", true)

    var locked = false

    val repeatable = repeatable {
        val player = mc.player ?: return@repeatable

        val bestArmor = findBestArmorPiecesInInventory()

        for ((armorIndex, armorPiece) in bestArmor.withIndex()) {
            if (!canOperate(player))
                break

            val moveOccurred = equipArmorPiece(armorPiece)

            if (!moveOccurred) {
                continue
            }

            locked = true

            // Wait if this was not the last armor piece we want to equip
            if (armorIndex != bestArmor.lastIndex) {
                // Wait the requested delay, then continue. In case the user violates NoMove,
                // it immediately goes to the next loop, breaks it and then closes inventory if it's open
                wait(inventoryConstraints.delay.random()) { inventoryConstraints.violatesNoMove }

                continue
            }

            // Close in the next tick, good anti-cheats will need this
            return@repeatable
        }

        if (locked && !isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        locked = false
    }

    /**
     * Tries to move the given armor piece in the target slot in the inventory. There are two possible behaviors:
     * 1. If there is no free space in the target slot, it will make space in that slot (see [performMoveOrHotbarClick])
     * 2. If there is free space, it will move the armor piece there
     *
     * @return false if a move was not possible, true if a move occurred
     */
    private fun equipArmorPiece(armorPiece: ArmorPiece): Boolean {
        val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

        if (armorPiece.isAlreadyEquipped || stackInArmor.item == Items.ELYTRA)
            return false

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

    private fun canOperate(player: ClientPlayerEntity): Boolean {
        if (inventoryConstraints.violatesNoMove && InventoryTracker.isInventoryOpenServerSide) {
            return false
        }

        // We cannot move items while in a different screen
        if (player.currentScreenHandler.syncId != 0) {
            return false
        }

        return true
    }

    private fun findBestArmorPiecesInInventory(): List<ArmorPiece> {
        val armorPiecesGroupedBySlotId = ALL_SLOTS_IN_INVENTORY.mapNotNull { slot ->
            return@mapNotNull when (slot.itemStack.item) {
                is ArmorItem -> ArmorPiece(slot)
                else -> null
            }
        }.groupBy(ArmorPiece::entitySlotId)

        return armorPiecesGroupedBySlotId.values.mapNotNull { it.maxWithOrNull(ArmorComparator) }
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
        isInArmorSlot: Boolean,
    ): Boolean {
        val canTryHotbarMove = !isInArmorSlot && hotbar && !InventoryTracker.isInventoryOpenServerSide

        if (slot is HotbarItemSlot && canTryHotbarMove) {
            useHotbarSlotOrOffhand(slot)

            return true
        }

        val canDoConventionalMove = !interaction.hasRidingInventory()

        if (!canDoConventionalMove) {
            return false
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && player.inventory.main.none { it.isEmpty }

        return performInventoryMove(slot, shouldThrow)
    }

    /**
     * Clicks the slot.
     *
     * @param shouldThrow true if a throw should be performed, otherwise a quick move.
     */
    private fun performInventoryMove(
        slot: ItemSlot,
        shouldThrow: Boolean,
    ): Boolean {
        val isCurrentMovementLegal = !inventoryConstraints.violatesNoMove
        val isInventoryStateLegal = !inventoryConstraints.invOpen || InventoryTracker.isInventoryOpenServerSide

        if (!isCurrentMovementLegal || !isInventoryStateLegal) {
            return false
        }

        val screen = mc.currentScreen as? GenericContainerScreen
        val serverSlotId = slot.getIdForServer(screen) ?: return false

        // Open inventory, click selected slots but don't close it just yet
        runWithOpenedInventory {
            if (shouldThrow) {
                interaction.clickSlot(0, serverSlotId, 1, SlotActionType.THROW, player)
            } else {
                interaction.clickSlot(0, serverSlotId, 0, SlotActionType.QUICK_MOVE, player)
            }

            false
        }

        return true
    }

    override fun disable() {
        locked = false
    }
}
