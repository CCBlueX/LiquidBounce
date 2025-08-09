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
import net.minecraft.block.BlockState
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.util.math.BlockPos
import net.minecraft.util.math.Direction

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

        val cache = BlockPos.Mutable()
        if (state.isAir) {
            // If this position is air, check placeable position below
            val blockBelow = cache.set(pos, Direction.DOWN).getState()?.block ?: return null

            when (blockBelow) {
                is FarmlandBlock -> track(cache, AutoFarmTrackedState.FARMLAND)
                is SoulSandBlock -> track(cache, AutoFarmTrackedState.SOUL_SAND)
            }

            // Air itself should be untracked
            return null
        }

        val blockBelow = cache.set(pos, Direction.DOWN).getState()?.block
        if (blockBelow is SoulSandBlock || blockBelow is FarmlandBlock) {
            untrack(cache)
        }

        if (pos.canUseBoneMeal(state)) {
            return AutoFarmTrackedState.CAN_USE_BONE_MEAL
        }

        val block = state.block

        // Check if air above
        return if (cache.set(pos, Direction.UP).getState()?.isAir == true) {
            when (block) {
                is FarmlandBlock -> AutoFarmTrackedState.FARMLAND
                is SoulSandBlock -> AutoFarmTrackedState.SOUL_SAND
                else -> null
            }
        } else {
            null
        }
    }

}

