/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ICPacketChatMessage
import net.minecraft.network.play.client.CPacketChatMessage

class CPacketChatMessageImpl<T : CPacketChatMessage>(wrapped: T) : PacketImpl<T>(wrapped), ICPacketChatMessage {
    override var message: String
        get() = wrapped.message
        set(value) {
            wrapped.message = value
        }
}

inline fun ICPacketChatMessage.unwrap(): CPacketChatMessage = (this as CPacketChatMessageImpl<*>).wrapped
inline fun CPacketChatMessage.wrap(): ICPacketChatMessage = CPacketChatMessageImpl(this)