/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.api.network

import net.ccbluex.liquidbounce.api.minecraft.item.IItemStack

interface IPacketBuffer {
    fun writeBytes(payload: ByteArray)
    fun writeItemStackToBuffer(itemStack: IItemStack)
    fun writeString(vanilla: String): IPacketBuffer
}