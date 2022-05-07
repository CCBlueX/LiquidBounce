/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.injection.forge.mixins.gui;

import com.google.gson.JsonObject;
import com.mojang.authlib.Agent;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import com.thealtening.AltService;
import com.thealtening.api.TheAltening;
import com.thealtening.api.data.AccountData;
import me.liuli.elixir.account.CrackedAccount;
import me.liuli.elixir.account.MinecraftAccount;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.SessionEvent;
import net.ccbluex.liquidbounce.features.special.AntiForge;
import net.ccbluex.liquidbounce.features.special.AutoReconnect;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.GuiLoginProgress;
import net.ccbluex.liquidbounce.ui.client.altmanager.menus.altgenerator.GuiTheAltening;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.ServerUtils;
import net.ccbluex.liquidbounce.utils.misc.RandomUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiDisconnected;
import net.minecraft.client.gui.GuiMainMenu;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.Session;
import net.minecraftforge.fml.client.config.GuiSlider;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.Proxy;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Random;

@Mixin(GuiDisconnected.class)
public abstract class MixinGuiDisconnected extends MixinGuiScreen {
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#0");

    @Shadow
    private int field_175353_i;

    private GuiButton reconnectButton;
    private GuiSlider autoReconnectDelaySlider;
    private GuiButton forgeBypassButton;
    private int reconnectTimer;

    @Inject(method = "initGui", at = @At("RETURN"))
    private void initGui(CallbackInfo callbackInfo) {
        reconnectTimer = 0;
        buttonList.add(reconnectButton = new GuiButton(1, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 22, 98, 20, "Reconnect"));

        this.drawReconnectDelaySlider();

        buttonList.add(new GuiButton(3, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 44, 98, 20, GuiTheAltening.Companion.getApiKey().isEmpty() ? "Random alt" : "New TheAltening alt"));
        buttonList.add(new GuiButton(4, this.width / 2 + 2, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 44, 98, 20, "Random username"));
        buttonList.add(forgeBypassButton = new GuiButton(5, this.width / 2 - 100, this.height / 2 + field_175353_i / 2 + this.fontRendererObj.FONT_HEIGHT + 66, "Bypass AntiForge: " + (AntiForge.enabled ? "On" : "Off")));

        updateSliderText();
    }

    @Inject(method = "actionPerformed", at = @At("HEAD"))
    private void actionPerformed(GuiButton button, CallbackInfo callbackInfo) {
        switch (button.id) {
            case 1:
                ServerUtils.connectToLastServer();
                break;
            case 3:
                if (!GuiTheAltening.Companion.getApiKey().isEmpty()) {
                    final String apiKey = GuiTheAltening.Companion.getApiKey();
                    final TheAltening theAltening = new TheAltening(apiKey);

                    try {
                        final AccountData account = theAltening.getAccountData();
                        GuiAltManager.Companion.getAltService().switchService(AltService.EnumAltService.THEALTENING);

                        final YggdrasilUserAuthentication yggdrasilUserAuthentication = new YggdrasilUserAuthentication(new YggdrasilAuthenticationService(Proxy.NO_PROXY, ""), Agent.MINECRAFT);
                        yggdrasilUserAuthentication.setUsername(account.getToken());
                        yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME);
                        yggdrasilUserAuthentication.logIn();

                        mc.session = new Session(yggdrasilUserAuthentication.getSelectedProfile().getName(), yggdrasilUserAuthentication.getSelectedProfile().getId().toString(), yggdrasilUserAuthentication.getAuthenticatedToken(), "mojang");
                        LiquidBounce.eventManager.callEvent(new SessionEvent());
                        ServerUtils.connectToLastServer();
                        break;
                    } catch (final Throwable throwable) {
                        ClientUtils.getLogger().error("Failed to login into random account from TheAltening.", throwable);
                    }
                }

                final List<MinecraftAccount> accounts = LiquidBounce.fileManager.accountsConfig.getAccounts();
                if (accounts.isEmpty())
                    break;
                final MinecraftAccount minecraftAccount = accounts.get(new Random().nextInt(accounts.size()));

                mc.displayGuiScreen(new GuiLoginProgress(minecraftAccount, () -> {
                    mc.addScheduledTask(() -> {
                        LiquidBounce.eventManager.callEvent(new SessionEvent());
                        ServerUtils.connectToLastServer();
                    });
                    return null;
                }, e -> {
                    mc.addScheduledTask(() -> {
                        final JsonObject jsonObject = new JsonObject();
                        jsonObject.addProperty("text", e.getMessage());

                        mc.displayGuiScreen(new GuiDisconnected(new GuiMultiplayer(new GuiMainMenu()), e.getMessage(), IChatComponent.Serializer.jsonToComponent(jsonObject.toString())));
                    });
                    return null;
                }, () -> null));

                break;
            case 4:
                final CrackedAccount crackedAccount = new CrackedAccount();
                crackedAccount.setName(RandomUtils.randomString(RandomUtils.nextInt(5, 16)));
                crackedAccount.update();

                mc.session = new Session(crackedAccount.getSession().getUsername(), crackedAccount.getSession().getUuid(),
                        crackedAccount.getSession().getToken(), crackedAccount.getSession().getType());
                LiquidBounce.eventManager.callEvent(new SessionEvent());
                ServerUtils.connectToLastServer();
                break;
            case 5:
                AntiForge.enabled = !AntiForge.enabled;
                forgeBypassButton.displayString = "Bypass AntiForge: " + (AntiForge.enabled ? "On" : "Off");
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
        }
    }

    @Override
    public void updateScreen() {
        if (AutoReconnect.INSTANCE.isEnabled()) {
            reconnectTimer++;
            if (reconnectTimer > AutoReconnect.INSTANCE.getDelay() / 50)
                ServerUtils.connectToLastServer();
        }
    }

    @Inject(method = "drawScreen", at = @At("RETURN"))
    private void drawScreen(CallbackInfo callbackInfo) {
        if (AutoReconnect.INSTANCE.isEnabled()) {
            this.updateReconnectButton();
        }
    }

    private void drawReconnectDelaySlider() {
        buttonList.add(autoReconnectDelaySlider =
                new GuiSlider(2, this.width / 2 + 2, this.height / 2 + field_175353_i / 2
                        + this.fontRendererObj.FONT_HEIGHT + 22, 98, 20, "AutoReconnect: ",
                        "ms", AutoReconnect.MIN, AutoReconnect.MAX, AutoReconnect.INSTANCE.getDelay(), false, true,
                        guiSlider -> {
                            AutoReconnect.INSTANCE.setDelay(guiSlider.getValueInt());

                            this.reconnectTimer = 0;
                            this.updateReconnectButton();
                            this.updateSliderText();
                        }));
    }

    private void updateSliderText() {
        if (this.autoReconnectDelaySlider == null)
            return;

        if (!AutoReconnect.INSTANCE.isEnabled()) {
            this.autoReconnectDelaySlider.displayString = "AutoReconnect: Off";
        } else {
            this.autoReconnectDelaySlider.displayString = "AutoReconnect: " + DECIMAL_FORMAT.format(AutoReconnect.INSTANCE.getDelay() / 1000.0) + "s";
        }
    }

    private void updateReconnectButton() {
        if (reconnectButton != null)
            reconnectButton.displayString = "Reconnect" + (AutoReconnect.INSTANCE.isEnabled() ? " (" + (AutoReconnect.INSTANCE.getDelay() / 1000 - reconnectTimer / 20) + ")" : "");
    }
}