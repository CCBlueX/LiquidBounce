/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
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

import it.unimi.dsi.fastutil.objects.ObjectIntPair
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemFunction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemSlotType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.kotlin.Priority
import net.ccbluex.liquidbounce.utils.sorting.compareValueByCondition
import net.minecraft.item.ItemStack

open class ItemFacet(val itemSlot: ItemSlot) : Comparable<ItemFacet> {
    open val category: ItemCategory
        get() = ItemCategory(ItemType.NONE, 0)

    open val providedItemFunctions: List<ObjectIntPair<ItemFunction>>
        get() = emptyList()

    val itemStack: ItemStack
        get() = this.itemSlot.itemStack

    val isInHotbar: Boolean
        get() = this.itemSlot.slotType == ItemSlotType.HOTBAR || this.itemSlot.slotType == ItemSlotType.OFFHAND

    open fun isSignificantlyBetter(other: ItemFacet): Boolean {
        return false
    }

    /**
     * Should this item be kept, even if it is not allocated to any slot?
     */
    open fun shouldKeep(): Boolean = false

    override fun compareTo(other: ItemFacet): Int = compareValueByCondition(this, other, ItemFacet::isInHotbar)
}
