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
package net.ccbluex.liquidbounce.utils.item

import net.minecraft.item.ArmorItem
import net.minecraft.item.ItemStack

class ArmorPiece(val itemStack: ItemStack, val slot: Int) {
    val entitySlotId: Int
        get() = (itemStack.item as ArmorItem).slotType.entitySlotId
    val inventorySlot: Int
        get() = 36 + entitySlotId
    val isAlreadyEquipped: Boolean
        get() = slot in 36..39
    val isReachableByHand: Boolean
        get() = isInHotbar(slot)
}
