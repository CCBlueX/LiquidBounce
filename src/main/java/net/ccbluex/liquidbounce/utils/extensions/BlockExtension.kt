/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.extensions

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.utils.block.Collidable
import kotlin.math.floor

/**
 * Get vector of block position
 */
val WBlockPos.vec: WVec3
	get() = WVec3(x + 0.5, y + 0.5, z + 0.5)

/**
 * Get block from [blockPos]
 */
fun IWorld.getBlock(blockPos: WBlockPos): IBlock = getBlockState(blockPos).block

/**
 * Get material from [blockPos]
 */
fun IWorld.getMaterial(blockPos: WBlockPos): IMaterial?
{
	val state = getBlockState(blockPos)
	return state.block.getMaterial(state)
}

fun IWorld.isReplaceable(bs: IIBlockState?): Boolean
{
	return ((bs ?: return true).block.getMaterial(bs)?.isReplaceable ?: return false) && !(LiquidBounce.wrapper.classProvider.isBlockSnow(bs.block) && getBlockCollisionBox(bs)!!.maxY > .125)
}

/**
 * Check [blockPos] is replaceable
 */
fun IWorld.isReplaceable(blockPos: WBlockPos): Boolean = getMaterial(blockPos)?.isReplaceable ?: false

/**
 * Check if [blockPos] is clickable
 */
fun IWorld.canBeClicked(blockPos: WBlockPos): Boolean = getBlock(blockPos).canCollideCheck(getBlockState(blockPos), false) && blockPos in worldBorder

fun IWorld.isFullBlock(blockPos: WBlockPos): Boolean
{
	val state = getBlockState(blockPos)
	val bb = state.block.getCollisionBoundingBox(this, blockPos, state) ?: return false
	return bb.maxX - bb.minX == 1.0 && bb.maxY - bb.minY == 1.0 && bb.maxZ - bb.minZ == 1.0
}

/**
 * Get distance to center of [blockPos]
 */
fun IEntity.distanceToCenter(blockPos: WBlockPos): Double = getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

/**
 * Search blocks around the player in a specific [radius]
 */
fun IWorld.searchBlocks(entity: IEntity, radius: Int): Map<WBlockPos, IBlock>
{
	val blocks = mutableMapOf<WBlockPos, IBlock>()

	(radius downTo -radius + 1).forEach { x ->
		(radius downTo -radius + 1).forEach { y ->
			(radius downTo -radius + 1).map { z -> WBlockPos(entity.posX.toInt() + x, entity.posY.toInt() + y, entity.posZ.toInt() + z) }.forEach { pos -> blocks[pos] = getBlock(pos) }
		}
	}

	return blocks
}

/**
 * Check if [bb] has collidable blocks using custom [collide] check
 */
fun IWorld.collideBlock(bb: IAxisAlignedBB, collide: Collidable): Boolean
{
	val minX = floor(bb.minX).toInt()
	val maxX = floor(bb.maxX).toInt() + 1
	val minY = bb.minY
	val minZ = floor(bb.minZ).toInt()
	val maxZ = floor(bb.maxZ).toInt() + 1

	return (minX until maxX).none { x -> (minZ until maxZ).any { z -> !collide(getBlockState(WBlockPos(x.toDouble(), minY, z.toDouble()))) } }
}

/**
 * Check if [bb] has collidable blocks using custom [collide] check
 */
fun IWorld.collideBlockIntersects(bb: IAxisAlignedBB, collide: Collidable): Boolean
{
	val minX = floor(bb.minX).toInt()
	val maxX = floor(bb.maxX).toInt() + 1
	val minY = bb.minY
	val minZ = floor(bb.minZ).toInt()
	val maxZ = floor(bb.maxZ).toInt() + 1

	return (minX until maxX).any { x ->
		(minZ until maxZ).map { z -> WBlockPos(x.toDouble(), minY, z.toDouble()).let { it to getBlockState(it) } }.filter { collide(it.second) }.any intersectCheck@{ (blockPos, state) ->
			bb.intersectsWith(state.block.getCollisionBoundingBox(this, blockPos, state) ?: return@intersectCheck false)
		}
	}
}

fun IWorld.getBlockCollisionBox(state: IIBlockState): IAxisAlignedBB? = state.block.getCollisionBoundingBox(this, WBlockPos.ORIGIN, state)

fun IWorld.getBlockDefaultCollisionBox(block: IBlock): IAxisAlignedBB? = block.defaultState?.let { block.getCollisionBoundingBox(this, WBlockPos.ORIGIN, it) }
