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
package net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.items

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.ItemStack

open class WeightedItem(val itemSlot: ItemSlot) : Comparable<WeightedItem> {
    open val category: ItemCategory
        get() = ItemCategory(ItemType.NONE, 0)

    val itemStack: ItemStack
        get() = this.itemSlot.itemStack

    val isInHotbar: Boolean
        get() = this.itemSlot.slotType == ItemSlotType.HOTBAR || this.itemSlot.slotType == ItemSlotType.OFFHAND

    open fun isSignificantlyBetter(other: WeightedItem): Boolean {
        return false
    }

    override fun compareTo(other: WeightedItem): Int = compareByCondition(this, other, WeightedItem::isInHotbar)
}
