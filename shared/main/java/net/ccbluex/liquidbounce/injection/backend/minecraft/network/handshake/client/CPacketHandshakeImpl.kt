/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.handshake.client

import net.ccbluex.liquidbounce.api.minecraft.network.IEnumConnectionState
import net.ccbluex.liquidbounce.api.minecraft.network.handshake.client.ICPacketHandshake
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.wrap
import net.minecraft.network.handshake.client.C00Handshake

class CPacketHandshakeImpl<out T : C00Handshake>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketHandshake
{
    override val port: Int
        get() = wrapped.port
    override var ip: String
        get() = wrapped.ip
        set(value)
        {
            wrapped.ip = value
        }
    override val requestedState: IEnumConnectionState
        get() = wrapped.requestedState.wrap()
}

fun ICPacketHandshake.unwrap(): C00Handshake = (this as CPacketHandshakeImpl<*>).wrapped
fun C00Handshake.wrap(): ICPacketHandshake = CPacketHandshakeImpl(this)
