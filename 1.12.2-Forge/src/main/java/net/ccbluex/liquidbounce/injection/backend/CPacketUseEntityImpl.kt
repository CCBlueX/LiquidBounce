/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketUseEntity
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.CPacketUseEntity

class CPacketUseEntityImpl<out T : CPacketUseEntity>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketUseEntity
{
	override val action: ICPacketUseEntity.WAction
		get() = wrapped.action.wrap()

	override fun getEntityFromWorld(theWorld: IWorld): IEntity?
	{
		wrapped.getEntityFromWorld(theWorld.unwrap())
	}
}

fun ICPacketUseEntity.unwrap(): CPacketUseEntity = (this as CPacketUseEntityImpl<*>).wrapped
fun CPacketUseEntity.wrap(): ICPacketUseEntity = CPacketUseEntityImpl(this)
