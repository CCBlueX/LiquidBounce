/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.client.entity.IEntity
import net.ccbluex.liquidbounce.api.minecraft.network.IPacket
import net.ccbluex.liquidbounce.api.minecraft.world.IWorld

interface ISPacketEntity : IPacket
{
	val rotating: Boolean

	val onGround: Boolean
	val yaw: Byte
	val pitch: Byte

	fun getEntity(world: IWorld): IEntity?
}
