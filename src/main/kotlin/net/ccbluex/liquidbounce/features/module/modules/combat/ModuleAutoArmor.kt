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
import net.ccbluex.liquidbounce.utils.item.*
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
    private val hotbar by boolean("Hotbar", true)

    var locked = false

    val repeatable = repeatable {
        val player = mc.player ?: return@repeatable

        val bestArmor = findBestArmorPiecesInInventory(player)

        for ((index, armorPiece) in bestArmor.withIndex()) {
            val isOnLastPiece = index == bestArmor.lastIndex

            if (inventoryConstraints.violatesNoMove && InventoryTracker.isInventoryOpenServerSide) {
                break
            }

            val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

            if (armorPiece.isAlreadyEquipped || stackInArmor.item == Items.ELYTRA) {
                continue
            }

            val moveOccurred = if (!stackInArmor.isNothing()) {
                // Clear current armor
                move(armorPiece.inventorySlot, true)
            } else {
                // Equip new armor
                move(armorPiece.slot, false)
            }

            if (moveOccurred) {
                locked = true

                if (!isOnLastPiece) {
                    wait(inventoryConstraints.delay.random()) { inventoryConstraints.violatesNoMove }
                }
            }
        }

        if (locked && !isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }

        locked = false
    }

    private fun findBestArmorPiecesInInventory(player: ClientPlayerEntity): List<ArmorPiece> {
        val armorPiecesGroupedBySlotId = (0..41).mapNotNull { slot ->
            val stack = player.inventory.getStack(slot) ?: return@mapNotNull null

            return@mapNotNull when (stack.item) {
                is ArmorItem -> ArmorPiece(stack, slot)
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
        val serverSlot = convertClientSlotToServerSlot(clientSlot)
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

        return tryConventionalMove(isInventoryOpen, isObsolete, serverSlot)
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
