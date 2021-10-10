/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.client.multiplayer

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.entity.player.IEntityPlayer
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.tileentity.ITileEntity
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.api.util.WrappedCollection
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.player.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.entity.player.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.tileentity.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.tileentity.wrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.world.WorldImpl
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.tileentity.TileEntity

class WorldClientImpl(wrapped: WorldClient) : WorldImpl<WorldClient>(wrapped), IWorldClient
{
	override var worldTime: Long
		get() = wrapped.worldTime
		set(value)
		{
			wrapped.worldTime = value
		}

	override val loadedEntityList: Collection<IEntity>
		get() = WrappedCollection(wrapped.loadedEntityList, IEntity::unwrap, Entity::wrap)
	override val loadedTileEntityList: Collection<ITileEntity>
		get() = WrappedCollection(wrapped.loadedTileEntityList, ITileEntity::unwrap, TileEntity::wrap)
	override val playerEntities: Collection<IEntityPlayer>
		get() = WrappedCollection(wrapped.playerEntities, IEntityPlayer::unwrap, EntityPlayer::wrap)

	// <editor-fold desc="Packet">
	override fun sendQuittingDisconnectingPacket() = wrapped.sendQuittingDisconnectingPacket()

	override fun sendBlockBreakProgress(entityId: Int, blockPos: WBlockPos, damage: Int) = wrapped.sendBlockBreakProgress(entityId, blockPos.unwrap(), damage)
	// </editor-fold>

	// <editor-fold desc="Entity">
	override fun addEntityToWorld(entityId: Int, entity: IEntity) = wrapped.addEntityToWorld(entityId, entity.unwrap())

	override fun removeEntityFromWorld(entityId: Int)
	{
		wrapped.removeEntityFromWorld(entityId)
	}
	// </editor-fold>

	// <editor-fold desc="Weather">
	override fun setRainStrength(strength: Float) = wrapped.setRainStrength(strength)

	override fun setThunderingStrength(strength: Float) = wrapped.setThunderStrength(strength)
	// </editor-fold>
}

fun IWorldClient.unwrap(): WorldClient = (this as WorldClientImpl).wrapped
fun WorldClient.wrap(): IWorldClient = WorldClientImpl(this)
