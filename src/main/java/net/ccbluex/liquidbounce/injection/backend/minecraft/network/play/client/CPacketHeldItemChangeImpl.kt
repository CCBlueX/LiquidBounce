/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.client.C09PacketHeldItemChange

class CPacketHeldItemChangeImpl<out T : C09PacketHeldItemChange>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketHeldItemChange
{
	override var slotId: Int
		get() = wrapped.slotId
		set(value)
		{
			wrapped.slotId = value
		}
}

fun ICPacketHeldItemChange.unwrap(): C09PacketHeldItemChange = (this as CPacketHeldItemChangeImpl<*>).wrapped
fun C09PacketHeldItemChange.wrap(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(this)
