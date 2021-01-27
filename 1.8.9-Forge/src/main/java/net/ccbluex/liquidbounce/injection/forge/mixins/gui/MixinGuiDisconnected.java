/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import java.net.Proxy;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

import com.mojang.authlib.Agent;
import com.mojang.authlib.UserAuthentication;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.thealtening.AltService.EnumAltService;
import com.thealtening.api.TheAltening;
import com.thealtening.api.data.AccountData;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.SessionEvent;
import net.ccbluex.liquidbounce.features.special.AntiModDisable;
import net.ccbluex.liquidbounce.features.special.AutoReconnect;
import net.ccbluex.liquidbounce.file.FileManager;
import net.ccbluex.liquidbounce.injection.backend.ClassProviderImpl;
import net.ccbluex.liquidbounce.injection.backend.GuiScreenImplKt;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator.GuiTheAltening;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.login.LoginUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.util.Session;
import net.minecraftforge.fml.client.config.GuiSlider;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiDisconnected.class)
public abstract class MixinGuiDisconnected extends MixinGuiScreen
{
	private static final ThreadLocal<DecimalFormat> DECIMAL_FORMAT = ThreadLocal.withInitial(() -> new DecimalFormat("#0"));

	@Shadow
	private int field_175353_i;

	private GuiButton reconnectButton;
	private GuiSlider autoReconnectDelaySlider;
	private GuiButton forgeBypassButton;
	private GuiButton markBanned;
	private int reconnectTimer;

	@Inject(method = "initGui", at = @At("RETURN"))
	private void initGui(final CallbackInfo callbackInfo)
	{
		reconnectTimer = 0;

		final String lastServerIp = ServerUtils.getLastServerIp();
		final boolean canMarkBannedOnThisServer = GuiAltManager.canMarkBannedCurrent(lastServerIp);

		final int middleWidth = width / 2;
		final int height = this.height / 2 + field_175353_i / 2 + fontRendererObj.FONT_HEIGHT;
		buttonList.add(reconnectButton = new GuiButton(1, middleWidth - 100, height + 22, 98, 20, "Reconnect"));

		drawReconnectDelaySlider();

		buttonList.add(new GuiButton(3, middleWidth - 100, height + 44, 98, 20, GuiTheAltening.Companion.getApiKey().isEmpty() ? "Random alt" : "New TheAltening alt"));
		buttonList.add(new GuiButton(4, middleWidth + 2, height + 44, 98, 20, "Random username"));
		buttonList.add(new GuiButton(5, middleWidth - 100, height + 88, "AltManager"));
		buttonList.add(markBanned = new GuiButton(6, middleWidth - 100, height + 110, "Mark this account " + (canMarkBannedOnThisServer ? "\u00A7cBANNED" : "\u00A7aUN-BANNED") + "\u00A7r from this server"));
		buttonList.add(forgeBypassButton = new GuiButton(7, middleWidth - 100, height + 66, "AntiModDisable: " + (AntiModDisable.enabled ? "\u00A7a(On)" : "\u00A7c(Off)")));

		if (lastServerIp != null)
			markBanned.enabled = GuiAltManager.canEnableMarkBannedButton();

		updateSliderText();
	}

