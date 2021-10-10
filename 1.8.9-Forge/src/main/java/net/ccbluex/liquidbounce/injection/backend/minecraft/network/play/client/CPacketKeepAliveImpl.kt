/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketKeepAlive
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C00PacketKeepAlive

class CPacketKeepAliveImpl<out T : C00PacketKeepAlive>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketKeepAlive
{
	override var key: Long
		get() = wrapped.key.toLong()
		set(value)
		{
			wrapped.key = value.toInt()
		}
}

fun ICPacketKeepAlive.unwrap(): C00PacketKeepAlive = (this as CPacketKeepAliveImpl<*>).wrapped
fun C00PacketKeepAlive.wrap(): ICPacketKeepAlive = CPacketKeepAliveImpl(this)
