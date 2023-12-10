package net.ccbluex.liquidbounce.features.module.modules.world.autoFarm

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

object AutoFarmBlockTracker : AbstractBlockLocationTracker<AutoFarmTrackedStates>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): AutoFarmTrackedStates? {
        val block = state.block

        if (ModuleAutoFarm.isTargeted(state, pos))
            return AutoFarmTrackedStates.Destroy

        val stateBellow = pos.down().getState() ?: return null

        if (stateBellow.isAir) return null

        val blockBellow = stateBellow.block

        if (blockBellow is FarmlandBlock) {
            handlePlaceableBlock(pos, state, AutoFarmTrackedStates.Farmland)
        } else if (blockBellow is SoulSandBlock) {
            handlePlaceableBlock(pos, state, AutoFarmTrackedStates.Soulsand)
        }
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
        val targetBlockPos = TargetBlockPos(pos.down())
        if (state.isAir){
            // If there is no air above, add it
            this.trackedBlockMap[targetBlockPos] = trackedState
        } else {
            // If there is no air above, we want to remove it
            this.trackedBlockMap.remove(targetBlockPos)
        }
    }
}