	@Inject(method = "actionPerformed", at = @At("HEAD"))
	private void actionPerformed(final GuiButton button, final CallbackInfo callbackInfo)
	{
		switch (button.id)
		{
			case 1:
				ServerUtils.connectToLastServer();
				break;
			case 3:
				if (!GuiTheAltening.Companion.getApiKey().isEmpty())
				{
					final String apiKey = GuiTheAltening.Companion.getApiKey();
					final TheAltening theAltening = new TheAltening(apiKey);

					try
					{
						final AccountData account = theAltening.getAccountData();
						GuiAltManager.altService.switchService(EnumAltService.THEALTENING);

						final UserAuthentication yggdrasilUserAuthentication = new YggdrasilUserAuthentication(new YggdrasilAuthenticationService(Proxy.NO_PROXY, ""), Agent.MINECRAFT);
						yggdrasilUserAuthentication.setUsername(account.getToken());
						yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME);
						yggdrasilUserAuthentication.logIn();

						mc.session = new Session(yggdrasilUserAuthentication.getSelectedProfile().getName(), yggdrasilUserAuthentication.getSelectedProfile().getId().toString(), yggdrasilUserAuthentication.getAuthenticatedToken(), "mojang");
						LiquidBounce.eventManager.callEvent(new SessionEvent());
						ServerUtils.connectToLastServer();
						break;
					}
					catch (final Throwable throwable)
					{
						ClientUtils.getLogger().error("Failed to login into random account from TheAltening.", throwable);
					}
				}

				final List<MinecraftAccount> accounts = LiquidBounce.fileManager.accountsConfig.getAccounts();
				if (accounts.isEmpty())
					break;

				final MinecraftAccount minecraftAccount = accounts.get(new Random().nextInt(accounts.size()));
				GuiAltManager.login(minecraftAccount);
				ServerUtils.connectToLastServer();
				break;
			case 4:
				LoginUtils.loginCracked(RandomUtils.randomString(RandomUtils.nextInt(5, 16)));
				ServerUtils.connectToLastServer();
				break;
			case 5:
				mc.displayGuiScreen(GuiScreenImplKt.unwrap(ClassProviderImpl.INSTANCE.wrapGuiScreen(new GuiAltManager(null))));
				break;
			case 6:
				final String lastServerIp = ServerUtils.getLastServerIp();
				if (lastServerIp != null)
				{
					GuiAltManager.toggleMarkBanned(lastServerIp);
					markBanned.displayString = "Mark this account " + (GuiAltManager.canMarkBannedCurrent(lastServerIp) ? "\u00A7cBANNED" : "\u00A7aUN-BANNED") + "\u00A7r from this server";
				}
				break;
			case 7:
				AntiModDisable.enabled = !AntiModDisable.enabled;
				forgeBypassButton.displayString = "AntiModDisable: " + (AntiModDisable.enabled ? "\u00A7a(On)" : "\u00A7c(Off)");
				FileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
				break;
		}
	}

	@Override
	public void updateScreen()
	{
		if (AutoReconnect.INSTANCE.isEnabled())
		{
			reconnectTimer++;
			if (reconnectTimer > AutoReconnect.INSTANCE.getDelay() / 50)
				ServerUtils.connectToLastServer();
		}
	}

	@Inject(method = "drawScreen", at = @At("RETURN"))
	private void drawScreen(final CallbackInfo callbackInfo)
	{
		if (AutoReconnect.INSTANCE.isEnabled())
			updateReconnectButton();
	}

	private void drawReconnectDelaySlider()
	{
		buttonList.add(autoReconnectDelaySlider = new GuiSlider(2, width / 2 + 2, height / 2 + field_175353_i / 2 + fontRendererObj.FONT_HEIGHT + 22, 98, 20, "AutoReconnect: ", "ms", AutoReconnect.MIN, AutoReconnect.MAX, AutoReconnect.INSTANCE.getDelay(), false, true, guiSlider ->
		{
			AutoReconnect.INSTANCE.setDelay(guiSlider.getValueInt());

			reconnectTimer = 0;
			updateReconnectButton();
			updateSliderText();
		}));
	}

	private void updateSliderText()
	{
		if (autoReconnectDelaySlider == null)
			return;

		autoReconnectDelaySlider.displayString = AutoReconnect.INSTANCE.isEnabled() ? "AutoReconnect: " + DECIMAL_FORMAT.get().format(AutoReconnect.INSTANCE.getDelay() / 1000.0) + "s" : "AutoReconnect: Off";
	}

	private void updateReconnectButton()
	{
		if (reconnectButton != null)
			reconnectButton.displayString = "Reconnect" + (AutoReconnect.INSTANCE.isEnabled() ? " (" + (AutoReconnect.INSTANCE.getDelay() / 1000 - reconnectTimer / 20) + ")" : "");
	}
}
