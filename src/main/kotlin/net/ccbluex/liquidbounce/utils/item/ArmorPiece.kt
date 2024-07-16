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
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.minecraft.entity.EquipmentSlot
import net.minecraft.item.ArmorItem

class ArmorPiece(val itemSlot: ItemSlot) {
    val slotType: EquipmentSlot
        get() = (itemSlot.itemStack.item as ArmorItem).slotType
    val entitySlotId: Int
        get() = this.slotType.entitySlotId
    val inventorySlot: Int
        get() = 36 + entitySlotId
    val isAlreadyEquipped: Boolean
        get() = itemSlot.slotType == ItemSlotType.ARMOR
    val isReachableByHand: Boolean
        get() = itemSlot.slotType == ItemSlotType.HOTBAR

    val toughness: Float
        get() = (itemSlot.itemStack.item as ArmorItem).toughness
    val defensePoints: Float
        get() {
            val item = itemSlot.itemStack.item as ArmorItem

            return item.material.value().getProtection(item.type).toFloat()
        }
}
