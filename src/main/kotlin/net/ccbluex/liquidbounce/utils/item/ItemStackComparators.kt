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
import net.minecraft.core.BlockPos
import net.minecraft.resources.ResourceKey
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.ItemStack
import net.minecraft.world.item.enchantment.Enchantment
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import java.util.function.ToIntFunction
import kotlin.math.abs
import kotlin.math.absoluteValue

fun Comparator<ItemStack>.asHolderComparator(): Comparator<ItemStackHolder> =
    Comparator { a, b -> this.compare(a.itemStack, b.itemStack) }

fun comparingEnchantmentLevel(key: ResourceKey<Enchantment>): Comparator<ItemStack> =
    Comparator.comparingInt(ToIntFunction { it.getEnchantment(key) })

@JvmField
val COMPARING_DESCRIPTION_ID: Comparator<ItemStack> = Comparator.comparing { it.item.descriptionId }

private fun ItemStack.block(): Block = (this.item as BlockItem).block

private fun ItemStack.defaultBlockState(): BlockState = this.block().defaultBlockState()

object PreferFavourableBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValuesBy(o1, o2) {
            !ScaffoldBlockItemSelection.isBlockUnfavourable(it)
        }
    }
}

object PreferSolidBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValuesBy(o1, o2) {
            it.defaultBlockState().isRedstoneConductor(mc.level!!, BlockPos.ZERO)
        }
    }
}

object PreferFullCubeBlocks : Comparator<ItemStack> {
    override fun compare(o1: ItemStack, o2: ItemStack): Int {
        return compareValuesBy(o1, o2) {
            it.defaultBlockState().isCollisionShapeFullBlock(mc.level!!, BlockPos.ZERO)
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
        return this.chain.compare(o1.block(), o2.block())
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
        val hardness = stack.defaultBlockState().getDestroySpeed(mc.level!!, BlockPos.ZERO)

        // If neutral range is enabled, items with a specific range of hardness values should be considered ideal.
        if (this.neutralRange && hardness in GOOD_HARDNESS_RANGE) {
            return 0.0
        }

        return (IDEAL_HARDNESS - hardness).absoluteValue
    }

}

object PreferStackSize {
    @JvmField
    val PREFER_FEWER: Comparator<ItemStack> = Comparator.comparingInt(ToIntFunction(ItemStack::getCount))

    @JvmField
    val PREFER_MORE: Comparator<ItemStack> = PREFER_FEWER.reversed()
}
