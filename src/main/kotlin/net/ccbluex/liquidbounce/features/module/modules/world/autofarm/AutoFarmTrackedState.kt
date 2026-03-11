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

package net.ccbluex.liquidbounce.features.module.modules.world.autofarm

import net.ccbluex.fastutil.enumSetOf
import net.ccbluex.fastutil.objectArraySetOf
import net.ccbluex.liquidbounce.config.types.list.Tagged
import net.ccbluex.liquidbounce.utils.block.DIRECTIONS_HORIZONTAL
import net.ccbluex.liquidbounce.utils.client.world
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.item.Item
import net.minecraft.world.item.Items
import net.minecraft.world.level.block.state.BlockState

sealed interface AutoFarmTrackedState {
    enum class Plantable(
        override val tag: String,
        val items: Collection<Item>,
    ) : AutoFarmTrackedState, Tagged {
        FARMLAND(
            "Farmland",
            objectArraySetOf(Items.WHEAT_SEEDS, Items.BEETROOT_SEEDS, Items.CARROT, Items.POTATO),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.supportsCrops
        },
        SOUL_SAND(
            "SoulSand",
            setOf(Items.NETHER_WART),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.supportsNetherWart
        },
        JUNGLE_LOGS(
            "JungleLogs",
            setOf(Items.COCOA_BEANS),
        ) {
            override fun isBlockMatches(state: BlockState): Boolean = state.supportsCocoa

            override fun findPlantableNeighbors0(pos: BlockPos, state: BlockState): Collection<Direction> {
                val result = enumSetOf<Direction>()
                val mutable = BlockPos.MutableBlockPos()
                for (dir in DIRECTIONS_HORIZONTAL) {
                    mutable.setWithOffset(pos, dir)
                    if (world.getBlockState(mutable).isAir) {
                        result.add(dir)
                    }
                }
                return result
            }
        };

        abstract fun isBlockMatches(state: BlockState): Boolean

        protected open fun findPlantableNeighbors0(pos: BlockPos, state: BlockState): Collection<Direction> {
            val above = pos.above()
            return if (world.getBlockState(above).isAir) {
                setOf(Direction.UP)
            } else {
                setOf()
            }
        }

        fun findPlantableSides(pos: BlockPos, state: BlockState): Collection<Direction> {
            return if (isBlockMatches(state)) {
                findPlantableNeighbors0(pos, state)
            } else {
                setOf()
            }
        }
    }

    object Bonemealable : AutoFarmTrackedState

    object ReadyForHarvest : AutoFarmTrackedState

}
