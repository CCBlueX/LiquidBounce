/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack
import net.ccbluex.liquidbounce.api.network.IPacketBuffer
import net.ccbluex.liquidbounce.injection.backend.utils.unwrap
import net.minecraft.network.PacketBuffer

class PacketBufferImpl(val wrapped: PacketBuffer) : IPacketBuffer {
    override fun writeBytes(payload: ByteArray) {
        wrapped.writeBytes(payload)
    }

    override fun writeItemStackToBuffer(itemStack: IItemStack) {
        wrapped.writeItemStackToBuffer(itemStack.unwrap())
    }

    override fun writeString(vanilla: String): IPacketBuffer {
        wrapped.writeString(vanilla)

        return this
    }
}