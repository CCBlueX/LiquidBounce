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

enum class AutoFarmTrackedStates {
    Destroy,
    Farmland,
    Soulsand
}

object AutoFarmBlockTracker : AbstractBlockLocationTracker.State2BlockPos<AutoFarmTrackedStates>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedStates? {
        if (ModuleAutoFarm.isTargeted(state, pos)) {
            return AutoFarmTrackedStates.Destroy
        }

        val stateBellow = pos.down().getState() ?: return null

        if (stateBellow.isAir) return null

        val blockBellow = stateBellow.block

        when (blockBellow) {
            is FarmlandBlock -> handlePlaceableBlock(pos, state, AutoFarmTrackedStates.Farmland)
            is SoulSandBlock -> handlePlaceableBlock(pos, state, AutoFarmTrackedStates.Soulsand)
        }

        val block = state.block

        if (ModuleAutoFarm.hasAirAbove(pos)) {
            return when (block) {
                is FarmlandBlock -> AutoFarmTrackedStates.Farmland
                is SoulSandBlock -> AutoFarmTrackedStates.Soulsand
                else -> null
            }
        }
        return null
    }


    private fun handlePlaceableBlock(pos: BlockPos, state: BlockState, trackedState: AutoFarmTrackedStates) {
        val targetBlockPos = pos.down()
        if (state.isAir) {
            // If there is no air above, add it
            track(targetBlockPos, trackedState)
        } else {
            // If there is no air above, we want to remove it
            untrack(targetBlockPos)
        }
    }
}

