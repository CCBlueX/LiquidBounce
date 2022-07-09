package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntityTeleport
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S18PacketEntityTeleport

class SPacketEntityTeleportImpl<out T : S18PacketEntityTeleport>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntityTeleport
{
	override val entityId: Int
		get() = wrapped.entityId

	override val x: Double
		get() = wrapped.x * 0.03125
	override val y: Double
		get() = wrapped.y * 0.03125
	override val z: Double
		get() = wrapped.z * 0.03125

	override val yaw: Byte
		get() = wrapped.yaw
	override val pitch: Byte
		get() = wrapped.pitch

	override val onGround: Boolean
		get() = wrapped.onGround
}

fun ISPacketEntityTeleport.unwrap(): S18PacketEntityTeleport = (this as SPacketEntityTeleportImpl<*>).wrapped
fun S18PacketEntityTeleport.wrap(): ISPacketEntityTeleport = SPacketEntityTeleportImpl(this)
