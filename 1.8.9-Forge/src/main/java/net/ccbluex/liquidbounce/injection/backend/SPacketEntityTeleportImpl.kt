package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityTeleport
import net.minecraft.network.play.server.S18PacketEntityTeleport

class SPacketEntityTeleportImpl<T : S18PacketEntityTeleport>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityTeleport
{
	override val entityId: Int
		get() = wrapped.entityId

	override val x: Int
		get() = wrapped.x
	override val y: Int
		get() = wrapped.y
	override val z: Int
		get() = wrapped.z
}

inline fun ISPacketEntityTeleport.unwrap(): S18PacketEntityTeleport = (this as SPacketEntityTeleportImpl<*>).wrapped
inline fun S18PacketEntityTeleport.wrap(): ISPacketEntityTeleport = SPacketEntityTeleportImpl(this)
