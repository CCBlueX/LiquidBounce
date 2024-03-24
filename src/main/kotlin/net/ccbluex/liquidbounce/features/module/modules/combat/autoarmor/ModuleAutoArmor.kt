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

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ArmorItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.HotbarItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen
import net.minecraft.client.network.ClientPlayerEntity
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
    private val hotbar by boolean("Hotbar", true)

    var locked = false
    private var clickedInInventory = false

    val repeatable = repeatable {
        // In case during swap delay something goes wrong, we check here
        if (!canOperate(player, !locked || !clickedInInventory)) {
            return@repeatable
        }

        // Filter out already equipped armor pieces
        val armorToEquip = ArmorEvaluation.findBestArmorPieces().values.filterNotNull().filter { !it.isAlreadyEquipped }

        for ((armorIndex, armorPiece) in armorToEquip.withIndex()) {
            if (!canOperate(player)) {
                return@repeatable
            }

            val startDelay = inventoryConstraints.startDelay.random()

            val hasToSwapPiece = !player.inventory.getStack(armorPiece.inventorySlot).isNothing()
            val isFirstInventoryClick = (!armorPiece.isReachableByHand || !hotbar) && !clickedInInventory

            val moveOccurred = equipArmorPiece(armorPiece, isFirstInventoryClick && startDelay > 0)

            if (moveOccurred == null) {
                setStatus(true)

                waitConditional(startDelay - 1) { !canOperate(player) }

                return@repeatable
            } else if (moveOccurred) {
                locked = true

                // Wait if this was not the last armor piece we want to equip or a swap has occurred
                if (armorIndex != armorToEquip.lastIndex || hasToSwapPiece) {
                    val delay = inventoryConstraints.clickDelay.random()

                    // Ignore checking if there is no delay
                    if (delay > 0) {
                        waitConditional(delay - 1) { !canOperate(player) }

                        return@repeatable
                    }

                    // Prevents the following behavior when no delay:
                    // 1. Remove worse equipped armor
                    // 2. Close inventory
                    // 3. Equip better unequipped armor
                    // 4. Close inventory
                    if (hasToSwapPiece) {
                        // Sacrifice speed (one tick) to maintain proper inventory process pattern
                        return@repeatable
                    }
                }
            }
        }

        if (locked && canCloseMainInventory) {
            waitConditional(inventoryConstraints.closeDelay.random()) { !canOperate(player) }

            // Can we still close the inventory or has something changed?
            if (locked && canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }
        }

        setStatus(false)
    }

    /**
     * Tries to move the given armor piece in the target slot in the inventory. There are two possible behaviors:
     * 1. If there is no free space in the target slot, it will make space in that slot (see [performMoveOrHotbarClick])
     * 2. If there is free space, it will move the armor piece there
     *
     * @return false if a move was not possible, true if a move occurred
     */
    private fun equipArmorPiece(armorPiece: ArmorPiece, delayFirstClick: Boolean): Boolean? {
        val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

        if (stackInArmor.item == Items.ELYTRA)
            return false

        val inventorySlot = armorPiece.itemSlot
        val armorPieceSlot = ArmorItemSlot(armorPiece.entitySlotId)

        return if (!stackInArmor.isNothing()) {
            // Clear current armor
            performMoveOrHotbarClick(armorPieceSlot, isInArmorSlot = true, delayFirstClick)
        } else {
            // Equip new armor
            performMoveOrHotbarClick(inventorySlot, isInArmorSlot = false, delayFirstClick)
        }
    }

    private fun canOperate(player: ClientPlayerEntity, ignore: Boolean = true): Boolean {
        val old = locked to clickedInInventory

        setStatus(false)

        if (inventoryConstraints.violatesNoMove && (!ignore || InventoryTracker.isInventoryOpenServerSide)) {
            if (canCloseMainInventory) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return false
        }

        if (!ignore && inventoryConstraints.invOpen && !isInInventoryScreen) {
            return false
        }

        // We cannot move items while in a different screen
        if (player.currentScreenHandler.syncId != 0 || interaction.hasRidingInventory() && !ignore) {
            return false
        }

        setStatus(old.first, old.second)

        return true
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
        delayFirstClick: Boolean,
    ): Boolean? {
        val canTryHotbarMove = !isInArmorSlot && hotbar && !InventoryTracker.isInventoryOpenServerSide

        if (slot is HotbarItemSlot && canTryHotbarMove) {
            useHotbarSlotOrOffhand(slot)

            return true
        }

        // Check if module can still operate in inventory
        if (!canOperate(player, false)) {
            return false
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isInArmorSlot && player.inventory.main.none { it.isEmpty }

        return performInventoryMove(slot, shouldThrow, delayFirstClick)
    }

    /**
     * Clicks the slot.
     *
     * @param shouldThrow true if a throw should be performed, otherwise a quick move.
     */
    private fun performInventoryMove(
        slot: ItemSlot,
        shouldThrow: Boolean,
        delayFirstClick: Boolean,
    ): Boolean? {
        val screen = mc.currentScreen as? GenericContainerScreen
        val serverSlotId = slot.getIdForServer(screen) ?: return false

        // Open inventory, click selected slots but don't close it just yet
        runWithOpenedInventory {
            // Is this the first time? Return an abnormal result if so
            if (delayFirstClick) {
                return null
            }

            if (shouldThrow) {
                interaction.clickSlot(0, serverSlotId, 1, SlotActionType.THROW, player)
            } else {
                interaction.clickSlot(0, serverSlotId, 0, SlotActionType.QUICK_MOVE, player)
            }

            false
        }

        return true
    }

    private fun setStatus(locked: Boolean, invClick: Boolean = locked) {
        ModuleAutoArmor.locked = locked
        clickedInInventory = invClick
    }

    override fun disable() {
        if (canCloseMainInventory) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        setStatus(false)
    }
}
