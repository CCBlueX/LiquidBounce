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

package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import it.unimi.dsi.fastutil.longs.LongCollection
import net.ccbluex.fastutil.longListOf
import net.ccbluex.fastutil.longMutableListOf
import net.ccbluex.fastutil.objectArraySetOf
import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_HORIZONTAL
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.tags.BlockTags
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.SoulSandBlock
import net.minecraft.world.level.block.state.BlockState

sealed interface AutoFarmTrackedState {
    enum class Plantable(
        override val choiceName: String,
        val items: Collection<Item>,
    ) : AutoFarmTrackedState, NamedChoice {
        FARM(
            "Farmland",
            objectArraySetOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.block is FarmBlock
        },
        SOUL_SAND(
            "SoulSand",
            setOf(Items.NETHER_WART),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.block is SoulSandBlock
        },
        JUNGLE_LOGS(
            "JungleLogs",
            setOf(Items.COCOA_BEANS),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.`is`(BlockTags.JUNGLE_LOGS)

            override fun findPlantableNeighbors0(pos: BlockPos, state: BlockState): LongCollection {
                val result = longMutableListOf()
                val mutable = BlockPos.MutableBlockPos()
                for (dir in DIRECTIONS_HORIZONTAL) {
                    mutable.set(pos).move(dir)
                    if (world.getBlockState(mutable).isAir) {
                        result.add(mutable.asLong())
                    }
                }
                return result
            }
        };

        abstract fun isBlockMatches(state: BlockState): Boolean

        protected open fun findPlantableNeighbors0(pos: BlockPos, state: BlockState): LongCollection {
            val above = pos.above()
            return if (world.getBlockState(above).isAir) {
                longListOf(above.asLong())
            } else {
                longListOf()
            }
        }

        fun findPlantableNeighbors(pos: BlockPos, state: BlockState): LongCollection {
            return if (isBlockMatches(state)) {
                findPlantableNeighbors0(pos, state)
            } else {
                longListOf()
            }
        }
    }

    object Bonemealable : AutoFarmTrackedState

    object ReadyForHarvest : AutoFarmTrackedState

}
