/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager;

import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.thealtening.AltService;
import com.thealtening.AltService.EnumAltService;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.*;
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator.GuiMCLeaks;
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator.GuiTheAltening;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.WorkerUtils;
import net.ccbluex.liquidbounce.utils.login.LoginUtils;
import net.ccbluex.liquidbounce.utils.login.LoginUtils.LoginResult;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType;
import net.ccbluex.liquidbounce.utils.login.UserUtils;
import net.ccbluex.liquidbounce.utils.misc.HttpUtils;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import net.mcleaks.MCLeaks;

import org.lwjgl.input.Keyboard;

public class GuiAltManager extends WrappedGuiScreen
{

	public static final AltService altService = new AltService();
	private static final Map<String, Boolean> GENERATORS = new HashMap<>();
	private final IGuiScreen prevGui;
	public String status = "\u00A77Idle...";
	IGuiButton loginButton;
	IGuiButton randomButton;
	GuiAltsList altsList;
	private IGuiTextField searchField;

	public GuiAltManager(final IGuiScreen prevGui)
	{
		this.prevGui = prevGui;
	}

	public static void loadGenerators()
	{
		try
		{
			// Read versions json from cloud
			final JsonElement jsonElement = new JsonParser().parse(HttpUtils.get(LiquidBounce.CLIENT_CLOUD + "/generators.json"));

			// Check json is valid object
			if (jsonElement.isJsonObject())
			{
				// Get json object of element
				final JsonObject jsonObject = jsonElement.getAsJsonObject();

				jsonObject.entrySet().forEach(stringJsonElementEntry -> GENERATORS.put(stringJsonElementEntry.getKey(), stringJsonElementEntry.getValue().getAsBoolean()));
			}
		}
		catch (final Throwable throwable)
		{
			// Print throwable to console
			ClientUtils.getLogger().error("Failed to load enabled generators.", throwable);
		}
	}

