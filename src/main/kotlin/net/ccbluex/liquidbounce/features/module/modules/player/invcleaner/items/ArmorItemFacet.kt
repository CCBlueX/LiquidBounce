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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece

/**
 * @param stacksToKeep armor items which should be kept since they might be strong in future situations
 */
class ArmorItemFacet(
    itemSlot: ItemSlot,
    private val stacksToKeep: List<ItemSlot>,
    private val armorComparator: ArmorComparator
) : ItemFacet(itemSlot) {
    private val armorPiece = ArmorPiece(itemSlot)

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ARMOR, armorPiece.entitySlotId)

    override fun shouldKeep(): Boolean {
        return this.stacksToKeep.contains(this.itemSlot)
    }

    override fun compareTo(other: ItemFacet): Int {
        return armorComparator.compare(this.armorPiece, (other as ArmorItemFacet).armorPiece)
    }
}
