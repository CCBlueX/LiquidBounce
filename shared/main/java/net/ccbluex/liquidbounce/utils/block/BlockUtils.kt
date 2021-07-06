/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import kotlin.math.floor

typealias Collidable = (IIBlockState) -> Boolean

object BlockUtils : MinecraftInstance()
{
	/**
	 * Get block from [blockPos]
	 */
	@JvmStatic
	fun getBlock(theWorld: IWorld, blockPos: WBlockPos): IBlock = getState(theWorld, blockPos).block

	/**
	 * Get material from [blockPos]
	 */
	@JvmStatic
	fun getMaterial(theWorld: IWorld, blockPos: WBlockPos): IMaterial?
	{
		val state = getState(theWorld, blockPos)

		return state.block.getMaterial(state)
	}

	@JvmStatic
	fun isReplaceable(theWorld: IWorld, bs: IIBlockState?): Boolean
	{
		bs ?: return true
		return (bs.block.getMaterial(bs)?.isReplaceable ?: return false) && !(classProvider.isBlockSnow(bs.block) && getBlockCollisionBox(theWorld, bs)!!.maxY > .125)
	}

	/**
	 * Check [blockPos] is replaceable
	 */
	@JvmStatic
	fun isReplaceable(theWorld: IWorld, blockPos: WBlockPos) = getMaterial(theWorld, blockPos)?.isReplaceable ?: false

	/**
	 * Get state from [blockPos]
	 */
	@JvmStatic
	fun getState(theWorld: IWorld, blockPos: WBlockPos): IIBlockState = theWorld.getBlockState(blockPos)

	/**
	 * Check if [blockPos] is clickable
	 */
	@JvmStatic
	fun canBeClicked(theWorld: IWorld, blockPos: WBlockPos) = getBlock(theWorld, blockPos).canCollideCheck(getState(theWorld, blockPos), false) && blockPos in theWorld.worldBorder

	/**
	 * Get block name by [id]
	 */
	@JvmStatic
	fun getBlockName(id: Int): String = functions.getBlockById(id)?.localizedName ?: ""

	/**
	 * Check if block is full block
	 */
	@JvmStatic
	fun isFullBlock(theWorld: IWorld, blockPos: WBlockPos): Boolean
	{
		val bb = getBlock(theWorld, blockPos).getCollisionBoundingBox(theWorld, blockPos, getState(theWorld, blockPos)) ?: return false

		return bb.maxX - bb.minX == 1.0 && bb.maxY - bb.minY == 1.0 && bb.maxZ - bb.minZ == 1.0
	}

	/**
	 * Get distance to center of [blockPos]
	 */
	@JvmStatic
	fun getCenterDistance(thePlayer: IEntity, blockPos: WBlockPos) = thePlayer.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

	/**
	 * Search blocks around the player in a specific [radius]
	 */
	@JvmStatic
	fun searchBlocks(theWorld: IWorld, thePlayer: IEntity, radius: Int): Map<WBlockPos, IBlock>
	{
		val blocks = mutableMapOf<WBlockPos, IBlock>()

		(radius downTo -radius + 1).forEach { x ->
			(radius downTo -radius + 1).forEach { y ->
				(radius downTo -radius + 1).map { z -> WBlockPos(thePlayer.posX.toInt() + x, thePlayer.posY.toInt() + y, thePlayer.posZ.toInt() + z) }.forEach { pos -> blocks[pos] = getBlock(theWorld, pos) }
			}
		}

		return blocks
	}

	/**
	 * Check if [bb] has collidable blocks using custom [collide] check
	 */
	@JvmStatic
	fun collideBlock(theWorld: IWorld, bb: IAxisAlignedBB, collide: Collidable): Boolean
	{
		val minX = floor(bb.minX).toInt()
		val maxX = floor(bb.maxX).toInt() + 1
		val minY = bb.minY
		val minZ = floor(bb.minZ).toInt()
		val maxZ = floor(bb.maxZ).toInt() + 1

		return (minX until maxX).none { x -> (minZ until maxZ).any { z -> !collide(getState(theWorld, WBlockPos(x.toDouble(), minY, z.toDouble()))) } }
	}

	/**
	 * Check if [bb] has collidable blocks using custom [collide] check
	 */
	@JvmStatic
	fun collideBlockIntersects(theWorld: IWorld, bb: IAxisAlignedBB, collide: Collidable): Boolean
	{
		val minX = floor(bb.minX).toInt()
		val maxX = floor(bb.maxX).toInt() + 1
		val minY = bb.minY
		val minZ = floor(bb.minZ).toInt()
		val maxZ = floor(bb.maxZ).toInt() + 1

		return (minX until maxX).any { x ->
			(minZ until maxZ).map { z -> WBlockPos(x.toDouble(), minY, z.toDouble()).let { it to getState(theWorld, it) } }.filter { collide(it.second) }.any intersectCheck@{ (blockPos, state) ->
				bb.intersectsWith(state.let { state.block.getCollisionBoundingBox(theWorld, blockPos, it) } ?: return@intersectCheck false)
			}
		}
	}

	@JvmStatic
	fun getBlockCollisionBox(theWorld: IWorld, state: IIBlockState): IAxisAlignedBB? = state.block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, state)

	@JvmStatic
	fun getBlockDefaultCollisionBox(theWorld: IWorld, block: IBlock): IAxisAlignedBB? = block.defaultState?.let { block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, it) }
}
