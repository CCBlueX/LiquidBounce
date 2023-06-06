/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2022 CCBlueX
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
import net.ccbluex.liquidbounce.utils.item.*
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ArmorItem
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand

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
        if (player.currentScreenHandler.syncId != 0 || interaction.hasRidingInventory()) {
            return@repeatable
        }

        val bestArmor = (0..41).mapNotNull { slot ->
            val stack = player.inventory.getStack(slot)

            return@mapNotNull when (stack?.item) {
                is ArmorItem -> ArmorPiece(stack, slot)
                else -> null
            }
        }.groupBy(ArmorPiece::entitySlotId).values.mapNotNull {
            it.maxWithOrNull(ArmorComparator)
        }

        for (armorPiece in bestArmor) {
            if (armorPiece.isAlreadyEquipped) {
                continue
            }

            val stackInArmor = player.inventory.getStack(armorPiece.inventorySlot)

            if (stackInArmor.item == Items.ELYTRA) {
                continue
            }

            if (!stackInArmor.isNothing() && move(armorPiece.inventorySlot, true) || stackInArmor.isNothing() && move(
                    armorPiece.slot, false
                )
            ) {
                locked = true
                wait { inventoryConstraints.delay.random() }

                return@repeatable
            }
        }

        locked = false
    }

    /**
     * Shift+Left clicks the specified [item]
     *
     * @return True if it is unable to move the item
     */
    private fun move(item: Int, isObsolete: Boolean): Boolean {
        val slot = convertClientSlotToServerSlot(item)
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        if (!isObsolete && hotbar && !isInInventoryScreen) {
            if (slot in 36..44) {
                if (item != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(item))
                }

                val interact = interaction.interactItem(player, Hand.MAIN_HAND)

                if (interact.shouldSwingHand()) {
                    player.swingHand(Hand.MAIN_HAND)
                }

                if (item != player.inventory.selectedSlot) {
                    network.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))
                }

                return true
            } else if (slot == 45) {
                val interact = interaction.interactItem(player, Hand.OFF_HAND)

                if (interact.shouldSwingHand()) {
                    player.swingHand(Hand.OFF_HAND)
                }

                return true
            }
        }

        if (!(inventoryConstraints.noMove && player.moving) && (!inventoryConstraints.invOpen || isInInventoryScreen)) {
            // Should the item be just thrown out of the inventory
            val shouldThrow = isObsolete && player.inventory.main.none { it.isEmpty }

            if (!isInInventoryScreen) {
                openInventorySilently()
            }

            if (shouldThrow) {
                interaction.clickSlot(0, slot, 1, SlotActionType.THROW, player)
            } else {
                interaction.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)
            }

            if (!isInInventoryScreen) {
                network.sendPacket(CloseHandledScreenC2SPacket(0))
            }

            return true
        }

        return false
    }

    override fun disable() {
        locked = false
    }
}
