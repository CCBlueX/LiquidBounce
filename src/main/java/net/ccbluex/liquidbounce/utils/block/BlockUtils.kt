/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.utils.MinecraftInstance
import net.minecraft.block.Block
import net.minecraft.block.BlockContainer
import net.minecraft.block.BlockFalling
import net.minecraft.block.BlockWorkbench
import net.minecraft.block.state.IBlockState
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos

typealias Collidable = (Block?) -> Boolean

object BlockUtils : MinecraftInstance() {
    /**
     * Get block from [blockPos]
     */
    fun getBlock(blockPos: BlockPos) = getState(blockPos)?.block

    /**
     * Get material from [blockPos]
     */
    fun getMaterial(blockPos: BlockPos) = getState(blockPos)?.block?.material

    /**
     * Check [blockPos] is replaceable
     */
    fun isReplaceable(blockPos: BlockPos) = getMaterial(blockPos)?.isReplaceable ?: false

    /**
     * Get state from [blockPos]
     */
    fun getState(blockPos: BlockPos) = mc.theWorld?.getBlockState(blockPos)

    /**
     * Check if [blockPos] is clickable
     */
    fun canBeClicked(blockPos: BlockPos): Boolean {
        val state = getState(blockPos) ?: return false
        val block = state.block ?: return false

        return block.canCollideCheck(state, false) && blockPos in mc.theWorld.worldBorder && !block.material.isReplaceable
                && !block.hasTileEntity(state) && isFullBlock(blockPos, state)
                && block !is BlockFalling && block !is BlockContainer && block !is BlockWorkbench
    }

    /**
     * Get block name by [id]
     */
    fun getBlockName(id: Int): String = Block.getBlockById(id).localizedName

    /**
     * Check if block is full block
     */
    fun isFullBlock(blockPos: BlockPos, blockState: IBlockState? = null): Boolean {
        val state = blockState ?: getState(blockPos) ?: return false

        val box = state.block.getCollisionBoundingBox(mc.theWorld, blockPos, state) ?: return false
        return box.maxX - box.minX == 1.0 && box.maxY - box.minY == 1.0 && box.maxZ - box.minZ == 1.0
    }

    fun isFullBlock(block: Block) = block.blockBoundsMaxX == 1.0 && block.blockBoundsMaxY == 1.0 && block.blockBoundsMaxZ == 1.0

    /**
     * Get distance to center of [blockPos]
     */
    fun getCenterDistance(blockPos: BlockPos) =
            mc.thePlayer.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    /**
     * Search blocks around the player in a specific [radius]
     */
    fun searchBlocks(radius: Int): Map<BlockPos, Block> {
        val blocks = mutableMapOf<BlockPos, Block>()

        val thePlayer = mc.thePlayer ?: return blocks

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = BlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y, thePlayer.posZ.toInt() + z)
                    val block = getBlock(blockPos) ?: continue

                    blocks[blockPos] = block
                }
            }
        }

        return blocks
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    fun collideBlock(axisAlignedBB: AxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.thePlayer

        for (x in thePlayer.entityBoundingBox.minX.toInt() until thePlayer.entityBoundingBox.maxX.toInt() + 1) {
            for (z in thePlayer.entityBoundingBox.minZ.toInt() until thePlayer.entityBoundingBox.maxZ.toInt() + 1) {
                val block = getBlock(BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))

                if (!collide(block))
                    return false
            }
        }

        return true
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    fun collideBlockIntersects(axisAlignedBB: AxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.thePlayer
        val world = mc.theWorld

        for (x in thePlayer.entityBoundingBox.minX.toInt() until thePlayer.entityBoundingBox.maxX.toInt() + 1) {
            for (z in thePlayer.entityBoundingBox.minZ.toInt() until thePlayer.entityBoundingBox.maxZ.toInt() + 1) {
                val blockPos = BlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
                val block = getBlock(blockPos)

                if (collide(block)) {
                    val boundingBox = getState(blockPos)?.let { block?.getCollisionBoundingBox(world, blockPos, it) }
                            ?: continue

                    if (thePlayer.entityBoundingBox.intersectsWith(boundingBox))
                        return true
                }
            }
        }
        return false
    }

}