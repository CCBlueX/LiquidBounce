package net.ccbluex.liquidbounce.features.module.modules.misc;

import java.util.regex.Pattern;

import net.ccbluex.liquidbounce.event.EventTarget;
import net.ccbluex.liquidbounce.event.PacketEvent;
import net.ccbluex.liquidbounce.features.module.Module;
import net.ccbluex.liquidbounce.features.module.ModuleCategory;
import net.ccbluex.liquidbounce.features.module.ModuleInfo;
import net.ccbluex.liquidbounce.value.TextValue;
import net.minecraft.network.play.server.S02PacketChat;
import net.minecraft.network.play.server.S45PacketTitle;

@ModuleInfo(name = "AutoLogin", description = "Automatically log-in or register with specified password.", category = ModuleCategory.MISC)
public class AutoLogin extends Module
{
	private static final Pattern loginPattern = Pattern.compile("/([lL])ogin ([\\[<(]).*([]>)])");
	private static final Pattern registerPattern = Pattern.compile("/([rR])egister ([\\[<(]).*([]>)])");
	private static final Pattern registerPattern2 = Pattern.compile("/([rR])egister ([\\[<(]).*([]>)]) ([\\[<(]).*([]>)])");

	private final TextValue password = new TextValue("Password", "123123");

	@EventTarget
	public final void onPacket(final PacketEvent event)
	{
		if (mc.getThePlayer() == null)
			return;

		if (event.getPacket() instanceof S02PacketChat)
		{
			final S02PacketChat chat = (S02PacketChat) event.getPacket();
			if (loginPattern.matcher(chat.getChatComponent().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/login " + password.get());

			if (registerPattern.matcher(chat.getChatComponent().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/register " + password.get());

			if (registerPattern2.matcher(chat.getChatComponent().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/register " + password.get() + " " + password.get());
		}
		else if (event.getPacket() instanceof S45PacketTitle)
		{
			final S45PacketTitle title = (S45PacketTitle) event.getPacket();
			if (loginPattern.matcher(title.getMessage().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/login " + password.get());

			if (registerPattern.matcher(title.getMessage().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/register " + password.get());

			if (registerPattern2.matcher(title.getMessage().getUnformattedText()).find())
				mc.getThePlayer().sendChatMessage("/register " + password.get() + " " + password.get());
		}
	}

	@Override
	public final String getTag()
	{
		return loginPattern.pattern();
	}
}
