/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.block

import net.ccbluex.liquidbounce.api.minecraft.block.material.IMaterial
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.block.IBlock
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import kotlin.math.floor

typealias Collidable = (IBlock?) -> Boolean

object BlockUtils : MinecraftInstance()
{
	/**
	 * Get block from [blockPos]
	 */
	@JvmStatic
	fun getBlock(theWorld: IWorldClient, blockPos: WBlockPos): IBlock = theWorld.getBlockState(blockPos).block

	/**
	 * Get material from [blockPos]
	 */
	@JvmStatic
	fun getMaterial(blockPos: WBlockPos): IMaterial?
	{
		val state = getState(blockPos)

		return state?.block?.getMaterial(state)
	}

	@JvmStatic
	fun isReplaceable(theWorld: IWorldClient, bs: IIBlockState?): Boolean
	{
		bs ?: return true
		return (bs.block.getMaterial(bs)?.isReplaceable ?: return false) && !(classProvider.isBlockSnow(bs.block) && getBlockCollisionBox(theWorld, bs)!!.maxY > .125)
	}

	/**
	 * Check [blockPos] is replaceable
	 */
	@JvmStatic
	fun isReplaceable(blockPos: WBlockPos) = getMaterial(blockPos)?.isReplaceable ?: false

	/**
	 * Get state from [blockPos]
	 */
	@JvmStatic
	fun getState(blockPos: WBlockPos): IIBlockState? = mc.theWorld?.getBlockState(blockPos)

	/**
	 * Check if [blockPos] is clickable
	 */
	@JvmStatic
	fun canBeClicked(theWorld: IWorldClient, blockPos: WBlockPos) = getBlock(theWorld, blockPos).canCollideCheck(getState(blockPos), false) && blockPos in theWorld.worldBorder

	/**
	 * Get block name by [id]
	 */
	@JvmStatic
	fun getBlockName(id: Int): String = functions.getBlockById(id)?.localizedName ?: ""

	/**
	 * Check if block is full block
	 */
	@JvmStatic
	fun isFullBlock(theWorld: IWorldClient, blockPos: WBlockPos): Boolean
	{
		val axisAlignedBB = getBlock(theWorld, blockPos).getCollisionBoundingBox(theWorld, blockPos, getState(blockPos) ?: return false) ?: return false

		return axisAlignedBB.maxX - axisAlignedBB.minX == 1.0 && axisAlignedBB.maxY - axisAlignedBB.minY == 1.0 && axisAlignedBB.maxZ - axisAlignedBB.minZ == 1.0
	}

	/**
	 * Get distance to center of [blockPos]
	 */
	@JvmStatic
	fun getCenterDistance(thePlayer: IEntityPlayerSP, blockPos: WBlockPos) = thePlayer.getDistance(blockPos.x + 0.5, blockPos.y + 0.5, blockPos.z + 0.5)

	/**
	 * Search blocks around the player in a specific [radius]
	 */
	@JvmStatic
	fun searchBlocks(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, radius: Int): Map<WBlockPos, IBlock>
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
	 * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
	 */
	@JvmStatic
	fun collideBlock(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, axisAlignedBB: IAxisAlignedBB, collide: Collidable): Boolean = (floor(thePlayer.entityBoundingBox.minX).toInt() until floor(thePlayer.entityBoundingBox.maxX).toInt() + 1).firstOrNull()?.let { x -> (floor(thePlayer.entityBoundingBox.minZ).toInt() until floor(thePlayer.entityBoundingBox.maxZ).toInt() + 1).none { z -> !collide(getBlock(theWorld, WBlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble()))) } } ?: true

	/**
	 * Check if [axisAlignedBB] has collidable blocks using custom [collide] check
	 */
	@JvmStatic
	fun collideBlockIntersects(theWorld: IWorldClient, thePlayer: IEntityPlayerSP, axisAlignedBB: IAxisAlignedBB, collide: Collidable): Boolean
	{
		val box = thePlayer.entityBoundingBox

		return (floor(box.minX).toInt() until floor(box.maxX).toInt() + 1).any { x ->
			(floor(box.minZ).toInt() until floor(box.maxZ).toInt() + 1).map { z ->
				val blockPos = WBlockPos(x.toDouble(), axisAlignedBB.minY, z.toDouble())
				blockPos to getBlock(theWorld, blockPos)
			}.filter { collide(it.second) }.any intersectCheck@{ (blockPos, block) -> box.intersectsWith(getState(blockPos)?.let { block.getCollisionBoundingBox(theWorld, blockPos, it) } ?: return@intersectCheck false) }
		}
	}

	@JvmStatic
	fun getBlockCollisionBox(theWorld: IWorldClient, state: IIBlockState): IAxisAlignedBB? = state.block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, state)

	@JvmStatic
	fun getDefaultBlockCollisionBox(theWorld: IWorldClient, block: IBlock): IAxisAlignedBB? = block.defaultState?.let { block.getCollisionBoundingBox(theWorld, WBlockPos.ORIGIN, it) }
}
