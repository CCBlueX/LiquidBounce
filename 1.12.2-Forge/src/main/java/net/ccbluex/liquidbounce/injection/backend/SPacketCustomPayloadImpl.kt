package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketCustomPayload
import net.minecraft.network.play.server.SPacketCustomPayload

class SPacketCustomPayloadImpl<out T : SPacketCustomPayload>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketCustomPayload
{
	override val channelName: String
		get() = wrapped.channelName
}

fun ISPacketCustomPayload.unwrap(): SPacketCustomPayload = (this as SPacketCustomPayloadImpl<*>).wrapped
fun SPacketCustomPayload.wrap(): ISPacketCustomPayload = SPacketCustomPayloadImpl(this)
