/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.google.common.base.Predicate
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.IMovingObjectPosition
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.util.WVec3
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.api.minecraft.world.border.IWorldBorder
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.entity.Entity
import net.minecraft.util.AxisAlignedBB
import net.minecraft.world.World

class WorldImpl(val wrapped: World) : IWorld {

    override val worldBorder: IWorldBorder
        get() = TODO("Not yet implemented")

    override fun getEntityByID(id: Int): IEntity? = wrapped.getEntityByID(id)?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap())?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid)?.wrap()

    override fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): IMovingObjectPosition? = wrapped.rayTraceBlocks(start.unwrap(), end.unwrap(), stopOnLiquid, ignoreBlockWithoutBoundingBox, returnLastUncollidableBlock)?.wrap()

    override fun getEntitiesInAABBexcluding(entityIn: IEntity?, boundingBox: IAxisAlignedBB, predicate: Predicate<in IEntity>): Collection<IEntity> {
        return WrappedCollection(
                wrapped.getEntitiesInAABBexcluding(entityIn?.unwrap(), boundingBox.unwrap()) { predicate.apply(it?.wrap()) },
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

    override fun getCollisionBoxes(bb: IAxisAlignedBB): Collection<IAxisAlignedBB> = WrappedCollection<AxisAlignedBB, IAxisAlignedBB>(wrapped.getCollisionBoxes(bb.unwrap()), IAxisAlignedBB::unwrap, AxisAlignedBB::wrap)
}

