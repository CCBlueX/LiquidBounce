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
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.item.ArmorComparator
import net.ccbluex.liquidbounce.utils.item.ArmorPiece

/**
 * @param fullArmorKit the armor kit, if the other armor pieces were already the best ones expected (i.e. full dia)
 */
class ArmorItemFacet(
    itemSlot: ItemSlot,
    private val fullArmorKit: List<ItemSlot>,
    private val armorComparator: ArmorComparator
) : ItemFacet(itemSlot) {
    private val armorPiece = ArmorPiece(itemSlot)

    override val category: ItemCategory
        get() = ItemCategory(ItemType.ARMOR, armorPiece.entitySlotId)

    override fun shouldKeep(): Boolean {
        // Sometimes there are situations where armor pieces are not the best ones with the current armor, but become
        // the best ones as soon as we upgrade one of the other armor pieces. In those cases we don't want to miss out
        // on this armor piece in the future thus we keep it.
        return this.fullArmorKit.contains(this.itemSlot)
    }

    override fun compareTo(other: ItemFacet): Int {
        return armorComparator.compare(this.armorPiece, (other as ArmorItemFacet).armorPiece)
    }
}
