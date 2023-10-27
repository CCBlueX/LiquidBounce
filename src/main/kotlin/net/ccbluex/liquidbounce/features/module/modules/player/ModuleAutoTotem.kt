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
package net.ccbluex.liquidbounce.features.module.modules.player

import net.ccbluex.liquidbounce.event.repeatable
import net.ccbluex.liquidbounce.features.module.Category
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.utils.item.convertClientSlotToServerSlot
import net.ccbluex.liquidbounce.utils.item.isInInventoryScreen
import net.ccbluex.liquidbounce.utils.item.openInventorySilently
import net.minecraft.item.ItemStack
import net.minecraft.item.Items
import net.minecraft.network.packet.c2s.play.CloseHandledScreenC2SPacket
import net.minecraft.screen.slot.SlotActionType

/**
 * AutoTotem module
 *
 * Automatically places a totem in off-hand.
 */

object ModuleAutoTotem : Module("AutoTotem", Category.PLAYER) {

    val repeatable = repeatable {
        val offHandStack = player.offHandStack

        if (isItemValid(offHandStack)) {
            return@repeatable
        }

        val inventory = player.inventory

        val slot = (0..40).find {
            isItemValid(inventory.getStack(it))
        } ?: return@repeatable

        val serverSlot = convertClientSlotToServerSlot(slot)

        if (!isInInventoryScreen) {
            openInventorySilently()
        }

        interaction.clickSlot(0, serverSlot, 40, SlotActionType.SWAP, player)

        if (!isInInventoryScreen) {
            network.sendPacket(CloseHandledScreenC2SPacket(0))
        }
    }

    private fun isItemValid(stack: ItemStack): Boolean {
        return !stack.isEmpty && stack.item == Items.TOTEM_OF_UNDYING
    }

}
