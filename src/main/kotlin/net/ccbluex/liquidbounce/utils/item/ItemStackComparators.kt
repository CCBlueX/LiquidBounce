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

import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.sorting.compareByCondition
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import kotlin.math.absoluteValue

object PreferSolidBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultState

            return@compareByCondition defaultState.isSolid
        }
    }

}

object PreferFullCubeBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultState

            return@compareByCondition defaultState.isFullCube(mc.world!!, BlockPos.ORIGIN)
        }
    }

}


object PreferLessSlipperyBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val o2Block = (o2.item as BlockItem).block
        val o1Block = (o1.item as BlockItem).block

        return o2Block.slipperiness.compareTo(o1Block.slipperiness)
    }

}



/**
 * We want to place average hard blocks such as stone or wood. We don't want to use obsidian or leaves first
 * (high/low hardness).
 */
object PreferAverageHardBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val o1HardnessDist = hardnessDist(o1)
        val o2HardnessDist = hardnessDist(o2)

        return o2HardnessDist.compareTo(o1HardnessDist)
    }

    private fun hardnessDist(stack: ItemStack): Double {
        val defaultState = (stack.item as BlockItem).block.defaultState
        val hardness = defaultState.getHardness(mc.world!!, BlockPos.ORIGIN)

        return (1.5 - hardness).absoluteValue
    }

}

class PreferStackSize(val higher: Boolean) : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val o1Size = o1.count
        val o2Size = o2.count

        return if (higher) o1Size.compareTo(o2Size)
        else o2Size.compareTo(o1Size)
    }

}
