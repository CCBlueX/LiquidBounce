package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerSpawn
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S0CPacketSpawnPlayer
import java.util.*

class SPacketPlayerSpawnImpl<out T : S0CPacketSpawnPlayer>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketPlayerSpawn
{
	override val entityID: Int
		get() = wrapped.entityID

	override val uuid: UUID
		get() = wrapped.player

	override val x: Double
		get() = wrapped.x.toDouble() / 32.0
	override val y: Double
		get() = wrapped.y.toDouble() / 32.0
	override val z: Double
		get() = wrapped.z.toDouble() / 32.0
}

fun ISPacketPlayerSpawn.unwrap(): S0CPacketSpawnPlayer = (this as SPacketPlayerSpawnImpl<*>).wrapped
fun S0CPacketSpawnPlayer.wrap(): ISPacketPlayerSpawn = SPacketPlayerSpawnImpl(this)
