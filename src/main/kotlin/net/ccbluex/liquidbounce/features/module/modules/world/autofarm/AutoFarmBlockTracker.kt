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

import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.level.block.state.BlockState

object AutoFarmBlockTracker : AbstractBlockLocationTracker.State2BlockPos<AutoFarmTrackedState>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedState? {
        return when {
            pos.readyForHarvest(state) -> AutoFarmTrackedState.ReadyForHarvest

            pos.canUseBoneMeal(state) -> AutoFarmTrackedState.Bonemealable

            state.supportsCocoa -> AutoFarmTrackedState.Plantable.JUNGLE_LOGS

            else -> {
                val cache = BlockPos.MutableBlockPos()
                val stateBelow = cache.setWithOffset(pos, Direction.DOWN).getState() ?: return null
                if (state.isAir) {
                    // If this position is air, check placeable position below
                    when {
                        stateBelow.supportsCrops ->
                            track(cache, AutoFarmTrackedState.Plantable.FARMLAND)

                        stateBelow.supportsNetherWart ->
                            track(cache, AutoFarmTrackedState.Plantable.SOUL_SAND)
                    }

                    // Air itself should be untracked
                    return null
                } else if (stateBelow.supportsCrops || stateBelow.supportsNetherWart) {
                    // Not air, and block below is either farm or soul sand, untrack it
                    untrack(cache)
                }

                // Check if air above
                if (cache.setWithOffset(pos, Direction.UP).getState()?.isAir == true) {
                    when {
                        state.supportsCrops -> AutoFarmTrackedState.Plantable.FARMLAND
                        state.supportsNetherWart -> AutoFarmTrackedState.Plantable.SOUL_SAND
                        else -> null
                    }
                } else {
                    null
                }
            }
        }
    }

}

