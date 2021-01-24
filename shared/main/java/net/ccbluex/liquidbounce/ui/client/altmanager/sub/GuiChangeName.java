/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import java.io.IOException;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.event.SessionEvent;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;

import org.lwjgl.input.Keyboard;

public class GuiChangeName extends WrappedGuiScreen
{

	private final GuiAltManager prevGui;
	private IGuiTextField name;
	private String status;

	public GuiChangeName(final GuiAltManager gui)
	{
		prevGui = gui;
	}

	public void initGui()
	{
		Keyboard.enableRepeatEvents(true);
		representedScreen.getButtonList().add(classProvider.createGuiButton(1, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 96, "Change"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(0, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 120, "Back"));

		name = classProvider.createGuiTextField(2, Fonts.font40, representedScreen.getWidth() / 2 - 100, 60, 200, 20);
		name.setFocused(true);
		name.setText(mc.getSession().getUsername());
		name.setMaxStringLength(16);
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		representedScreen.drawBackground(0);
		RenderUtils.drawRect(30, 30, representedScreen.getWidth() - 30, representedScreen.getHeight() - 30, Integer.MIN_VALUE);

		Fonts.font40.drawCenteredString("Change Name", representedScreen.getWidth() / 2.0f, 34, 0xffffff);
		Fonts.font40.drawCenteredString(status == null ? "" : status, representedScreen.getWidth() / 2.0f, representedScreen.getHeight() / 4.0f + 84, 0xffffff);
		name.drawTextBox();

		if (name.getText().isEmpty() && !name.isFocused())
			Fonts.font40.drawCenteredString("\u00A77Username", representedScreen.getWidth() / 2.0f - 74, 66, 0xffffff);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void actionPerformed(final IGuiButton button) {
		switch (button.getId())
		{
			case 0:
				mc.displayGuiScreen(prevGui.representedScreen);
				break;
			case 1:
				if (name.getText().isEmpty())
				{
					status = "\u00A7cEnter a name!";
					return;
				}

				if (!name.getText().equalsIgnoreCase(mc.getSession().getUsername()))
				{
					status = "\u00A7cJust change the upper and lower case!";
					return;
				}

				mc.setSession(classProvider.createSession(name.getText(), mc.getSession().getPlayerId(), mc.getSession().getToken(), mc.getSession().getSessionType()));
				LiquidBounce.eventManager.callEvent(new SessionEvent());
				status = "\u00A7aChanged name to \u00A77" + name.getText() + "\u00A7c.";
				prevGui.status = status;
				mc.displayGuiScreen(prevGui.representedScreen);
				break;
		}
	}

	@Override
	public void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		if (Keyboard.KEY_ESCAPE == keyCode)
		{
			mc.displayGuiScreen(prevGui.getRepresentedScreen());
			return;
		}

		if (name.isFocused())
			name.textboxKeyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
	{
		name.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen()
	{
		name.updateCursorCounter();
		super.updateScreen();
	}

	@Override
	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
		super.onGuiClosed();
	}
}
