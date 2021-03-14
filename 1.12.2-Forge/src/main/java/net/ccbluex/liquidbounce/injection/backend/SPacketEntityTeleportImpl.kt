package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityTeleport
import net.minecraft.network.play.server.SPacketEntityTeleport

class SPacketEntityTeleportImpl<out T : SPacketEntityTeleport>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityTeleport
{
	override val entityId: Int
		get() = wrapped.entityId

	override val x: Double
		get() = wrapped.x
	override val y: Double
		get() = wrapped.y
	override val z: Double
		get() = wrapped.z
}

fun ISPacketEntityTeleport.unwrap(): SPacketEntityTeleport = (this as SPacketEntityTeleportImpl<*>).wrapped
fun SPacketEntityTeleport.wrap(): ISPacketEntityTeleport = SPacketEntityTeleportImpl(this)
