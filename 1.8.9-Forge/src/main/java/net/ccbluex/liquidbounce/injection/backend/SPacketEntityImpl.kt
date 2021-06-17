/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntity
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.minecraft.network.play.server.S14PacketEntity

class SPacketEntityImpl<out T : S14PacketEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntity
{
	override val rotating: Boolean
		get() = wrapped.func_149060_h() // isRotating()

	override val posX: Byte
		get() = wrapped.func_149062_c()
	override val posY: Byte
		get() = wrapped.func_149061_d()
	override val posZ: Byte
		get() = wrapped.func_149064_e()

	override val onGround: Boolean
		get() = wrapped.onGround
	override val yaw: Byte
		get() = wrapped.func_149066_f() // getYaw()
	override val pitch: Byte
		get() = wrapped.func_149063_g() // getPitch()

	override fun getEntity(world: IWorld): IEntity? = wrapped.getEntity(world.unwrap())?.wrap()
}

fun ISPacketEntity.unwrap(): S14PacketEntity = (this as SPacketEntityImpl<*>).wrapped
fun S14PacketEntity.wrap(): ISPacketEntity = SPacketEntityImpl(this)
