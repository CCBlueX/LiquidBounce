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
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ModuleInventoryCleaner
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

        val screen = mc.currentScreen as? GenericContainerScreen

        for ((index, armorPiece) in bestArmor.withIndex()) {
            if (!canOperate(player))
                break

            val isOnLastPiece = index == bestArmor.lastIndex
            val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

            if (armorPiece.isAlreadyEquipped || stackInArmor.item == Items.ELYTRA)
                continue

            val inventorySlot = armorPiece.itemSlot.getIdForServer(screen) ?: continue
            val armorPieceSlot = ArmorItemSlot(armorPiece.entitySlotId).getIdForServer(null) ?: continue

            val moveOccurred = if (!stackInArmor.isNothing()) {
                // Clear current armor
                move(armorPieceSlot, true)
            } else {
                // Equip new armor
                move(inventorySlot, false)
            }

            if (moveOccurred) {
                locked = true

                // Is it on its way to the last piece?
                if (!isOnLastPiece) {
                    // Wait the requested delay, then continue. In case the user violates NoMove,
                    // it immediately goes to the next loop, breaks it and then closes inventory if it's open
                    wait(inventoryConstraints.delay.random()) { inventoryConstraints.violatesNoMove }

                    continue
                }

                // Close in the next tick, good anti-cheats will need this
                return@repeatable
            }
        }

        if (locked && !isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        locked = false
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
        val itemsInInventory = ModuleInventoryCleaner.findItemSlotsInInventory()
        val armorPiecesGroupedBySlotId = itemsInInventory.mapNotNull { slot ->
            return@mapNotNull when (slot.itemStack.item) {
                is ArmorItem -> ArmorPiece(slot)
                else -> null
            }
        }.groupBy(ArmorPiece::entitySlotId)

        return armorPiecesGroupedBySlotId.values.mapNotNull { it.maxWithOrNull(ArmorComparator) }
    }

    /**
     * Shift+Left-clicks the specified [clientSlot]
     *
     * @return True if it is unable to move the item
     */
    private fun move(
        clientSlot: Int,
        isObsolete: Boolean,
    ): Boolean {
        val isInventoryOpen = InventoryTracker.isInventoryOpenServerSide

        val canTryHotbarMove = !isObsolete && hotbar && !isInventoryOpen

        if (isHotbarSlot(clientSlot) && canTryHotbarMove) {
            clickHotbarOrOffhand(clientSlot)

            return true
        }

        val canDoConventionalMove = !interaction.hasRidingInventory()

        if (!canDoConventionalMove) {
            return false
        }

        return tryConventionalMove(isInventoryOpen, isObsolete, clientSlot)
    }

    private fun tryConventionalMove(
        isInventoryOpen: Boolean,
        isObsolete: Boolean,
        slot: Int,
    ): Boolean {
        val isCurrentMovementLegal = !inventoryConstraints.violatesNoMove
        val isInventoryStateLegal = !inventoryConstraints.invOpen || isInventoryOpen

        if (!isCurrentMovementLegal || !isInventoryStateLegal) {
            return false
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isObsolete && player.inventory.main.none { it.isEmpty }

        // Open inventory, click selected slots but don't close it just yet
        runWithOpenedInventory {
            if (shouldThrow) {
                interaction.clickSlot(0, slot, 1, SlotActionType.THROW, player)
            } else {
                interaction.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)
            }

            false
        }

        return true
    }

    override fun disable() {
        locked = false
    }
}
