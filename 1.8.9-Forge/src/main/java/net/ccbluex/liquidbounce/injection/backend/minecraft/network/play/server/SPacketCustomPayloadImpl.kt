package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketCustomPayload
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S3FPacketCustomPayload

class SPacketCustomPayloadImpl<out T : S3FPacketCustomPayload>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketCustomPayload
{
	override val channelName: String
		get() = wrapped.channelName
}

fun ISPacketCustomPayload.unwrap(): S3FPacketCustomPayload = (this as SPacketCustomPayloadImpl<*>).wrapped
fun S3FPacketCustomPayload.wrap(): ISPacketCustomPayload = SPacketCustomPayloadImpl(this)
