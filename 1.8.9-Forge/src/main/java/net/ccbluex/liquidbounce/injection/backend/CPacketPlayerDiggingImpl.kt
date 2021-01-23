package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketPlayerDigging
import net.ccbluex.liquidbounce.api.minecraft.util.IEnumFacing
import net.ccbluex.liquidbounce.api.minecraft.util.WBlockPos
import net.ccbluex.liquidbounce.injection.backend.utils.wrap
import net.minecraft.network.play.client.C07PacketPlayerDigging

class CPacketPlayerDiggingImpl<T : C07PacketPlayerDigging>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketPlayerDigging
{
	override val status: ICPacketPlayerDigging.WAction
		get() = wrapped.status.wrap()
	override val position: WBlockPos
		get() = wrapped.position.wrap()
	override val facing: IEnumFacing
		get() = wrapped.facing.wrap()
}

fun ICPacketPlayerDigging.unwrap(): C07PacketPlayerDigging = (this as CPacketPlayerDiggingImpl<*>).wrapped
fun C07PacketPlayerDigging.wrap(): ICPacketPlayerDigging = CPacketPlayerDiggingImpl(this)
