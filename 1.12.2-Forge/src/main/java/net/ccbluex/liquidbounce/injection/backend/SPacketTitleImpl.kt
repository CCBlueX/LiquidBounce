package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.server.ISPacketTitle
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.minecraft.network.play.server.SPacketTitle

class SPacketTitleImpl<out T : SPacketTitle>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketTitle
{
	override val message: IIChatComponent
		get() = wrapped.message.wrap()
}

fun ISPacketTitle.unwrap(): SPacketTitle = (this as SPacketTitleImpl<*>).wrapped
fun SPacketTitle.wrap(): ISPacketTitle = SPacketTitleImpl(this)
