/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.client.multiplayer.IWorldClient
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.C02PacketUseEntity

class CPacketUseEntityImpl<out T : C02PacketUseEntity>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketUseEntity
{
	override val action: ICPacketUseEntity.WAction
		get() = wrapped.action.wrap()

	override fun getEntityFromWorld(theWorld: IWorldClient): IEntity? = wrapped.getEntityFromWorld(theWorld.unwrap())?.wrap()
}

fun ICPacketUseEntity.unwrap(): C02PacketUseEntity = (this as CPacketUseEntityImpl<*>).wrapped
fun C02PacketUseEntity.wrap(): ICPacketUseEntity = CPacketUseEntityImpl(this)
