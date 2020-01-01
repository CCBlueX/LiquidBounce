package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import com.mojang.authlib.Agent;
import com.mojang.authlib.exceptions.AuthenticationException;
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService;
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication;
import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.TabUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.net.Proxy;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class GuiAdd extends GuiScreen {

    private final GuiAltManager prevGui;

    private GuiButton addButton;
    private GuiButton clipboardButton;
    private GuiTextField username;
    private GuiPasswordField password;

    private String status = "§7Idle...";

    public GuiAdd(final GuiAltManager gui) {
        this.prevGui = gui;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.add(addButton = new GuiButton(1, width / 2 - 100, height / 4 + 72, "Add"));
        buttonList.add(clipboardButton = new GuiButton(2, width / 2 - 100, height / 4 + 96, "Clipboard"));
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"));
        username = new GuiTextField(2, Fonts.font40, width / 2 - 100, 60, 200, 20);
        username.setFocused(true);
        username.setMaxStringLength(Integer.MAX_VALUE);
        password = new GuiPasswordField(3, Fonts.font40, width / 2 - 100, 85, 200, 20);
        password.setMaxStringLength(Integer.MAX_VALUE);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(0);
        Gui.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE);

        drawCenteredString(Fonts.font40, "Add Account", width / 2, 34, 0xffffff);
        drawCenteredString(Fonts.font35, status == null ? "" : status, width / 2, height / 4 + 60, 0xffffff);

        username.drawTextBox();
        password.drawTextBox();

        if(username.getText().isEmpty() && !username.isFocused())
            drawCenteredString(Fonts.font40, "§7Username / E-Mail", width / 2 - 55, 66, 0xffffff);

        if(password.getText().isEmpty() && !password.isFocused())
            drawCenteredString(Fonts.font40, "§7Password", width / 2 - 74, 91, 0xffffff);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        if(!button.enabled) return;

        switch(button.id) {
            case 0:
                mc.displayGuiScreen(prevGui);
                break;
            case 1:
                if(LiquidBounce.CLIENT.fileManager.accountsConfig.altManagerMinecraftAccounts.stream().anyMatch(account -> account.getName().equals(username.getText()))) {
                    status = "§cThe account has already been added.";
                    break;
                }

                MinecraftAccount minecraftAccount;

                if(password.getText().isEmpty())
                    minecraftAccount = new MinecraftAccount(username.getText());
                else
                    minecraftAccount = new MinecraftAccount(username.getText(), password.getText());

                addButton.enabled = clipboardButton.enabled = false;

                new Thread(() -> {
                    status = "§aChecking...";

                    boolean isWorking;

                    if(!minecraftAccount.isCracked()) {
                        final YggdrasilUserAuthentication userAuthentication = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);

                        userAuthentication.setUsername(minecraftAccount.getName());
                        userAuthentication.setPassword(minecraftAccount.getPassword());

                        try{
                            userAuthentication.logIn();
                            minecraftAccount.setAccountName(userAuthentication.getSelectedProfile().getName());
                            isWorking = true;
                        }catch(final NullPointerException | AuthenticationException e) {
                            isWorking = false;
                        }
                    }else isWorking = true;

                    if(isWorking) {
                        LiquidBounce.CLIENT.fileManager.accountsConfig.altManagerMinecraftAccounts.add(minecraftAccount);
                        LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.accountsConfig);
                        status = "§aThe account has been added.";
                        prevGui.status = status;
                        mc.displayGuiScreen(prevGui);
                    }else status = "§cThe account doesn't work.";

                    addButton.enabled = clipboardButton.enabled = true;
                }).start();
                break;
            case 2:
                try{
                    final String clipboardData = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    final String[] args = clipboardData.split(":", 2);

                    if(!clipboardData.contains(":") || args.length != 2) {
                        status = "§cInvalid clipboard data. (Use: E-Mail:Password)";
                        return;
                    }

                    if(LiquidBounce.CLIENT.fileManager.accountsConfig.altManagerMinecraftAccounts.stream().anyMatch(account -> account.getName().equals(args[0]))) {
                        status = "§cThe account has already been added.";
                        break;
                    }

                    addButton.enabled = clipboardButton.enabled = false;

                    new Thread(() -> {
                        status = "§aChecking...";

                        final MinecraftAccount account = new MinecraftAccount(args[0], args[1]);
                        final YggdrasilUserAuthentication userAuthentication = (YggdrasilUserAuthentication) new YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT);

                        userAuthentication.setUsername(account.getName());
                        userAuthentication.setPassword(account.getPassword());

                        try {
                            userAuthentication.logIn();
                            account.setAccountName(userAuthentication.getSelectedProfile().getName());

                            LiquidBounce.CLIENT.fileManager.accountsConfig.altManagerMinecraftAccounts.add(account);
                            LiquidBounce.CLIENT.fileManager.saveConfig(LiquidBounce.CLIENT.fileManager.accountsConfig);
                            status = "§aThe account has been added.";
                            prevGui.status = status;
                            mc.displayGuiScreen(prevGui);
                        } catch (NullPointerException | AuthenticationException exception) {
                            status = "§cThe account doesn't work.";
                        }

                        addButton.enabled = clipboardButton.enabled = true;
                    }).start();
                }catch(final UnsupportedFlavorException e) {
                    status = "§cClipboard flavor unsupported!";
                    ClientUtils.getLogger().error("Failed to read data from clipboard.", e);
                }
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                mc.displayGuiScreen(prevGui);
                return;
            case Keyboard.KEY_TAB:
                TabUtils.tab(username, password);
                return;
            case Keyboard.KEY_RETURN:
                actionPerformed(addButton);
                return;
        }

        if(username.isFocused())
            username.textboxKeyTyped(typedChar, keyCode);

        if(password.isFocused())
            password.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        username.mouseClicked(mouseX, mouseY, mouseButton);
        password.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        username.updateCursorCounter();
        password.updateCursorCounter();

        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }
}
