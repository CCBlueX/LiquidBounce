package net.ccbluex.liquidbounce.api.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket

interface ISPacketEntityTeleport : IPacket
{
	val entityId: Int

	val x: Double
	val y: Double
	val z: Double

	val yaw: Byte
	val pitch: Byte

	val onGround: Boolean
}
