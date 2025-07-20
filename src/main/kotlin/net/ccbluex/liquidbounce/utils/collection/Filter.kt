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
package net.ccbluex.liquidbounce.utils.collection

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.inventory.HotbarItemSlot
import net.ccbluex.liquidbounce.utils.inventory.Slots
import net.ccbluex.liquidbounce.utils.item.getBlock
import net.minecraft.block.Block

enum class Filter(override val choiceName: String) : NamedChoice {
    WHITELIST("Whitelist") {
        override fun <T> invoke(item: T, collection: Collection<T>): Boolean = item in collection
    },
    BLACKLIST("Blacklist") {
        override fun <T> invoke(item: T, collection: Collection<T>): Boolean = item !in collection
    };

    /**
     * @return true if the [item] should be included according to the filter.
     */
    abstract operator fun <T> invoke(item: T, collection: Collection<T>): Boolean
}

fun Filter.getSlot(blocks: Set<Block>, offhand: Boolean = true): HotbarItemSlot? {
    val slots = if (offhand) Slots.OffhandWithHotbar else Slots.Hotbar

    return slots.find {
        val block = it.itemStack.getBlock() ?: return@find false
        this(block, blocks)
    }
}
