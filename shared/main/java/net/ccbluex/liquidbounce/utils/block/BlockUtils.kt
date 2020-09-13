/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import kotlin.math.floor

typealias Collidable = (IBlock?) -> Boolean

object BlockUtils : MinecraftInstance() {

    /**
     * Get block from [blockPos]
     */
    @JvmStatic
    inline fun getBlock(blockPos: WBlockPos): IBlock? = mc.theWorld?.getBlockState(blockPos)?.block

    /**
     * Get material from [blockPos]
     */
    @JvmStatic
    inline fun getMaterial(blockPos: WBlockPos): IMaterial? {
        val state = getState(blockPos)

        return state?.block?.getMaterial(state)
    }

    /**
     * Check [blockPos] is replaceable
     */
    @JvmStatic
    inline fun isReplaceable(blockPos: WBlockPos) = getMaterial(blockPos)?.isReplaceable ?: false

    /**
     * Get state from [blockPos]
     */
    @JvmStatic
    inline fun getState(blockPos: WBlockPos): IIBlockState? = mc.theWorld?.getBlockState(blockPos)

    /**
     * Check if [blockPos] is clickable
     */
    @JvmStatic
    fun canBeClicked(blockPos: WBlockPos) = getBlock(blockPos)?.canCollideCheck(getState(blockPos), false) ?: false &&
            mc.theWorld!!.worldBorder.contains(blockPos)

    /**
     * Get block name by [id]
     */
    @JvmStatic
    fun getBlockName(id: Int): String = functions.getBlockById(id)!!.localizedName

    /**
     * Check if block is full block
     */
    @JvmStatic
    fun isFullBlock(blockPos: WBlockPos): Boolean {
        val axisAlignedBB = getBlock(blockPos)?.getCollisionBoundingBox(mc.theWorld!!, blockPos, getState(blockPos)
                ?: return false)
                ?: return false
        return axisAlignedBB.maxX - axisAlignedBB.minX == 1.0 && axisAlignedBB.maxY - axisAlignedBB.minY == 1.0 && axisAlignedBB.maxZ - axisAlignedBB.minZ == 1.0
    }

    /**
     * Get distance to center of [blockPos]
     */
    @JvmStatic
    fun getCenterDistance(blockPos: WBlockPos) =
            mc.thePlayer!!.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

    /**
     * Search blocks around the player in a specific [radius]
     */
    @JvmStatic
    fun searchBlocks(radius: Int): Map<WBlockPos, IBlock> {
        val blocks = mutableMapOf<WBlockPos, IBlock>()

        val thePlayer = mc.thePlayer ?: return blocks

        for (x in radius downTo -radius + 1) {
            for (y in radius downTo -radius + 1) {
                for (z in radius downTo -radius + 1) {
                    val blockPos = WBlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y,
                            thePlayer.posZ.toInt() + z)
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
    @JvmStatic
    fun collideBlock(axisAlignedBB: IAxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.thePlayer!!

        for (x in floor(thePlayer.entityBoundingBox.minX).toInt() until
                floor(thePlayer.entityBoundingBox.maxX).toInt() + 1L) {
            for (z in floor(thePlayer.entityBoundingBox.minZ).toInt() until
                    floor(thePlayer.entityBoundingBox.maxZ).toInt() + 1) {
                val block = getBlock(WBlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))

                if (!collide(block))
                    return false
            }
        }

        return true
    }

    /**
     * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
     */
    @JvmStatic
    fun collideBlockIntersects(axisAlignedBB: IAxisAlignedBB, collide: Collidable): Boolean {
        val thePlayer = mc.thePlayer!!
        val world = mc.theWorld!!

        for (x in floor(thePlayer.entityBoundingBox.minX).toInt() until
                floor(thePlayer.entityBoundingBox.maxX).toInt() + 1) {
            for (z in floor(thePlayer.entityBoundingBox.minZ).toInt() until
                    floor(thePlayer.entityBoundingBox.maxZ).toInt() + 1) {
                val blockPos = WBlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
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