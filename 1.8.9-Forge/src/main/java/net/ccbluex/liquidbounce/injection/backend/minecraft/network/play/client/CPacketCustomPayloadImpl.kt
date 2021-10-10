/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.IPacketBuffer
import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketCustomPayload
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.unwrap
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.wrap
import net.minecraft.network.play.client.C17PacketCustomPayload

class CPacketCustomPayloadImpl<out T : C17PacketCustomPayload>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketCustomPayload
{
	override var data: IPacketBuffer
		get() = wrapped.data.wrap()
		set(value)
		{
			wrapped.data = value.unwrap()
		}
	override val channelName: String
		get() = wrapped.channelName
}

fun ICPacketCustomPayload.unwrap(): C17PacketCustomPayload = (this as CPacketCustomPayloadImpl<*>).wrapped
fun C17PacketCustomPayload.wrap(): ICPacketCustomPayload = CPacketCustomPayloadImpl(this)
