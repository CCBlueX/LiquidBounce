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

enum class AutoFarmTrackedStates {
    SHOULD_BE_DESTROYED,
    CAN_USE_BONE_MEAL,
    FARMLAND,
    SOUL_SAND,
}

object AutoFarmBlockTracker : AbstractBlockLocationTracker.State2BlockPos<AutoFarmTrackedStates>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedStates? {
        // Should be destroyed? e.g. Melon block, Pumpkin block
        if (ModuleAutoFarm.shouldBeDestroyed(state, pos)) {
            return AutoFarmTrackedStates.SHOULD_BE_DESTROYED
        }

        val cache = BlockPos.Mutable()
        // If this position is air, check placeable position below
        if (state.isAir) {
            val blockBelow = cache.set(pos, Direction.DOWN).getState()?.block ?: return null

            when (blockBelow) {
                is FarmlandBlock -> track(cache, AutoFarmTrackedStates.FARMLAND)
                is SoulSandBlock -> track(cache, AutoFarmTrackedStates.SOUL_SAND)
            }

            // Air itself should be untracked
            return null
        } else if (ModuleAutoFarm.canUseBoneMeal(state, pos)) {
            return AutoFarmTrackedStates.CAN_USE_BONE_MEAL
        } else {
            val block = state.block

            // Check if air above
            return if (cache.set(pos, Direction.UP).getState()?.isAir == true) {
                when (block) {
                    is FarmlandBlock -> AutoFarmTrackedStates.FARMLAND
                    is SoulSandBlock -> AutoFarmTrackedStates.SOUL_SAND
                    else -> null
                }
            } else {
                null
            }
        }
    }

}

