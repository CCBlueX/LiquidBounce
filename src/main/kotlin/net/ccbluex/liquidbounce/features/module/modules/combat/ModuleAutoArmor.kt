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
import net.ccbluex.liquidbounce.utils.entity.moving
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece
import net.ccbluex.liquidbounce.utils.item.InventoryConstraintsConfigurable
import net.ccbluex.liquidbounce.utils.item.clickHotbarOrOffhand
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
import net.ccbluex.liquidbounce.utils.item.isHotbarSlot
import net.ccbluex.liquidbounce.utils.item.isNothing
import net.ccbluex.liquidbounce.utils.item.isPlayerInventory
import net.ccbluex.liquidbounce.utils.item.runWithOpenedInventory
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.client.network.ClientPlayerEntity
import net.minecraft.item.ArmorItem
import net.minecraft.item.Items
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

    val repeatable =
        repeatable {
            val player = mc.player ?: return@repeatable

            if (player.currentScreenHandler.isPlayerInventory || interaction.hasRidingInventory()) {
                return@repeatable
            }

            val bestArmor = findBestArmorPiecesInInventory(player)

            for (armorPiece in bestArmor) {
                if (armorPiece.isAlreadyEquipped) {
                    continue
                }

                val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

                if (stackInArmor.item == Items.ELYTRA) {
                    continue
                }

                val moveOccurred =
                    if (stackInArmor.isNothing()) {
                        // Clear current armor
                        move(armorPiece.inventorySlot, true)
                    } else {
                        // Equip new armor
                        move(armorPiece.slot, false)
                    }

                if (moveOccurred) {
                    locked = true
                    wait(inventoryConstraints.delay.random())

                    return@repeatable
                }
            }

            locked = false
        }

    private fun findBestArmorPiecesInInventory(player: ClientPlayerEntity): List<ArmorPiece> {
        val armorPiecesGroupedBySlotId =
            (0..41)
                .mapNotNull { slot ->
                    val stack = player.inventory.getStack(slot)

                    return@mapNotNull when (stack?.item) {
                        is ArmorItem -> ArmorPiece(stack, slot)
                        else -> null
                    }
                }
                .groupBy(ArmorPiece::entitySlotId)

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
        val serverSlot = convertClientSlotToServerSlot(clientSlot, null)
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        val canTryHotbarMove = !isObsolete && hotbar && !isInInventoryScreen

        if (isHotbarSlot(clientSlot) && canTryHotbarMove) {
            clickHotbarOrOffhand(clientSlot)

            return true
        }

        return tryConventionalMove(isInInventoryScreen, isObsolete, serverSlot)
    }

    private fun tryConventionalMove(
        isInInventoryScreen: Boolean,
        isObsolete: Boolean,
        slot: Int,
    ): Boolean {
        val isCurrentMovementLegal = !inventoryConstraints.noMove || !player.moving
        val isInventoryStateLegal = !inventoryConstraints.invOpen || isInInventoryScreen

        if (!isCurrentMovementLegal || !isInventoryStateLegal) {
            return false
        }

        // Should the item be just thrown out of the inventory
        val shouldThrow = isObsolete && player.inventory.main.none { it.isEmpty }

        runWithOpenedInventory {
            if (shouldThrow) {
                interaction.clickSlot(0, slot, 1, SlotActionType.THROW, player)
            } else {
                interaction.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)
            }
        }

        return true
    }

    override fun disable() {
        locked = false
    }
}
