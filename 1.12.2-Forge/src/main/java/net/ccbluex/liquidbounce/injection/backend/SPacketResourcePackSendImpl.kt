/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketResourcePackSend
import net.minecraft.network.play.server.SPacketResourcePackSend

class SPacketResourcePackSendImpl<T : SPacketResourcePackSend>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketResourcePackSend {
    override val url: String
        get() = wrapped.url
    override val hash: String
        get() = wrapped.hash
}

inline fun ISPacketResourcePackSend.unwrap(): SPacketResourcePackSend = (this as SPacketResourcePackSendImpl<*>).wrapped
inline fun SPacketResourcePackSend.wrap(): ISPacketResourcePackSend = SPacketResourcePackSendImpl(this)