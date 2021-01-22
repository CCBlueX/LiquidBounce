/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import java.awt.*;
import java.io.IOException;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;

import org.lwjgl.input.Keyboard;

public class GuiBannedServers extends WrappedGuiScreen
{
	public String status = "\u00A77Idle...";
	private final GuiAltManager prevGui;
	private final MinecraftAccount account;
	GuiServersList serversList;

	public GuiBannedServers(final GuiAltManager gui, final MinecraftAccount acc)
	{
		prevGui = gui;
		account = acc;
	}

	@Override
	public final void initGui()
	{
		final int width = getRepresentedScreen().getWidth();
		final int height = getRepresentedScreen().getHeight();
		serversList = new GuiServersList(this, account);
		serversList.represented.registerScrollButtons(7, 8);

		final int j = 22;
		getRepresentedScreen().getButtonList().add(classProvider.createGuiButton(1, width - 80, 46, 70, 20, "Add"));
		getRepresentedScreen().getButtonList().add(classProvider.createGuiButton(2, width - 80, 70, 70, 20, "Remove"));

		getRepresentedScreen().getButtonList().add(classProvider.createGuiButton(0, width - 80, height - 65, 70, 20, "Back"));
	}

	@Override
	public final void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		final int width = representedScreen.getWidth();
		representedScreen.drawBackground(0);

		serversList.represented.drawScreen(mouseX, mouseY, partialTicks);

		Fonts.font40.drawCenteredString("\u00A7cBanned servers\u00A78 of \u00A7a" + (account.getAccountName() == null ? account.getName() : account.getAccountName()), width / 2, 6, 0xffffff);
		Fonts.font35.drawCenteredString("\u00A7a" + (account.getAccountName() == null ? account.getName() : account.getAccountName()) + "\u00A78 is \u00A7cbanned\u00A78 from \u00A7c" + serversList.getSize() + "\u00A7a servers.", width / 2, 18, 0xffffff);
		Fonts.font35.drawCenteredString(status, representedScreen.getWidth() / 2, 32, 0xffffff);

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public final void actionPerformed(final IGuiButton button) throws IOException
	{
		switch (button.getId())
		{
			case 0:
				mc.displayGuiScreen(prevGui.representedScreen);
				break;
			case 1:
				mc.displayGuiScreen(new GuiAddBanned(this, account).representedScreen);
				break;
			case 2:
				if (serversList.getSelectedSlot() != -1 && serversList.getSelectedSlot() < serversList.getSize())
				{
					account.getBannedServers().remove(account.getBannedServers().get(serversList.getSelectedSlot()));
					LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
					status = "\u00A7aThe server has been removed.";
				}
				else
					status = "\u00A7cSelect a server.";
				break;
		}
		super.actionPerformed(button);
	}

	@Override
	public final void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		switch (keyCode)
		{
			case Keyboard.KEY_ESCAPE:
				mc.displayGuiScreen(prevGui.representedScreen);
				return;
			case Keyboard.KEY_UP:
			{
				int i = serversList.getSelectedSlot() - 1;
				if (i < 0)
					i = 0;
				serversList.elementClicked(i, false, 0, 0);
				break;
			}
			case Keyboard.KEY_DOWN:
				int i = serversList.getSelectedSlot() + 1;
				if (i >= serversList.getSize())
					i = serversList.getSize() - 1;
				serversList.elementClicked(i, false, 0, 0);
				break;
			case Keyboard.KEY_RETURN:
				serversList.elementClicked(serversList.getSelectedSlot(), true, 0, 0);
				break;
			case Keyboard.KEY_NEXT:
				serversList.represented.scrollBy(representedScreen.getHeight() - 100);
				break;
			case Keyboard.KEY_PRIOR:
				serversList.represented.scrollBy(-representedScreen.getHeight() + 100);
				return;
		}

		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public final void handleMouseInput() throws IOException
	{
		super.handleMouseInput();
		serversList.represented.handleMouseInput();
	}

	private class GuiServersList extends WrappedGuiSlot
	{
		private final MinecraftAccount account;
		int selectedSlot;

		GuiServersList(final GuiBannedServers prevGui, final MinecraftAccount acc)
		{
			super(mc, prevGui.representedScreen.getWidth(), prevGui.representedScreen.getHeight(), 40, prevGui.representedScreen.getHeight() - 40, 15);
			account = acc;
		}

