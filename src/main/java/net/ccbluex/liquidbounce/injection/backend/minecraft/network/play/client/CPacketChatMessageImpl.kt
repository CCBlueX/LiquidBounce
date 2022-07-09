/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketChatMessage
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C01PacketChatMessage

class CPacketChatMessageImpl<out T : C01PacketChatMessage>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketChatMessage
{
	override var message: String
		get() = wrapped.message
		set(value)
		{
			wrapped.message = value
		}
}

fun ICPacketChatMessage.unwrap(): C01PacketChatMessage = (this as CPacketChatMessageImpl<*>).wrapped
fun C01PacketChatMessage.wrap(): ICPacketChatMessage = CPacketChatMessageImpl(this)
