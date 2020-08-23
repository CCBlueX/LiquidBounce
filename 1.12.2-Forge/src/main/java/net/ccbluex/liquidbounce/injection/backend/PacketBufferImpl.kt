/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.network.IPacketBuffer
import net.minecraft.network.PacketBuffer

class PacketBufferImpl(val wrapped: PacketBuffer) : IPacketBuffer {
    override fun writeBytes(payload: ByteArray) {
        wrapped.writeBytes(payload)
    }

    override fun writeItemStackToBuffer(itemStack: IItemStack) {
        wrapped.writeItemStack(itemStack.unwrap())
    }

    override fun writeString(vanilla: String): IPacketBuffer {
        wrapped.writeString(vanilla)

        return this
    }

    override fun equals(other: Any?): Boolean {
        return other is PacketBufferImpl && other.wrapped == this.wrapped
    }
}

inline fun IPacketBuffer.unwrap(): PacketBuffer = (this as PacketBufferImpl).wrapped
inline fun PacketBuffer.wrap(): IPacketBuffer = PacketBufferImpl(this)