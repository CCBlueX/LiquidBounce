package net.ccbluex.liquidbounce.features.module.modules.world.autoFarm

import net.ccbluex.liquidbounce.utils.block.AbstractBlockLocationTracker
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.BlockState
import net.minecraft.block.FarmlandBlock
import net.minecraft.block.SoulSandBlock
import net.minecraft.util.math.BlockPos

enum class TrackedState {
    Destroy,
    Farmland,
    Soulsand
}

object BlockTracker : AbstractBlockLocationTracker<TrackedState>() {
    override fun getStateFor(pos: BlockPos, state: BlockState): TrackedState? {
        val block = state.block
        if (block is FarmlandBlock && ModuleAutoFarm.hasAirAbove(pos))
            return TrackedState.Farmland

        if (block is SoulSandBlock && ModuleAutoFarm.hasAirAbove(pos))
            return TrackedState.Soulsand

        if (ModuleAutoFarm.isTargeted(state, pos))
            return TrackedState.Destroy


        val stateBellow = pos.down().getState() ?: return null

        if(stateBellow.isAir) return null

        val blockBellow = stateBellow.block

        if (blockBellow is FarmlandBlock){
            val targetBlockPos = TargetBlockPos(pos.down())
            if (state.isAir){
                this.trackedBlockMap[targetBlockPos] = TrackedState.Farmland
                return null
            } else {
                this.trackedBlockMap.remove(targetBlockPos)
            }
        } else if (blockBellow is SoulSandBlock){
            val targetBlockPos = TargetBlockPos(pos.down())
            if(state.isAir){
                this.trackedBlockMap[targetBlockPos] = TrackedState.Soulsand
                return null
            } else {
                this.trackedBlockMap.remove(targetBlockPos)
            }
        }

        return null
    }

}
