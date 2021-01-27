package net.ccbluex.liquidbounce.injection.backend

import net.ccbluex.liquidbounce.api.minecraft.network.play.client.ISPacketChat
import net.ccbluex.liquidbounce.api.minecraft.util.IIChatComponent
import net.minecraft.network.play.server.S02PacketChat

class SPacketChatImpl<out T : S02PacketChat>(wrapped: T) : PacketImpl<T>(wrapped), ISPacketChat
{
	override val chatComponent: IIChatComponent
		get() = wrapped.chatComponent.wrap()
}

fun ISPacketChat.unwrap(): S02PacketChat = (this as SPacketChatImpl<*>).wrapped
fun S02PacketChat.wrap(): ISPacketChat = SPacketChatImpl(this)
