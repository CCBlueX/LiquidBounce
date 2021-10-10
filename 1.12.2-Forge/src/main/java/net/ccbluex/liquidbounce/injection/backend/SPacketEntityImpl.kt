/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketEntity
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld
import net.minecraft.network.play.server.SPacketEntity

class SPacketEntityImpl<out T : SPacketEntity>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketEntity
{
	override val rotating: Boolean
		get() = wrapped.isRotating
	override val posX: Int
		get() = wrapped.x
	override val posY: Int
		get() = wrapped.y
	override val posZ: Int
		get() = wrapped.z
	override val onGround: Boolean
		get() = wrapped.onGround
	override val yaw: Byte
		get() = wrapped.yaw
	override val pitch: Byte
		get() = wrapped.pitch

	override fun getEntity(world: IWorld): IEntity = wrapped.getEntity(world.unwrap()).wrap()
}

fun ISPacketEntity.unwrap(): SPacketEntity = (this as SPacketEntityImpl<*>).wrapped
fun SPacketEntity.wrap(): ISPacketEntity = SPacketEntityImpl(this)
