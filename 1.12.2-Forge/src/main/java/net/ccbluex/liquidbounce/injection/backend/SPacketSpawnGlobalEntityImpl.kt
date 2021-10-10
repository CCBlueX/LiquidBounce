/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketSpawnGlobalEntity
import net.minecraft.network.play.server.SPacketSpawnGlobalEntity

class SPacketSpawnGlobalEntityImpl<out T : SPacketSpawnGlobalEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketSpawnGlobalEntity
{
	override val type: Int
		get() = wrapped.type

	override val x: Double
		get() = wrapped.x
	override val y: Double
		get() = wrapped.y
	override val z: Double
		get() = wrapped.z
}

fun ISPacketSpawnGlobalEntity.unwrap(): SPacketSpawnGlobalEntity = (this as SPacketSpawnGlobalEntityImpl<*>).wrapped
fun SPacketSpawnGlobalEntity.wrap(): ISPacketSpawnGlobalEntity = SPacketSpawnGlobalEntityImpl(this)