	public static String login(final MinecraftAccount minecraftAccount)
	{
		if (minecraftAccount == null)
			return "";

		if (AltServiceType.MOJANG.equals(minecraftAccount.getServiceType()) && altService.getCurrentService() != EnumAltService.MOJANG)
			try
			{
				altService.switchService(EnumAltService.MOJANG);
			}
			catch (final NoSuchFieldException | IllegalAccessException e)
			{
				ClientUtils.getLogger().error("Something went wrong while trying to switch alt service.", e);
			}

		// Cracked account (not premium) login
		if (minecraftAccount.isCracked())
		{
			LoginUtils.loginCracked(minecraftAccount.getName());
			MCLeaks.remove();
			return "\u00A7cYour name is now \u00A78" + minecraftAccount.getName() + "\u00A7c.";
		}

		final LoginResult result = LoginUtils.login(minecraftAccount.getServiceType(), minecraftAccount.getName(), minecraftAccount.getPassword());

		switch (result)
		{
			case LOGGED_IN:
			{
				if (!AltServiceType.MCLEAKS.equals(minecraftAccount.getServiceType()))
					MCLeaks.remove();

				final String userName = mc.getSession().getUsername();
				minecraftAccount.setAccountName(userName);
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
				return "\u00A7aYour name is now \u00A7b\u00A7l" + userName + "\u00A7c.";
			}
			case AUTHENTICATION_FAILURE:
			{
				return "\u00A7cAuthentication failed. Please check e-mail and password.";
			}
			case AUTHENTICATION_UNAVAILABLE:
			{
				return "\u00A7cCannot contact authentication server.";
			}
			case INVALID_ACCOUNT_DATA:
			{
				switch (minecraftAccount.getServiceType())
				{
					case MCLEAKS:
						minecraftAccount.setServiceType(AltServiceType.MCLEAKS_INVALID);
						// noinspection fallthrough
					case MCLEAKS_INVALID:
						FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
						return "\u00A7cThe MCLeaks token has to be 16 characters long!";

					default:
						minecraftAccount.setServiceType(AltServiceType.MOJANG_INVALID);
						FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);

						return "\u00A7cInvalid username or wrong password or the account is get mojang-banned.";
				}
			}
			case MIGRATED:
			{
				minecraftAccount.setServiceType(AltServiceType.MOJANG_MIGRATED);
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
				return "\u00A7cAccount migrated.";
			}
			case MCLEAKS_INVALID:
			{
				minecraftAccount.setServiceType(AltServiceType.MCLEAKS_INVALID);
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);

				return "\u00A7cMCLeaks token invalid or expired.";
			}
			case THEALTENING_INVALID:
			{
				minecraftAccount.setServiceType(AltServiceType.THEALTENING_INVALID);
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
				return "\u00A7cTheAltening token invalid or expired.";
			}
			default:
				return "";
		}
	}

	public static boolean canMarkBannedCurrent(final String serverIp)
	{
		if (serverIp == null)
			return true;

		return LiquidBounce.fileManager.accountsConfig.getAccounts().stream().filter(acc -> mc.getSession().getProfile().getName().equalsIgnoreCase(acc.getName()) || mc.getSession().getProfile().getName().equalsIgnoreCase(acc.getAccountName())).findFirst().map(acc -> !acc.getBannedServers().contains(serverIp)).orElse(true);
	}

	public static void toggleMarkBanned(final String serverIp)
	{
		for (final MinecraftAccount acc : LiquidBounce.fileManager.accountsConfig.getAccounts())
			if (mc.getSession().getProfile().getName().equalsIgnoreCase(acc.getName()) || mc.getSession().getProfile().getName().equalsIgnoreCase(acc.getAccountName()))
			{
				final boolean canmarkbanned = canMarkBannedCurrent(serverIp);
				if (canmarkbanned)
					acc.getBannedServers().add(serverIp);
				else
					acc.getBannedServers().remove(serverIp);
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
				ClientUtils.getLogger().info("Marked account {} {} on {}", acc.getName(), canmarkbanned ? "banned" : "un-banned", serverIp);
			}
	}

	public void initGui()
	{
		final int width = representedScreen.getWidth();
		final int textFieldWidth = Math.max(width / 8, 70);

		searchField = classProvider.createGuiTextField(2, Fonts.font40, width - textFieldWidth - 10, 10, textFieldWidth, 20);
		searchField.setMaxStringLength(Integer.MAX_VALUE);

		altsList = new GuiAltsList(representedScreen);
		altsList.represented.registerScrollButtons(7, 8);

		int index = -1;

		// Find the current logged-on account and automatically select it
		for (int i = 0, j = LiquidBounce.fileManager.accountsConfig.getAccounts().size(); i < j; i++)
		{
			final MinecraftAccount minecraftAccount = LiquidBounce.fileManager.accountsConfig.getAccounts().get(i);

			if (minecraftAccount != null && ((minecraftAccount.getPassword() == null || minecraftAccount.getPassword().isEmpty()) && minecraftAccount.getName() != null && minecraftAccount.getName().equals(mc.getSession().getUsername()) || minecraftAccount.getAccountName() != null && minecraftAccount.getAccountName().equals(mc.getSession().getUsername())))
			{
				index = i;
				break;
			}
		}

		altsList.elementClicked(index, false, 0, 0);
		altsList.represented.scrollBy(index * altsList.represented.getSlotHeight());

		final int j = 22;
		representedScreen.getButtonList().add(classProvider.createGuiButton(1, width - 80, j + 24, 70, 20, "Add"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(2, width - 80, j + (24 << 1), 70, 20, "Remove"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(8, width - 80, j + 24 * 3, 70, 20, "Copy"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(7, width - 80, j + (24 << 2), 70, 20, "Import"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(12, width - 80, j + 24 * 5, 70, 20, "Export"));

		representedScreen.getButtonList().add(classProvider.createGuiButton(0, width - 80, representedScreen.getHeight() - 65, 70, 20, "Back"));

		representedScreen.getButtonList().add(loginButton = classProvider.createGuiButton(3, 5, j + 24, 90, 20, "Login"));
		representedScreen.getButtonList().add(randomButton = classProvider.createGuiButton(4, 5, j + (24 << 1), 90, 20, "Random"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(6, 5, j + 24 * 3, 90, 20, "Direct Login"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(88, 5, j + (24 << 2), 90, 20, "Change Name"));

		if (GENERATORS.getOrDefault("mcleaks", true))
			representedScreen.getButtonList().add(classProvider.createGuiButton(5, 5, j + 24 * 5 + 5, 90, 20, "MCLeaks"));
		if (GENERATORS.getOrDefault("thealtening", true))
			representedScreen.getButtonList().add(classProvider.createGuiButton(9, 5, j + 24 * 6 + 5, 90, 20, "TheAltening"));

		representedScreen.getButtonList().add(classProvider.createGuiButton(10, 5, j + 24 * 7 + 5, 90, 20, "Session Login"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(11, 5, j + (24 << 3) + 10, 90, 20, "Cape"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(99, 5, 248, 90, 20, "Reconnect to last server"));

		representedScreen.getButtonList().add(classProvider.createGuiButton(13, width - 80, 166, 70, 20, "Banned Servers"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(14, width - 80, 190, 70, 20, "Copy Current Session Into Clipboard"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(15, width - 80, 214, 70, 20, "Current Session Info"));
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		final int width = representedScreen.getWidth();
		representedScreen.drawBackground(0);

		altsList.represented.drawScreen(mouseX, mouseY, partialTicks);

		Fonts.font40.drawCenteredString("AltManager", width / 2.0f, 6, 0xffffff);
		Fonts.font35.drawCenteredString(searchField.getText().isEmpty() ? LiquidBounce.fileManager.accountsConfig.getAccounts().size() + " Alts" : altsList.accounts.size() + " Search Results", width / 2.0f, 18, 0xffffff);
		Fonts.font35.drawCenteredString(status, width / 2.0f, 32, 0xffffff);
		Fonts.font35.drawStringWithShadow("\u00A77User: \u00A7a" + (MCLeaks.isAltActive() ? MCLeaks.getSession().getUsername() : mc.getSession().getUsername()), 6, 6, 0xffffff);
		Fonts.font35.drawStringWithShadow("\u00A77Type: \u00A7a" + (altService.getCurrentService() == EnumAltService.THEALTENING ? "\u00A7aTheAltening" : MCLeaks.isAltActive() ? "\u00A7bMCLeaks" : UserUtils.INSTANCE.isValidTokenOffline(mc.getSession().getToken()) ? "\u00A76Mojang" : "\u00A78Cracked"), 6, 15, 0xffffff);

		searchField.drawTextBox();

		if (searchField.getText().isEmpty() && !searchField.isFocused())
			Fonts.font40.drawStringWithShadow("\u00A77Search...", searchField.getXPosition() + 4, 17, 0xffffff);

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
				mc.displayGuiScreen(prevGui);
				break;
			case 1:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiAdd(this)));
				break;
			case 2:
				if (altsList.getSelectedSlot() != -1 && altsList.getSelectedSlot() < altsList.getSize())
				{
					LiquidBounce.fileManager.accountsConfig.removeAccount(altsList.accounts.get(altsList.getSelectedSlot()));
					FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);
					status = "\u00A7aThe account has been removed.";

					altsList.updateAccounts(searchField.getText());
				}
				else
					status = "\u00A7cSelect an account.";
				break;
			case 3:
				if (altsList.getSelectedSlot() != -1 && altsList.getSelectedSlot() < altsList.getSize())
				{
					loginButton.setEnabled(false);
					randomButton.setEnabled(false);

					WorkerUtils.getWorkers().submit(() ->
					{
						try
						{
							final MinecraftAccount minecraftAccount = altsList.accounts.get(altsList.getSelectedSlot());
							status = "\u00A7aLogging in...";
							status = login(minecraftAccount);
						}
						finally
						{
							loginButton.setEnabled(true);
							randomButton.setEnabled(true);
						}
					});
				}
				else
					status = "\u00A7cSelect an account.";
				break;
			case 4:
				if (altsList.accounts.isEmpty())
				{
					status = "\u00A7cThe list is empty.";
					return;
				}

				final int randomInteger = new Random().nextInt(altsList.accounts.size());

				if (randomInteger < altsList.getSize())
					altsList.selectedSlot = randomInteger;

				loginButton.setEnabled(false);
				randomButton.setEnabled(false);

				WorkerUtils.getWorkers().submit(() ->
				{
					try
					{
						final MinecraftAccount minecraftAccount = altsList.accounts.get(randomInteger);
						status = "\u00A7aLogging in...";
						status = login(minecraftAccount);
					}
					finally
					{
						loginButton.setEnabled(true);
						randomButton.setEnabled(true);
					}
				});
				break;
			case 5:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiMCLeaks(this)));
				break;
			case 6:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiDirectLogin(this)));
				break;
			case 7:
				final File file = MiscUtils.openFileChooser();

				if (file == null)
					return;

				final BufferedReader bufferedReader = MiscUtils.createBufferedFileReader(file);

				String line;
				while ((line = bufferedReader.readLine()) != null)
				{
					final String[] accountData = line.split(":", 2);

					if (!LiquidBounce.fileManager.accountsConfig.isAccountExists(accountData[0]))
						LiquidBounce.fileManager.accountsConfig.addAccount(accountData.length > 1 ? new MinecraftAccount(AltServiceType.MOJANG, accountData[0], accountData[1]) : new MinecraftAccount(AltServiceType.MOJANG, accountData[0]));
				}

				bufferedReader.close();

				altsList.updateAccounts(searchField.getText());
				FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig);

				status = "\u00A7aThe accounts were imported successfully.";
				break;
			case 8:
				if (altsList.getSelectedSlot() != -1 && altsList.getSelectedSlot() < altsList.getSize())
				{
					final MinecraftAccount minecraftAccount = altsList.accounts.get(altsList.getSelectedSlot());

					if (minecraftAccount == null)
						break;

					Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(minecraftAccount.getName() + ":" + minecraftAccount.getPassword()), null);
					status = "\u00A7aCopied account into your clipboard.";
				}
				else
					status = "\u00A7cSelect an account.";
				break;
			case 88:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiChangeName(this)));
				break;
			case 9:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiTheAltening(this)));
				break;
			case 10:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiSessionLogin(representedScreen)));
				break;
			case 11:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiDonatorCape(this)));
				break;
			case 12:
				if (LiquidBounce.fileManager.accountsConfig.getAccounts().size() == 0)
				{
					status = "\u00A7cThe list is empty.";
					return;
				}

				final File selectedFile = MiscUtils.saveFileChooser();

				if (selectedFile == null || selectedFile.isDirectory())
					return;

				try
				{
					if (!selectedFile.exists())
						selectedFile.createNewFile();

					final BufferedWriter fileWriter = MiscUtils.createBufferedFileWriter(selectedFile);

					for (final MinecraftAccount account : LiquidBounce.fileManager.accountsConfig.getAccounts())
						fileWriter.write(account.isCracked() ? account.getName()  + System.lineSeparator() : account.getName() + ":" + account.getPassword()  + System.lineSeparator());

					fileWriter.flush();
					fileWriter.close();
					JOptionPane.showMessageDialog(null, "Exported successfully!", "AltManager", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					MiscUtils.showErrorPopup("Error", "Exception class: " + e.getClass().getName() + "\nMessage: " + e.getMessage());
				}
				break;
			case 99:
				ServerUtils.connectToLastServer();
				break;
			case 13:
				if (altsList.getSelectedSlot() != -1 && altsList.getSelectedSlot() < altsList.getSize())
					mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiBannedServers(this, LiquidBounce.fileManager.accountsConfig.getAccounts().get(altsList.getSelectedSlot()))));
				else
					status = "\u00A7cSelect an account.";
				break;
			case 14:
				Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(mc.getSession().getSessionID()), null);
				status = "\u00A7aCopied current session id into your clipboard.";
				break;
			case 15:
				mc.displayGuiScreen(classProvider.wrapGuiScreen(new GuiSessionInfo(representedScreen, null)));
				break;
		}
	}

	@Override
	public void keyTyped(final char typedChar, final int keyCode) {
		if (searchField.isFocused())
		{
			searchField.textboxKeyTyped(typedChar, keyCode);
			altsList.updateAccounts(searchField.getText());
		}

		switch (keyCode)
		{
			case Keyboard.KEY_ESCAPE:
				mc.displayGuiScreen(prevGui);
				return;
			case Keyboard.KEY_UP:
			{
				int i = altsList.getSelectedSlot() - 1;
				if (i < 0)
					i = 0;
				altsList.elementClicked(i, false, 0, 0);
				break;
			}
			case Keyboard.KEY_DOWN:
			{
				int i = altsList.getSelectedSlot() + 1;
				if (i >= altsList.getSize())
					i = altsList.getSize() - 1;
				altsList.elementClicked(i, false, 0, 0);
				break;
			}
			case Keyboard.KEY_RETURN:
			{
				altsList.elementClicked(altsList.getSelectedSlot(), true, 0, 0);
				break;
			}
			case Keyboard.KEY_NEXT:
			{
				altsList.represented.scrollBy(representedScreen.getHeight() - 100);
				break;
			}
			case Keyboard.KEY_PRIOR:
			{
				altsList.represented.scrollBy(-representedScreen.getHeight() + 100);
				return;
			}
		}

		representedScreen.superKeyTyped(typedChar, keyCode);
	}

	@Override
	public void handleMouseInput()
	{
		representedScreen.superHandleMouseInput();

		altsList.represented.handleMouseInput();
	}

	@Override
	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) {
		searchField.mouseClicked(mouseX, mouseY, mouseButton);

		representedScreen.superMouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen()
	{
		searchField.updateCursorCounter();
	}

	private class GuiAltsList extends WrappedGuiSlot
	{
		@SuppressWarnings("WeakerAccess")
		List<MinecraftAccount> accounts;
		int selectedSlot;

		GuiAltsList(final IGuiScreen prevGui)
		{
			super(mc, prevGui.getWidth(), prevGui.getHeight(), 40, prevGui.getHeight() - 40, 30);

			updateAccounts(null);
		}

		void updateAccounts(String search)
		{
			if (search == null || search.isEmpty())
			{
				accounts = LiquidBounce.fileManager.accountsConfig.getAccounts();
				return;
			}

			search = search.toLowerCase();

			accounts = new ArrayList<>();

			for (final MinecraftAccount account : LiquidBounce.fileManager.accountsConfig.getAccounts())
				if (account.getName() != null && account.getName().toLowerCase().contains(search) || account.getAccountName() != null && account.getAccountName().toLowerCase().contains(search))
					accounts.add(account);
		}

		@Override
		public boolean isSelected(final int id)
		{
			return selectedSlot == id;
		}

		int getSelectedSlot()
		{
			if (selectedSlot > accounts.size())
				selectedSlot = -1;
			return selectedSlot;
		}

//		public void setSelectedSlot(final int selectedSlot)
//		{
//			this.selectedSlot = selectedSlot;
//		}

		@Override
		public int getSize()
		{
			return accounts.size();
		}

		@Override
		public void elementClicked(final int id, final boolean doubleClick, final int var3, final int var4)
		{
			selectedSlot = id;

			if (doubleClick)
				if (altsList.getSelectedSlot() != -1 && altsList.getSelectedSlot() < altsList.getSize() && loginButton.getEnabled())
				{
					loginButton.setEnabled(false);
					randomButton.setEnabled(false);

					WorkerUtils.getWorkers().submit(() ->
					{
						final MinecraftAccount minecraftAccount = accounts.get(altsList.getSelectedSlot());
						status = "\u00A7aLogging in...";
						status = "\u00A7c" + login(minecraftAccount);

						loginButton.setEnabled(true);
						randomButton.setEnabled(true);
					});
				}
				else
					status = "\u00A7cSelect an account.";
		}

		@Override
		public void drawSlot(final int id, final int x, final int y, final int var4, final int mouseXIn, final int mouseYIn)
		{
			final int width = getRepresented().getWidth();
			final MinecraftAccount minecraftAccount = accounts.get(id);
			final AltServiceType serviceType = minecraftAccount.getServiceType();
			final boolean isInvalid = serviceType == AltServiceType.MOJANG_INVALID || serviceType == AltServiceType.MOJANG_MIGRATED || serviceType == AltServiceType.MCLEAKS_INVALID || serviceType == AltServiceType.THEALTENING_INVALID;

			Fonts.font40.drawCenteredString(minecraftAccount.getAccountName() == null ? minecraftAccount.getName() : minecraftAccount.getAccountName(), width / 2, y + 2, Color.WHITE.getRGB(), true);
			Fonts.font40.drawCenteredString(minecraftAccount.isCracked() ? "Cracked" : minecraftAccount.getServiceType().getId(), width / 2, y + 10, minecraftAccount.isCracked() ? Color.GRAY.getRGB() : minecraftAccount.getAccountName() == null ? Color.LIGHT_GRAY.getRGB() : isInvalid ? Color.RED.getRGB() : Color.GREEN.getRGB(), true);
			if (!minecraftAccount.getBannedServers().isEmpty())
				Fonts.font35.drawCenteredString("Banned on " + minecraftAccount.serializeBannedServers(), width / 2, y + 20, Color.RED.getRGB(), true);
		}

		@Override
		public void drawBackground()
		{
		}
	}
}
