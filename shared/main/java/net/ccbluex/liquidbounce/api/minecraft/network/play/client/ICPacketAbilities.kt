package net.ccbluex.liquidbounce.api.minecraft.network.play.client

import net.ccbluex.liquidbounce.api.minecraft.network.IPacket

interface ICPacketAbilities : IPacket
{
	var flying: Boolean

	override fun equals(other: Any?): Boolean
}
