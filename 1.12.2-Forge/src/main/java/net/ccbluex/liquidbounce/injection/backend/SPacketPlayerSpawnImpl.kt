package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerSpawn
import net.minecraft.network.play.server.SPacketSpawnPlayer
import java.util.*

class SPacketPlayerSpawnImpl<out T : SPacketSpawnPlayer>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketPlayerSpawn
{
	override val entityID: Int
		get() = wrapped.entityID
	override val uuid: UUID
		get() = wrapped.uniqueId
	override val x: Double
		get() = wrapped.x
	override val y: Double
		get() = wrapped.y
	override val z: Double
		get() = wrapped.z
}

fun ISPacketPlayerSpawn.unwrap(): SPacketSpawnPlayer = (this as SPacketPlayerSpawnImpl<*>).wrapped
fun SPacketSpawnPlayer.wrap(): ISPacketPlayerSpawn = SPacketPlayerSpawnImpl(this)