		@Override
		public final boolean isSelected(final int id)
		{
			return selectedSlot == id;
		}

		final int getSelectedSlot()
		{
			if (selectedSlot > account.getBannedServers().size())
				selectedSlot = -1;
			return selectedSlot;
		}

		@Override
		public final int getSize()
		{
			return account.getBannedServers().size();
		}

		@Override
		public final void elementClicked(final int id, final boolean doubleClick, final int var3, final int var4)
		{
			selectedSlot = id;
		}

		@Override
		public final void drawSlot(final int id, final int x, final int y, final int var4, final int mouseXIn, final int mouseYIn)
		{
			final String server = account.getBannedServers().get(id);

			Fonts.font40.drawCenteredString(server, represented.getWidth() / 2, y + 1, Color.RED.getRGB(), true);
		}

		@Override
		public void drawBackground()
		{
		}
	}

	private static class GuiAddBanned extends WrappedGuiScreen
	{
		private final GuiBannedServers prevGui;
		private final MinecraftAccount account;
		private IGuiTextField name;
		private String status;

		private GuiAddBanned(final GuiBannedServers prevGui, final MinecraftAccount acc)
		{
			this.prevGui = prevGui;
			account = acc;
		}

		@Override
		public final void initGui()
		{
			final int width = representedScreen.getWidth();
			final int height = representedScreen.getHeight();

			Keyboard.enableRepeatEvents(true);
			representedScreen.getButtonList().add(classProvider.createGuiButton(1, width / 2 - 100, height / 4 + 96, "Add " + (account.getAccountName() == null ? account.getName() : account.getAccountName()) + "'s banned server"));
			representedScreen.getButtonList().add(classProvider.createGuiButton(0, width / 2 - 100, height / 4 + 120, "Back"));

			name = classProvider.createGuiTextField(2, Fonts.font40, width / 2 - 100, 60, 200, 20);
			name.setFocused(true);
			name.setText("");
			name.setMaxStringLength(128);
		}

		@Override
		public final void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
		{
			final int width = representedScreen.getWidth();
			final int height = representedScreen.getHeight();

			representedScreen.drawBackground(0);
			RenderUtils.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE);

			Fonts.font40.drawCenteredString("Add Banned Server", width / 2, 34, 0xffffff);
			if (status != null)
				Fonts.font40.drawCenteredString(status, width / 2, height / 4 + 84, 0xffffff);
			name.drawTextBox();

			if (name.getText().isEmpty() && !name.isFocused())
				Fonts.font40.drawCenteredString("\u00A77Add Server", width / 2 - 74, 66, 0xffffff);

			super.drawScreen(mouseX, mouseY, partialTicks);
		}

		@Override
		public final void actionPerformed(final IGuiButton button) throws IOException
		{
			switch (button.getId())
			{
				case 0:
					mc.displayGuiScreen(prevGui.representedScreen);
					break;
				case 1:
					final String server = name.getText();
					if (server.isEmpty())
					{
						status = "\u00A7cEnter a server address!";
						return;
					}

					if (account.getBannedServers().contains(server))
					{
						status = "\u00A7cServer already exists!";
						return;
					}

					account.getBannedServers().add(server);
					LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);

					prevGui.status = "\u00A7aAdded banned server \u00A7c" + server + " \u00A7aof " + (account.getAccountName() == null ? account.getName() : account.getAccountName()) + "\u00A7c.";
					mc.displayGuiScreen(prevGui.representedScreen);
					break;
			}
			super.actionPerformed(button);
		}

		@Override
		public final void keyTyped(final char typedChar, final int keyCode) throws IOException
		{
			if (keyCode == Keyboard.KEY_ESCAPE)
			{
				mc.displayGuiScreen(prevGui.representedScreen);
				return;
			}

			if (name.isFocused())
				name.textboxKeyTyped(typedChar, keyCode);
			super.keyTyped(typedChar, keyCode);
		}

		@Override
		public final void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
		{
			name.mouseClicked(mouseX, mouseY, mouseButton);
			super.mouseClicked(mouseX, mouseY, mouseButton);
		}

		@Override
		public final void updateScreen()
		{
			name.updateCursorCounter();
			super.updateScreen();
		}

		@Override
		public final void onGuiClosed()
		{
			Keyboard.enableRepeatEvents(false);
			super.onGuiClosed();
		}
	}
}
