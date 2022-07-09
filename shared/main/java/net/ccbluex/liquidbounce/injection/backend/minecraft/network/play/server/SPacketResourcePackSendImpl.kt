/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */

package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketResourcePackSend
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.minecraft.network.play.server.S48PacketResourcePackSend

class SPacketResourcePackSendImpl<out T : S48PacketResourcePackSend>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketResourcePackSend
{
    override val url: String
        get() = wrapped.url
    override val hash: String
        get() = wrapped.hash
}

fun ISPacketResourcePackSend.unwrap(): S48PacketResourcePackSend = (this as SPacketResourcePackSendImpl<*>).wrapped
fun S48PacketResourcePackSend.wrap(): ISPacketResourcePackSend = SPacketResourcePackSendImpl(this)
