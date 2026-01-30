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

package net.ccbluex.liquidbounce.features.module.modules.player.autoqueue

import net.ccbluex.fastutil.objectRBTreeSetOf
import net.ccbluex.liquidbounce.config.types.group.Mode
import net.ccbluex.liquidbounce.config.types.group.ModeValueGroup
import net.ccbluex.liquidbounce.utils.collection.itemSortedSetOf
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.Items
import java.util.function.Predicate

sealed class AutoQueueItemMode(
    final override val parent: ModeValueGroup<*>,
    name: String,
) : Mode(name), Predicate<ItemStack> {

    class ByName(parent: ModeValueGroup<*>) : AutoQueueItemMode(parent, "Name") {
        private val stackName by textList("Name", objectRBTreeSetOf("Paper"))
        override fun test(itemStack: ItemStack): Boolean {
            val string = itemStack.hoverName.string
            return stackName.any { it in string }
        }
    }

    class ByItem(parent: ModeValueGroup<*>) : AutoQueueItemMode(parent, "Item") {
        private val slotItem by items("Item", itemSortedSetOf(Items.PAPER))
        override fun test(itemStack: ItemStack): Boolean =
            slotItem.any { it === itemStack.item }
    }
}
