package net.ccbluex.liquidbounce.features.module.modules.misc

import net.ccbluex.liquidbounce.event.EventTarget
import net.ccbluex.liquidbounce.event.PacketEvent
import net.ccbluex.liquidbounce.features.module.Module
import net.ccbluex.liquidbounce.features.module.ModuleCategory
import net.ccbluex.liquidbounce.features.module.ModuleInfo
import net.ccbluex.liquidbounce.value.TextValue
import java.util.regex.Pattern

// TODO: Add Delay option
@ModuleInfo(name = "AutoLogin", description = "Automatically log-in or register with specified password.", category = ModuleCategory.MISC)
class AutoLogin : Module()
{
	private val password = TextValue("Password", "123123")

	@EventTarget
	fun onPacket(event: PacketEvent)
	{
		val thePlayer = mc.thePlayer ?: return
		if (classProvider.isSPacketChat(event.packet))
		{
			val chat = event.packet.asSPacketChat().chatComponent.unformattedText
			if (loginPattern.matcher(chat).find()) thePlayer.sendChatMessage("/login " + password.get())
			if (registerPattern.matcher(chat).find()) thePlayer.sendChatMessage("/register " + password.get())
			if (registerPattern2.matcher(chat).find()) thePlayer.sendChatMessage("/register " + password.get() + " " + password.get())
		}
		else if (classProvider.isSPacketTitle(event.packet))
		{
			val title = event.packet.asSPacketTitle().message.unformattedText
			if (loginPattern.matcher(title).find()) thePlayer.sendChatMessage("/login " + password.get())
			if (registerPattern.matcher(title).find()) thePlayer.sendChatMessage("/register " + password.get())
			if (registerPattern2.matcher(title).find()) thePlayer.sendChatMessage("/register " + password.get() + " " + password.get())
		}
	}

	companion object
	{
		private val loginPattern = Pattern.compile("/([lL])ogin ([\\[<(]).*([]>)])")
		private val registerPattern = Pattern.compile("/([rR])egister ([\\[<(]).*([]>)])")
		private val registerPattern2 = Pattern.compile("/([rR])egister ([\\[<(]).*([]>)]) ([\\[<(]).*([]>)])")
	}
}
