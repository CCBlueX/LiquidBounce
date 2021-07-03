/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook

class SPacketPlayerPosLookImpl<out T : S08PacketPlayerPosLook>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketPlayerPosLook
{
	override val x: Double
		get() = wrapped.x
	override val y: Double
		get() = wrapped.y
	override val z: Double
		get() = wrapped.z

	override var yaw: Float
		get() = wrapped.yaw
		set(value)
		{
			wrapped.yaw = value
		}
	override var pitch: Float
		get() = wrapped.pitch
		set(value)
		{
			wrapped.pitch = value
		}
}

fun ISPacketPlayerPosLook.unwrap(): S08PacketPlayerPosLook = (this as SPacketPlayerPosLookImpl<*>).wrapped
fun S08PacketPlayerPosLook.wrap(): ISPacketPlayerPosLook = SPacketPlayerPosLookImpl(this)
