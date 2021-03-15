/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import com.google.common.base.Predicate
import net.ccbluex.liquidbounce.api.minecraft.block.state.IIBlockState
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.api.util.WrappedMutableList
import net.ccbluex.liquidbounce.api.util.WrappedPredicate
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.entity.Entity
import net.minecraft.world.chunk.Chunk

class ChunkImpl(val wrapped: Chunk) : IChunk
{
	override val x: Int
		get() = wrapped.x
	override val z: Int
		get() = wrapped.z
	override val isLoaded: Boolean
		get() = wrapped.isLoaded

	override fun getEntitiesWithinAABBForEntity(thePlayer: IEntityPlayerSP, arrowBox: IAxisAlignedBB, collidedEntities: MutableList<IEntity>, predicate: Predicate<IEntity>?) = wrapped.getEntitiesWithinAABBForEntity(thePlayer.unwrap(), arrowBox.unwrap(), WrappedMutableList(collidedEntities, Entity::wrap, IEntity::unwrap), predicate?.let { WrappedPredicate(it, Entity::wrap) })

	override fun getHeightValue(x: Int, z: Int): Int = wrapped.getHeightValue(x, z)

	override fun getBlockState(blockPos: WBlockPos): IIBlockState = wrapped.getBlockState(blockPos.unwrap()).wrap()

	override fun equals(other: Any?): Boolean = other is ChunkImpl && other.wrapped == wrapped
}

fun IChunk.unwrap(): Chunk = (this as ChunkImpl).wrapped
fun Chunk.wrap(): IChunk = ChunkImpl(this)
