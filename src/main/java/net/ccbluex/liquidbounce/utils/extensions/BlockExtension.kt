/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.utils.block.Collidable
import net.minecraft.block.Block
import net.minecraft.block.BlockSnow
import net.minecraft.block.material.Material
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraft.world.World
import kotlin.math.floor

/**
 * Get vector of block position
 */
val BlockPos.vec: Vec3
    get() = Vec3(x + 0.5, y + 0.5, z + 0.5)

/**
 * Get block from [blockPos]
 */
fun World.getBlock(blockPos: BlockPos): Block = getBlockState(blockPos).block

/**
 * Get material from [blockPos]
 */
fun World.getMaterial(blockPos: BlockPos): Material?
{
    val state = getBlockState(blockPos)
    return state.block.material
}

fun World.isReplaceable(bs: IBlockState?): Boolean
{
    return ((bs ?: return true).block.material?.isReplaceable ?: return false) && !(bs.block is BlockSnow && getBlockCollisionBox(bs)!!.maxY > .125)
}

/**
 * Check [blockPos] is replaceable
 */
fun World.isReplaceable(blockPos: BlockPos): Boolean = getMaterial(blockPos)?.isReplaceable ?: false

/**
 * Check if [blockPos] is clickable
 */
fun World.canBeClicked(blockPos: BlockPos): Boolean = getBlock(blockPos).canCollideCheck(getBlockState(blockPos), false) && blockPos in worldBorder

fun World.isFullBlock(blockPos: BlockPos): Boolean
{
    val state = getBlockState(blockPos)
    val bb = state.block.getCollisionBoundingBox(this, blockPos, state) ?: return false
    return bb.maxX - bb.minX == 1.0 && bb.maxY - bb.minY == 1.0 && bb.maxZ - bb.minZ == 1.0
}

// TODO: Move this to EntityExtension
/**
 * Get distance to center of [blockPos]
 */
fun Entity.distanceToCenter(blockPos: BlockPos): Double = getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

/**
 * Search blocks around the player in a specific [radius]
 */
fun World.searchBlocks(entity: Entity, radius: Int): Map<BlockPos, Block>
{
    val blocks = mutableMapOf<BlockPos, Block>()

    (radius downTo -radius + 1).forEach { x ->
        (radius downTo -radius + 1).forEach { y ->
            (radius downTo -radius + 1).map { z -> BlockPos(entity.posX.toInt() + x, entity.posY.toInt() + y, entity.posZ.toInt() + z) }.forEach { pos -> blocks[pos] = getBlock(pos) }
        }
    }

    return blocks
}

/**
 * Check if [bb] has collidable blocks using custom [collide] check
 */
fun World.collideBlock(bb: AxisAlignedBB, collide: Collidable): Boolean
{
    val minX = floor(bb.minX).toInt()
    val maxX = floor(bb.maxX).toInt() + 1
    val minY = bb.minY
    val minZ = floor(bb.minZ).toInt()
    val maxZ = floor(bb.maxZ).toInt() + 1

    return (minX until maxX).none { x -> (minZ until maxZ).any { z -> !collide(getBlockState(BlockPos(x.toDouble(), minY, z.toDouble()))) } }
}

/**
 * Check if [bb] has collidable blocks using custom [collide] check
 */
fun World.collideBlockIntersects(bb: AxisAlignedBB, collide: Collidable): Boolean
{
    val minX = floor(bb.minX).toInt()
    val maxX = floor(bb.maxX).toInt() + 1
    val minY = bb.minY
    val minZ = floor(bb.minZ).toInt()
    val maxZ = floor(bb.maxZ).toInt() + 1

    return (minX until maxX).any { x ->
        (minZ until maxZ).map { z -> BlockPos(x.toDouble(), minY, z.toDouble()).let { it to getBlockState(it) } }.filter { collide(it.second) }.any intersectCheck@{ (blockPos, state) ->
            bb.intersectsWith(state.block.getCollisionBoundingBox(this, blockPos, state) ?: return@intersectCheck false)
        }
    }
}

fun World.getBlockCollisionBox(state: IBlockState): AxisAlignedBB? = state.block.getCollisionBoundingBox(this, BlockPos.ORIGIN, state)

fun World.getBlockDefaultCollisionBox(block: Block): AxisAlignedBB? = block.defaultState?.let { block.getCollisionBoundingBox(this, BlockPos.ORIGIN, it) }
