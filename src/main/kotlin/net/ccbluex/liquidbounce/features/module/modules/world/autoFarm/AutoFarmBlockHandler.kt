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
        if (block is FarmlandBlock && ModuleAutoFarm.hasAirAbove(pos))
            return AutoFarmTrackedStates.Farmland

        if (block is SoulSandBlock && ModuleAutoFarm.hasAirAbove(pos))
            return AutoFarmTrackedStates.Soulsand

        if (ModuleAutoFarm.isTargeted(state, pos))
            return AutoFarmTrackedStates.Destroy


        val stateBellow = pos.down().getState() ?: return null

        if(stateBellow.isAir) return null

        val blockBellow = stateBellow.block

        if (blockBellow is FarmlandBlock){
            val targetBlockPos = TargetBlockPos(pos.down())
            if (state.isAir){
                this.trackedBlockMap[targetBlockPos] = AutoFarmTrackedStates.Farmland
                return null
            } else {
                this.trackedBlockMap.remove(targetBlockPos)
            }
        } else if (blockBellow is SoulSandBlock){
            val targetBlockPos = TargetBlockPos(pos.down())
            if(state.isAir){
                this.trackedBlockMap[targetBlockPos] = AutoFarmTrackedStates.Soulsand
                return null
            } else {
                this.trackedBlockMap.remove(targetBlockPos)
            }
        }

        return null
    }

}
