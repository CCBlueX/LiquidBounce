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

import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.FarmBlock
import net.minecraft.world.level.block.SoulSandBlock
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction

enum class AutoFarmTrackedState {
    SHOULD_BE_DESTROYED,
    CAN_USE_BONE_MEAL,
    FARMLAND,
    SOUL_SAND,
}

object AutoFarmBlockTracker : AbstractBlockLocationTracker.State2BlockPos<AutoFarmTrackedState>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedState? {
        // Should be destroyed? e.g., Melon block, Pumpkin block
        if (pos.readyForHarvest(state)) {
            return AutoFarmTrackedState.SHOULD_BE_DESTROYED
        }

        val cache = BlockPos.MutableBlockPos()
        if (state.isAir) {
            // If this position is air, check placeable position below
            val blockBelow = cache.setWithOffset(pos, Direction.DOWN).getState()?.block ?: return null

            when (blockBelow) {
                is FarmBlock -> track(cache, AutoFarmTrackedState.FARMLAND)
                is SoulSandBlock -> track(cache, AutoFarmTrackedState.SOUL_SAND)
            }

            // Air itself should be untracked
            return null
        }

        val blockBelow = cache.setWithOffset(pos, Direction.DOWN).getState()?.block
        if (blockBelow is SoulSandBlock || blockBelow is FarmBlock) {
            untrack(cache)
        }

        if (pos.canUseBoneMeal(state)) {
            return AutoFarmTrackedState.CAN_USE_BONE_MEAL
        }

        val block = state.block

        // Check if air above
        return if (cache.setWithOffset(pos, Direction.UP).getState()?.isAir == true) {
            when (block) {
                is FarmBlock -> AutoFarmTrackedState.FARMLAND
                is SoulSandBlock -> AutoFarmTrackedState.SOUL_SAND
                else -> null
            }
        } else {
            null
        }
    }

}

