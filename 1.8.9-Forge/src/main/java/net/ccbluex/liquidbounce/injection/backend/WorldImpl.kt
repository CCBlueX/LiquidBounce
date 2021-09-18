/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.scoreboard.IScoreboard
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.api.minecraft.world.border.IWorldBorder
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World

open class WorldImpl<out T : World>(val wrapped: T) : IWorld
{
	override val isRemote: Boolean
		get() = wrapped.isRemote
	override val scoreboard: IScoreboard
		get() = wrapped.scoreboard.wrap()

	override val worldBorder: IWorldBorder
		get() = wrapped.worldBorder.wrap()

	// <editor-fold desc="Chunk">
	override fun getChunkFromChunkCoords(x: Int, z: Int): IChunk = wrapped.getChunkFromChunkCoords(x, z).wrap()
	// </editor-fold>

	// <editor-fold desc="Block">
	override fun getBlockState(blockPos: WBlockPos): IIBlockState = wrapped.getBlockState(blockPos.unwrap()).wrap()
	// </editor-fold>

	// <editor-fold desc="Collision Box">
	override fun getCollidingBoundingBoxes(entity: IEntity, bb: IAxisAlignedBB): Collection<IAxisAlignedBB> = WrappedCollection(wrapped.getCollidingBoundingBoxes(entity.unwrap(), bb.unwrap()), IAxisAlignedBB::unwrap, AxisAlignedBB::wrap)

	override fun checkBlockCollision(aabb: IAxisAlignedBB): Boolean = wrapped.checkBlockCollision(aabb.unwrap())

	override fun getCollisionBoxes(bb: IAxisAlignedBB): Collection<IAxisAlignedBB> = WrappedCollection(wrapped.getCollisionBoxes(bb.unwrap()), IAxisAlignedBB::unwrap, AxisAlignedBB::wrap)
	// </editor-fold>

	// <editor-fold desc="Entity">
	override fun getEntityByID(id: Int): IEntity? = wrapped.getEntityByID(id)?.wrap()

	override fun getEntitiesInAABBexcluding(entityIn: IEntity?, boundingBox: IAxisAlignedBB, predicate: (IEntity?) -> Boolean): Collection<IEntity> = WrappedCollection(wrapped.getEntitiesInAABBexcluding(entityIn?.unwrap(), boundingBox.unwrap()) { predicate(it?.wrap()) }, IEntity::unwrap, Entity::wrap)

	override fun getEntitiesWithinAABBExcludingEntity(entity: IEntity?, bb: IAxisAlignedBB): Collection<IEntity> = WrappedCollection(wrapped.getEntitiesWithinAABBExcludingEntity(entity?.unwrap(), bb.unwrap()), IEntity::unwrap, Entity::wrap)
	// </editor-fold>

	// <editor-fold desc="Raytrace">
	override fun rayTraceBlocks(start: WVec3, end: WVec3): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap())?.wrap()

	override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid)?.wrap()

	override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)?.wrap()
	// </editor-fold>

	override fun equals(other: Any?): Boolean = other is WorldImpl<*> && other.wrapped == wrapped
}

fun IWorld.unwrap(): World = (this as WorldImpl<*>).wrapped
fun World.wrap(): IWorld = WorldImpl(this)
