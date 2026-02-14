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
package net.ccbluex.liquidbounce.utils.item

import net.ccbluex.liquidbounce.features.module.modules.world.scaffold.ScaffoldBlockItemSelection
import net.ccbluex.liquidbounce.utils.client.mc
import net.ccbluex.liquidbounce.utils.sorting.ComparatorChain
import net.ccbluex.liquidbounce.utils.sorting.compareValueByCondition
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.Block
import java.util.function.ToIntFunction
import kotlin.math.abs
import kotlin.math.absoluteValue

fun Comparator<ItemStack>.asHolderComparator(): Comparator<ItemStackHolder> =
    Comparator { a, b -> this.compare(a.itemStack, b.itemStack) }

fun comparingEnchantmentLevel(key: ResourceKey<Enchantment>): Comparator<ItemStack> =
    Comparator.comparingInt(ToIntFunction { it.getEnchantment(key) })

object PreferFavourableBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            !ScaffoldBlockItemSelection.isBlockUnfavourable(it)
        }
    }
}

object PreferSolidBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultBlockState()

            defaultState.isRedstoneConductor(mc.level!!, BlockPos.ZERO)
        }
    }
}

object PreferFullCubeBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValueByCondition(o1, o2) {
            val defaultState = (it.item as BlockItem).block.defaultBlockState()

            defaultState.isCollisionShapeFullBlock(mc.level!!, BlockPos.ZERO)
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
        compareBy { it.friction.toDouble() },
        compareBy { abs(it.jumpFactor - 1.0) },
        compareBy { abs(it.speedFactor - 1.0) },
    )

    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return this.chain.compare((o1.item as BlockItem).block, (o2.item as BlockItem).block)
    }

}


/**
 * We want to place average hard blocks such as stone or wood. We don't want to use obsidian or leaves first
 * (high/low hardness).
 *
 * @param neutralRange if enabled, there is a range of hardness values which are accepted as *good*. If disabled we
 * prefer the closest to the *ideal* hardness value.
 */
class PreferAverageHardBlocks(private val neutralRange: Boolean) : Comparator<ItemStack> {
    companion object {
        private val GOOD_HARDNESS_RANGE = 0.8..2.0
        private const val IDEAL_HARDNESS = 1.7
    }

    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val o1HardnessDist = hardnessDist(o1)
        val o2HardnessDist = hardnessDist(o2)

        return o2HardnessDist.compareTo(o1HardnessDist)
    }

    private fun hardnessDist(stack: ItemStack): Double {
        val defaultState = (stack.item as BlockItem).block.defaultBlockState()
        val hardness = defaultState.getDestroySpeed(mc.level!!, BlockPos.ZERO)

        // If neutral range is enabled, items with a specific range of hardness values should be considered ideal.
        if (this.neutralRange && hardness in GOOD_HARDNESS_RANGE) {
            return 0.0
        }

        return (IDEAL_HARDNESS - hardness).absoluteValue
    }

}

class PreferStackSize private constructor(val higher: Boolean) : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        val o1Size = o1.count
        val o2Size = o2.count

        return if (higher) {
            o1Size.compareTo(o2Size)
        } else {
            o2Size.compareTo(o1Size)
        }
    }

    companion object {
        /**
         * Fewer items first
         */
        @JvmField
        val PREFER_FEWER = PreferStackSize(true)

        /**
         * More items first
         */
        @JvmField
        val PREFER_MORE = PreferStackSize(false)
    }
}
