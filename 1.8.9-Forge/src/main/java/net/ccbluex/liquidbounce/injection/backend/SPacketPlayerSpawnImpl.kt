package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerSpawn
import net.minecraft.network.play.server.S0CPacketSpawnPlayer

class SPacketPlayerSpawnImpl<out T : S0CPacketSpawnPlayer>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketPlayerSpawn
{
	override val entityID: Int
		get() = wrapped.entityID
	override val x: Int
		get() = wrapped.x
	override val y: Int
		get() = wrapped.y
	override val z: Int
		get() = wrapped.z
}

fun ISPacketPlayerSpawn.unwrap(): S0CPacketSpawnPlayer = (this as SPacketPlayerSpawnImpl<*>).wrapped
fun S0CPacketSpawnPlayer.wrap(): ISPacketPlayerSpawn = SPacketPlayerSpawnImpl(this)
