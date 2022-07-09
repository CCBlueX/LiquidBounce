package net.ccbluex.liquidbounce.injection.backend.minecraft.network.play.server

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTitle
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.ccbluex.liquidbounce.injection.backend.minecraft.network.PacketImpl
import net.ccbluex.liquidbounce.injection.backend.minecraft.util.wrap
import net.minecraft.network.play.server.S45PacketTitle

class SPacketTitleImpl<out T : S45PacketTitle>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTitle
{
    override val message: IIChatComponent?
        get() = wrapped.message?.wrap()
}

fun ISPacketTitle.unwrap(): S45PacketTitle = (this as SPacketTitleImpl<*>).wrapped
fun S45PacketTitle.wrap(): ISPacketTitle = SPacketTitleImpl(this)
