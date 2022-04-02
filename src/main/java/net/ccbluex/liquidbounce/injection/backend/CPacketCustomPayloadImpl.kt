/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketCustomPayload
import net.ccbluex.liquidbounce.api.network.IPacketBuffer
import net.minecraft.network.play.client.C17PacketCustomPayload

class CPacketCustomPayloadImpl<T : C17PacketCustomPayload>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketCustomPayload {
    override var data: IPacketBuffer
        get() = wrapped.data.wrap()
        set(value) {
            wrapped.data = value.unwrap()
        }
    override val channelName: String
        get() = wrapped.channelName

}

inline fun ICPacketCustomPayload.unwrap(): C17PacketCustomPayload = (this as CPacketCustomPayloadImpl<*>).wrapped
inline fun C17PacketCustomPayload.wrap(): ICPacketCustomPayload = CPacketCustomPayloadImpl(this)