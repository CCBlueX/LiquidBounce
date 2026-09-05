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

import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemCategory
import net.ccbluex.liquidbounce.features.module.modules.player.invcleaner.ItemType
import net.ccbluex.liquidbounce.utils.item.PreferAverageHardBlocks
import net.ccbluex.liquidbounce.utils.item.PreferFavourableBlocks
import net.ccbluex.liquidbounce.utils.item.PreferFullCubeBlocks
import net.ccbluex.liquidbounce.utils.item.PreferMoreBlocksFuzzy
import net.ccbluex.liquidbounce.utils.item.PreferSolidBlocks
import net.ccbluex.liquidbounce.utils.item.PreferWalkableBlocks
import net.ccbluex.liquidbounce.utils.item.asHolderComparator
import net.ccbluex.liquidbounce.utils.inventory.ItemSlot
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain

class BlockItemFacet(itemSlot: ItemSlot) : ItemFacet(itemSlot) {
    companion object {
        private val COMPARATOR =
            ComparatorChain<BlockItemFacet>(
                // First, usability gates: a non-favourable block (slab, slime, entity block...) or a
                // non-full-cube must never be kept over a normal full block, regardless of count.
                PreferFavourableBlocks.asHolderComparator(),
                PreferFullCubeBlocks.asHolderComparator(),
                // Then COUNT — among usable full-cube blocks the fullest stack wins. This is what stops a
                // big stack of glass (full cube, favourable) being ignored for a tiny stack of a "nicer"
                // block like stone, just because glass is not a redstone conductor. Glass — stained or
                // not — is a normal scaffolding block here.
                PreferMoreBlocksFuzzy().asHolderComparator(),
                // Soft quality preferences only break ties between similarly-sized usable stacks.
                PreferSolidBlocks.asHolderComparator(),
                PreferWalkableBlocks.asHolderComparator(),
                PreferAverageHardBlocks(neutralRange = true).asHolderComparator(),
                PREFER_ITEMS_IN_HOTBAR,
                PreferAverageHardBlocks(neutralRange = false).asHolderComparator(),
                STABILIZE_COMPARISON,
            )
    }

    override val category: ItemCategory
        get() = ItemType.BLOCK.defaultCategory

    override fun compareTo(other: ItemFacet): Int {
        return COMPARATOR.compare(this, other as BlockItemFacet)
    }
}
