package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ISPacketCustomPayload
import net.minecraft.network.play.server.S3FPacketCustomPayload

class SPacketCustomPayloadImpl<out T : S3FPacketCustomPayload>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketCustomPayload
{
	override val channelName: String
		get() = wrapped.channelName
}

fun ISPacketCustomPayload.unwrap(): S3FPacketCustomPayload = (this as SPacketCustomPayloadImpl<*>).wrapped
fun S3FPacketCustomPayload.wrap(): ISPacketCustomPayload = SPacketCustomPayloadImpl(this)
