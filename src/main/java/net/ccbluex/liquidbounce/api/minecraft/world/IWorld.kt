/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.world

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreboard
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.border.IWorldBorder

interface IWorld
{
	val isRemote: Boolean
	val scoreboard: IScoreboard
	val worldBorder: IWorldBorder

	// <editor-fold desc="Chunk">
	fun getChunkFromChunkCoords(x: Int, z: Int): IChunk
	// </editor-fold>

	// <editor-fold desc="Block">
	fun getBlockState(blockPos: WBlockPos): IIBlockState
	// </editor-fold>

	// <editor-fold desc="Collision Box">
	fun getCollidingBoundingBoxes(entity: IEntity, bb: IAxisAlignedBB): Collection<IAxisAlignedBB>
	fun checkBlockCollision(aabb: IAxisAlignedBB): Boolean
	fun getCollisionBoxes(bb: IAxisAlignedBB): Collection<IAxisAlignedBB>
	// </editor-fold>

	// <editor-fold desc="Entity">
	fun getEntityByID(id: Int): IEntity?
	fun getEntitiesInAABBexcluding(entityIn: IEntity?, boundingBox: IAxisAlignedBB, predicate: (IEntity?) -> Boolean): Collection<IEntity>
	fun getEntitiesWithinAABBExcludingEntity(entity: IEntity?, bb: IAxisAlignedBB): Collection<IEntity>
	// </editor-fold>

	// <editor-fold desc="Raytrace">
	fun rayTraceBlocks(start: WVec3, end: WVec3): IMovingObjectPosition?
	fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean): IMovingObjectPosition?
	fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): IMovingObjectPosition?
	// </editor-fold>
}
