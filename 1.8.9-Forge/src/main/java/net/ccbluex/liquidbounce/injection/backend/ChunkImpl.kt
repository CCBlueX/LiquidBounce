/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntityPlayerSP
import net.ccbluex.liquidbounce.api.minecraft.util.IAxisAlignedBB
import net.ccbluex.liquidbounce.api.minecraft.world.IChunk
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.world.chunk.Chunk

class ChunkImpl(val wrapped: Chunk) : IChunk {
    override fun getEntitiesWithinAABBForEntity(thePlayer: IEntityPlayerSP, arrowBox: IAxisAlignedBB, collidedEntities: MutableList<IEntity>, nothing: Nothing?) {
        return wrapped.getEntitiesWithinAABBForEntity(thePlayer.unwrap(), arrowBox.unwrap(), collidedEntities.unwrap(), null)
    }

    override fun equals(other: Any?): Boolean {
        return other is ChunkImpl && other.wrapped == this.wrapped
    }
}

inline fun IChunk.unwrap(): Chunk = (this as ChunkImpl).wrapped
inline fun Chunk.wrap(): IChunk = ChunkImpl(this)