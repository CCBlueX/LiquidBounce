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

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ModuleScaffold
import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareValueByCondition
import net.minecraft.block.Block
import net.minecraft.item.BlockItem
import net.minecraft.item.ItemStack
import net.minecraft.util.math.BlockPos
import kotlin.math.abs
import kotlin.math.absoluteValue

object PreferFavourableBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            return@compareValueByCondition !ScaffoldBlockItemSelection.isBlockUnfavourable(it)
        }
    }

}

object PreferSolidBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultState

            return@compareValueByCondition defaultState.isSolid
        }
    }

}

object PreferFullCubeBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultState

            return@compareValueByCondition defaultState.isFullCube(mc.world!!, BlockPos.ORIGIN)
        }
    }

}

/**
 * This predicate sorts blocks by
 * 1. least slipperiness
 * 2. nearest jump velocity modifier to 1.0
 * 3. nearest velocity jump modifier to 1.0
 */
object PreferWalkableBlocks : Comparator<ItemStack> {
    private val chain = ComparatorChain<Block>(
        compareBy { it.slipperiness.toDouble() },
        compareBy { abs(it.jumpVelocityMultiplier - 1.0) },
        compareBy { abs(it.velocityMultiplier - 1.0) },
    )

    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return this.chain.compare((o1.item as BlockItem).block, (o2.item as BlockItem).block)
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

        return if (higher) {
            o1Size.compareTo(o2Size)
        } else {
            o2Size.compareTo(o1Size)
        }
    }

}
