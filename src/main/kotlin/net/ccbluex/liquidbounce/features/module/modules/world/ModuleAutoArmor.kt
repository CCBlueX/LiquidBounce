/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2016 - 2021 CCBlueX
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

package net.ccbluex.liquidbounce.features.module.modules.world

import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.repeatable
import net.ccbluex.liquidbounce.utils.ArmorComparator
import net.ccbluex.liquidbounce.utils.ArmorPiece
import net.ccbluex.liquidbounce.utils.convertClientSlotToServerSlot
import net.ccbluex.liquidbounce.utils.extensions.isNothing
import net.ccbluex.liquidbounce.utils.extensions.moving
import net.minecraft.client.gui.screen.ingame.InventoryScreen
import net.minecraft.item.ArmorItem
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket
import net.minecraft.screen.slot.SlotActionType
import net.minecraft.util.Hand


// TODO: Add ItemDelay
object ModuleAutoArmor : Module("AutoArmor", Category.COMBAT) {
    private var delay by intRange("Delay", 2..4, 0..20)
    private val invOpen by boolean("InvOpen", false)
    private val simulateInventory by boolean("SimulateInventory", true)
    private val noMove by boolean("NoMove", false)
    private val hotbar by boolean("Hotbar", true)

    var locked = false

    val onTick = repeatable {
        val player = mc.player ?: return@repeatable

        if (player.currentScreenHandler.syncId != 0 && invOpen)
            return@repeatable

        val bestArmor = (0..41)
            .mapNotNull { slot ->
                val stack = player.inventory.getStack(slot)

                return@mapNotNull when (stack?.item) {
                    is ArmorItem -> ArmorPiece(stack, slot)
                    else -> null
                }
            }
            .groupBy(ArmorPiece::entitySlotId)
            .values
            .mapNotNull {
                it.maxWithOrNull(ArmorComparator)
            }

        for (armorPiece in bestArmor) {
            if (armorPiece.isAlreadyEquipped)
                continue

            if (
                !player.inventory.getStack(armorPiece.inventorySlot).isNothing() && move(armorPiece.inventorySlot, true)
                || player.inventory.getStack(armorPiece.inventorySlot).isNothing() && move(armorPiece.slot, false)
            ) {
                locked = true
                wait(delay.random())

                return@repeatable
            }
        }

        locked = false
    }

    /**
     * Shift+Left clicks the specified item
     *
     * @param slot        Slot of the item to click
     * @param isObsolete
     *
     * @return True if it is unable to move the item
     */
    private fun move(item: Int, isObsolete: Boolean): Boolean {
        val player = mc.player!!
        val networkHandler = mc.networkHandler!!

        val slot = convertClientSlotToServerSlot(item)
        val isInInventoryScreen = mc.currentScreen is InventoryScreen

        if (!isObsolete && hotbar && !isInInventoryScreen) {
            if (slot in 36..44) {
                networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(slot))
                networkHandler.sendPacket(PlayerInteractItemC2SPacket(Hand.MAIN_HAND))
                networkHandler.sendPacket(UpdateSelectedSlotC2SPacket(player.inventory.selectedSlot))

                return true
            } else if (slot == 45) {
                networkHandler.sendPacket(PlayerInteractItemC2SPacket(Hand.OFF_HAND))

                return true
            }
        }

        if (!(noMove && player.moving) && (!invOpen || isInInventoryScreen)) {
            val openInventory = simulateInventory && !isInInventoryScreen

            if (openInventory)
                networkHandler.sendPacket(ClientCommandC2SPacket(player, ClientCommandC2SPacket.Mode.OPEN_INVENTORY))

            // Should the item be just thrown out of the inventory
            val shouldThrow = isObsolete && player.inventory.main.none { it.isEmpty }

            if (shouldThrow) {
                mc.interactionManager!!.clickSlot(0, slot, 1, SlotActionType.THROW, player)
            } else {
                mc.interactionManager!!.clickSlot(0, slot, 0, SlotActionType.QUICK_MOVE, player)
            }

            if (openInventory)
                networkHandler.sendPacket(CloseHandledScreenC2SPacket(0))

            return true
        }

        return false
    }
}
