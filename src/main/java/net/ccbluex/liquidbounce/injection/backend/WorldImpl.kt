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

open class WorldImpl<T : World>(val wrapped: T) : IWorld {
    override val isRemote: Boolean
        get() = wrapped.isRemote
    override val scoreboard: IScoreboard
        get() = wrapped.scoreboard.wrap()

    override val worldBorder: IWorldBorder
        get() = wrapped.worldBorder.wrap()

    override fun getEntityByID(id: Int): IEntity? = wrapped.getEntityByID(id)?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap())?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid)?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)?.wrap()

    override fun getEntitiesInAABBexcluding(entityIn: IEntity?, boundingBox: IAxisAlignedBB, predicate: (IEntity?) -> Boolean): Collection<IEntity> {
        return WrappedCollection(
                wrapped.getEntitiesInAABBexcluding(entityIn?.unwrap(), boundingBox.unwrap()) { predicate(it?.wrap()) },
                IEntity::unwrap,
                Entity::wrap
        )
    }

    override fun getBlockState(blockPos: WBlockPos): IIBlockState = wrapped.getBlockState(blockPos.unwrap()).wrap()

    override fun getEntitiesWithinAABBExcludingEntity(entity: IEntity?, bb: IAxisAlignedBB): Collection<IEntity> {
        return WrappedCollection(
                wrapped.getEntitiesWithinAABBExcludingEntity(entity?.unwrap(), bb.unwrap()),
                IEntity::unwrap,
                Entity::wrap
        )
    }

    override fun getCollidingBoundingBoxes(entity: IEntity, bb: IAxisAlignedBB): Collection<IAxisAlignedBB> {
        return WrappedCollection(
                wrapped.getCollidingBoundingBoxes(entity.unwrap(), bb.unwrap()),
                IAxisAlignedBB::unwrap,
                AxisAlignedBB::wrap
        )
    }

    override fun checkBlockCollision(aabb: IAxisAlignedBB): Boolean = wrapped.checkBlockCollision(aabb.unwrap())

    override fun getCollisionBoxes(bb: IAxisAlignedBB): Collection<IAxisAlignedBB> = WrappedCollection(wrapped.getCollisionBoxes(bb.unwrap()), IAxisAlignedBB::unwrap, AxisAlignedBB::wrap)
    override fun getChunkFromChunkCoords(x: Int, z: Int): IChunk = wrapped.getChunkFromChunkCoords(x, z).wrap()

    override fun equals(other: Any?): Boolean {
        return other is WorldImpl<*> && other.wrapped == this.wrapped
    }
}

inline fun IWorld.unwrap(): World = (this as WorldImpl<*>).wrapped
inline fun World.wrap(): IWorld = WorldImpl(this)