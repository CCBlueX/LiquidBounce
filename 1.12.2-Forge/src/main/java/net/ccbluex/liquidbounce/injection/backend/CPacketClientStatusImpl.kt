/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketClientStatus
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.CPacketClientStatus

class CPacketClientStatusImpl<out T : CPacketClientStatus>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketClientStatus
{
	override val status: ICPacketClientStatus.WEnumState
		get() = wrapped.status.wrap()
}

fun ICPacketClientStatus.unwrap(): CPacketClientStatus = (this as CPacketClientStatusImpl<*>).wrapped
fun CPacketClientStatus.wrap(): ICPacketClientStatus = CPacketClientStatusImpl(this)
