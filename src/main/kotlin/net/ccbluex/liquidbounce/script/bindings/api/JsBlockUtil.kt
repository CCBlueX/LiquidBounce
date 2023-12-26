package net.ccbluex.liquidbounce.script.bindings.api

import net.ccbluex.liquidbounce.utils.block.getBlock
import net.ccbluex.liquidbounce.utils.block.getState
import net.minecraft.block.BlockState
import net.minecraft.util.math.BlockPos

/**
 * Object used by the script API to provide an
 */
object JsBlockUtil {

    @JvmName("newBlockPos")
    fun newBlockPos(x: Int, y: Int, z: Int): BlockPos = BlockPos(x, y, z)

    @JvmName("toBlock")
    fun toBlock(blockPos: BlockPos) = blockPos.getBlock()

    @JvmName("toState")
    fun toState(blockPos: BlockPos): BlockState? = blockPos.getState()

}
