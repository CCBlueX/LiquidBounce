/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.GenericItemType
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemFunction
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.item.ItemStackHolder

open class ItemFacet(val itemSlot: ItemSlot) : Comparable<ItemFacet>, ItemStackHolder by itemSlot {
    open val category: ItemCategory
        get() = ItemCategory(GenericItemType.ANY_ITEM, itemSlot.itemStack.item)

    open val providedItemFunctions: List<ProvidedFunction>
        get() = emptyList()

    val isInHotbar: Boolean
        get() = this.itemSlot.slotType == ItemSlot.Type.HOTBAR || this.itemSlot.slotType == ItemSlot.Type.OFFHAND

    /**
     * Should this item be kept, even if it is not allocated to any slot?
     */
    open fun shouldKeep(): Boolean = false

    override fun compareTo(other: ItemFacet): Int = compareValuesBy<ItemFacet>(this, other, ItemFacet::isInHotbar)

    /**
     * Example:
     * - Bow -> (BOW_LIKE, 1)
     * - Porkchop -> (FOOD, <amount of hunger points it regenerates>)
     *
     * @param amount The amount of the function this item gives.
     */
    data class ProvidedFunction(val type: ItemFunction, val amount: Int)
}
