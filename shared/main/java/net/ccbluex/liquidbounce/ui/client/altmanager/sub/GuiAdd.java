/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.Proxy;
import java.util.Optional;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.thealtening.AltService.EnumAltService;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.TabUtils;
import net.ccbluex.liquidbounce.utils.WorkerUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;

import org.lwjgl.input.Keyboard;

public class GuiAdd extends WrappedGuiScreen
{

	private final GuiAltManager prevGui;

	private IGuiButton addButton;
	private IGuiButton clipboardButton;
	private IGuiTextField username;
	private IGuiTextField password;

	private String status = "\u00A77Idle...";

	public GuiAdd(final GuiAltManager gui)
	{
		prevGui = gui;
	}

	public void initGui()
	{
		Keyboard.enableRepeatEvents(true);
		representedScreen.getButtonList().add(addButton = classProvider.createGuiButton(1, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 72, "Add"));
		representedScreen.getButtonList().add(clipboardButton = classProvider.createGuiButton(2, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 96, "Clipboard"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(0, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 120, "Back"));
		username = classProvider.createGuiTextField(2, Fonts.font40, representedScreen.getWidth() / 2 - 100, 60, 200, 20);
		username.setFocused(true);
		username.setMaxStringLength(Integer.MAX_VALUE);
		password = classProvider.createGuiPasswordField(3, Fonts.font40, representedScreen.getWidth() / 2 - 100, 85, 200, 20);
		password.setMaxStringLength(Integer.MAX_VALUE);
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		representedScreen.drawBackground(0);
		RenderUtils.drawRect(30, 30, representedScreen.getWidth() - 30, representedScreen.getHeight() - 30, Integer.MIN_VALUE);

		Fonts.font40.drawCenteredString("Add Account", representedScreen.getWidth() / 2.0f, 34, 0xffffff);
		Fonts.font35.drawCenteredString(Optional.ofNullable(status).orElse(""), representedScreen.getWidth() / 2.0f, representedScreen.getHeight() / 4.0f + 60, 0xffffff);

		username.drawTextBox();
		password.drawTextBox();

		if (username.getText().isEmpty() && !username.isFocused())
			Fonts.font40.drawCenteredString("\u00A77Username / E-Mail", representedScreen.getWidth() / 2 - 55, 66, 0xffffff);

		if (password.getText().isEmpty() && !password.isFocused())
			Fonts.font40.drawCenteredString("\u00A77Password", representedScreen.getWidth() / 2 - 74, 91, 0xffffff);
		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void actionPerformed(final IGuiButton button) throws IOException
	{
		if (!button.getEnabled())
			return;

		switch (button.getId())
		{
			case 0:
				mc.displayGuiScreen(prevGui.representedScreen);
				break;
			case 1:
				if (LiquidBounce.fileManager.accountsConfig.isAccountExists(username.getText()))
				{
					status = "\u00A7cThe account has already been added.";
					break;
				}

				addAccount(username.getText(), password.getText());
				break;
			case 2:
				try
				{
					final String clipboardData = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
					final String[] accountData = clipboardData.split(":", 2);

					if (!clipboardData.contains(":") || accountData.length != 2)
					{
						status = "\u00A7cInvalid clipboard data. (Use: E-Mail:Password)";
						return;
					}

					addAccount(accountData[0], accountData[1]);
				}
				catch (final UnsupportedFlavorException e)
				{
					status = "\u00A7cClipboard flavor unsupported!";
					ClientUtils.getLogger().error("Failed to read data from clipboard.", e);
				}
				break;
		}
	}

	@Override
	public void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		switch (keyCode)
		{
			case Keyboard.KEY_ESCAPE:
				mc.displayGuiScreen(prevGui.representedScreen);
				return;
			case Keyboard.KEY_TAB:
				TabUtils.tab(username, password);
				return;
			case Keyboard.KEY_RETURN:
				actionPerformed(addButton);
				return;
		}

		if (username.isFocused())
			username.textboxKeyTyped(typedChar, keyCode);

		if (password.isFocused())
			password.textboxKeyTyped(typedChar, keyCode);
		super.keyTyped(typedChar, keyCode);
	}

	@Override
	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
	{
		username.mouseClicked(mouseX, mouseY, mouseButton);
		password.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen()
	{
		username.updateCursorCounter();
		password.updateCursorCounter();

		super.updateScreen();
	}

	@Override
	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
	}

	private void addAccount(final String name, final String password)
	{
		if (LiquidBounce.fileManager.accountsConfig.isAccountExists(name))
		{
			status = "\u00A7cThe account has already been added.";
			return;
		}

		addButton.setEnabled(false);
		clipboardButton.setEnabled(false);

		final MinecraftAccount account = new MinecraftAccount(AltServiceType.MOJANG, name, password);

		WorkerUtils.getWorkers().submit(() ->
		{
			if (!account.isCracked())
			{
				status = "\u00A7aChecking...";

				try
				{
					final EnumAltService oldService = GuiAltManager.altService.getCurrentService();

					if (oldService != EnumAltService.MOJANG)
						GuiAltManager.altService.switchService(EnumAltService.MOJANG);

					final UserAuthentication userAuthentication = new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);

					userAuthentication.setUsername(account.getName());
					userAuthentication.setPassword(account.getPassword());

					userAuthentication.logIn();
					account.setAccountName(userAuthentication.getSelectedProfile().getName());

					if (oldService == EnumAltService.THEALTENING)
						GuiAltManager.altService.switchService(EnumAltService.THEALTENING);
				}
				catch (final NullPointerException | AuthenticationException | NoSuchFieldException | IllegalAccessException e)
				{
					status = "\u00A7cThe account doesn't work.";
					addButton.setEnabled(true);
					clipboardButton.setEnabled(true);
					return;
				}
			}

			LiquidBounce.fileManager.accountsConfig.getAccounts().add(account);
			FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);

			status = "\u00A7aThe account has been added.";
			prevGui.status = status;
			mc.displayGuiScreen(prevGui.representedScreen);
		});
	}
}
