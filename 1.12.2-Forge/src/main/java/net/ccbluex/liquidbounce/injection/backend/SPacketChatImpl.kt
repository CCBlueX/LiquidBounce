package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ISPacketChat
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.minecraft.network.play.server.SPacketChat

class SPacketChatImpl<out T : SPacketChat>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketChat
{
	override val chatComponent: IIChatComponent
		get() = wrapped.chatComponent.wrap()
}

fun ISPacketChat.unwrap(): SPacketChat = (this as SPacketChatImpl<*>).wrapped
fun SPacketChat.wrap(): ISPacketChat = SPacketChatImpl(this)
