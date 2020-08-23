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

interface IWorld {
    val isRemote: Boolean
    val scoreboard: IScoreboard
    val worldBorder: IWorldBorder

    fun getEntityByID(id: Int): IEntity?

    fun rayTraceBlocks(start: WVec3, end: WVec3): IMovingObjectPosition?
    fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean): IMovingObjectPosition?
    fun rayTraceBlocks(start: WVec3, end: WVec3, stopOnLiquid: Boolean, ignoreBlockWithoutBoundingBox: Boolean, returnLastUncollidableBlock: Boolean): IMovingObjectPosition?

    fun getEntitiesInAABBexcluding(entityIn: IEntity?, boundingBox: IAxisAlignedBB, predicate: (IEntity?) -> Boolean): Collection<IEntity>
    fun getBlockState(blockPos: WBlockPos): IIBlockState
    fun getEntitiesWithinAABBExcludingEntity(entity: IEntity?, bb: IAxisAlignedBB): Collection<IEntity>
    fun getCollidingBoundingBoxes(entity: IEntity, bb: IAxisAlignedBB): Collection<IAxisAlignedBB>
    fun checkBlockCollision(aabb: IAxisAlignedBB): Boolean
    fun getCollisionBoxes(bb: IAxisAlignedBB): Collection<IAxisAlignedBB>
    fun getChunkFromChunkCoords(x: Int, z: Int): IChunk
}