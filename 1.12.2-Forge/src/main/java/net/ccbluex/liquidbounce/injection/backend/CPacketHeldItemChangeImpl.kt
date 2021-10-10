/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketHeldItemChange
import net.minecraft.network.play.client.CPacketHeldItemChange

class CPacketHeldItemChangeImpl<out T : CPacketHeldItemChange>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketHeldItemChange
{
	override var slotId: Int
		get() = wrapped.slotId
		set(value)
		{
			wrapped.slotId = value
		}
}

fun ICPacketHeldItemChange.unwrap(): CPacketHeldItemChange = (this as CPacketHeldItemChangeImpl<*>).wrapped
fun CPacketHeldItemChange.wrap(): ICPacketHeldItemChange = CPacketHeldItemChangeImpl(this)
