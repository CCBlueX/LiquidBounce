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

import net.ccbluex.liquidbounce.config.types.NamedChoice
import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.SoulSandBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.tags.BlockTags

sealed interface AutoFarmTrackedState {
    object ReadyForHarvest : AutoFarmTrackedState
    object Bonemealable : AutoFarmTrackedState

    enum class Plantable(override val choiceName: String) : AutoFarmTrackedState, NamedChoice {
        FARM("Farmland"),
        SOUL_SAND("SoulSand"),
        JUNGLE_LOGS("JungleLogs");
    }
}

object AutoFarmBlockTracker : AbstractBlockLocationTracker.State2BlockPos<AutoFarmTrackedState>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedState? {
        // Should be destroyed? e.g., Melon block, Pumpkin block
        if (pos.readyForHarvest(state)) {
            return AutoFarmTrackedState.ReadyForHarvest
        }

        val cache = BlockPos.MutableBlockPos()
        if (state.isAir) {
            // If this position is air, check placeable position below
            val blockBelow = cache.setWithOffset(pos, Direction.DOWN).getState()?.block ?: return null

            when (blockBelow) {
                is FarmBlock -> track(cache, AutoFarmTrackedState.Plantable.FARM)
                is SoulSandBlock -> track(cache, AutoFarmTrackedState.Plantable.SOUL_SAND)
            }

            // Air itself should be untracked
            return null
        } else if (state.`is`(BlockTags.JUNGLE_LOGS)) {
            return AutoFarmTrackedState.Plantable.JUNGLE_LOGS
        }

        val blockBelow = cache.setWithOffset(pos, Direction.DOWN).getState()?.block
        if (blockBelow is SoulSandBlock || blockBelow is FarmBlock) {
            untrack(cache)
        }

        if (pos.canUseBoneMeal(state)) {
            return AutoFarmTrackedState.Bonemealable
        }

        val block = state.block

        // Check if air above
        return if (cache.setWithOffset(pos, Direction.UP).getState()?.isAir == true) {
            when (block) {
                is FarmBlock -> AutoFarmTrackedState.Plantable.FARM
                is SoulSandBlock -> AutoFarmTrackedState.Plantable.SOUL_SAND
                else -> null
            }
        } else {
            null
        }
    }

}

