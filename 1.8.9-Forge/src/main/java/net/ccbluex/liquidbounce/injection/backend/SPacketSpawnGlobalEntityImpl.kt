/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketSpawnGlobalEntity
import net.minecraft.network.play.server.S2CPacketSpawnGlobalEntity

class SPacketSpawnGlobalEntityImpl<out T : S2CPacketSpawnGlobalEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketSpawnGlobalEntity
{
	override val type: Int
		get() = wrapped.func_149052_c()

	override val x: Int
		get() = wrapped.func_149051_d()
	override val y: Int
		get() = wrapped.func_149050_e()
	override val z: Int
		get() = wrapped.func_149049_f()
}

fun ISPacketSpawnGlobalEntity.unwrap(): S2CPacketSpawnGlobalEntity = (this as SPacketSpawnGlobalEntityImpl<*>).wrapped
fun S2CPacketSpawnGlobalEntity.wrap(): ISPacketSpawnGlobalEntity = SPacketSpawnGlobalEntityImpl(this)
