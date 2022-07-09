/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.minecraft.network.IPacketBuffer
import net.ccbluex.liquidbounce.injection.backend.minecraft.item.unwrap
import net.minecraft.network.PacketBuffer

class PacketBufferImpl(val wrapped: PacketBuffer) : IPacketBuffer
{
	override fun writeBytes(payload: ByteArray)
	{
		wrapped.writeBytes(payload)
	}

	override fun writeItemStackToBuffer(itemStack: IItemStack)
	{
		wrapped.writeItemStackToBuffer(itemStack.unwrap())
	}

	override fun writeString(vanilla: String): IPacketBuffer
	{
		wrapped.writeString(vanilla)

		return this
	}

	override fun equals(other: Any?): Boolean = other is PacketBufferImpl && other.wrapped == wrapped
}

fun IPacketBuffer.unwrap(): PacketBuffer = (this as PacketBufferImpl).wrapped
fun PacketBuffer.wrap(): IPacketBuffer = PacketBufferImpl(this)
