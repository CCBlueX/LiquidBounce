/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.TabUtils;
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount;
import net.ccbluex.liquidbounce.utils.render.ColorUtils;
import net.ccbluex.liquidbounce.utils.render.RenderUtils;
import org.lwjgl.input.Keyboard;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class GuiDirectLogin extends WrappedGuiScreen {

    private final IGuiScreen prevGui;

    private IGuiButton loginButton;
    private IGuiButton clipboardLoginButton;
    private IGuiTextField username;
    private IGuiTextField password;

    private String status = "§7Idle...";

    public GuiDirectLogin(final GuiAltManager gui) {
        this.prevGui = gui.representedScreen;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        getRepresentedScreen().getButtonList().add(loginButton = classProvider.createGuiButton(1, getRepresentedScreen().getWidth() / 2 - 100, getRepresentedScreen().getHeight() / 4 + 72, "Login"));
        getRepresentedScreen().getButtonList().add(clipboardLoginButton = classProvider.createGuiButton(2, getRepresentedScreen().getWidth() / 2 - 100, getRepresentedScreen().getHeight() / 4 + 96, "Clipboard Login"));
        getRepresentedScreen().getButtonList().add(classProvider.createGuiButton(0, getRepresentedScreen().getWidth() / 2 - 100, getRepresentedScreen().getHeight() / 4 + 120, "Back"));
        username = classProvider.createGuiTextField(2, Fonts.font40, getRepresentedScreen().getWidth() / 2 - 100, 60, 200, 20);
        username.setFocused(true);
        username.setMaxStringLength(Integer.MAX_VALUE);
        password = classProvider.createGuiPasswordField(3, Fonts.font40, getRepresentedScreen().getWidth() / 2 - 100, 85, 200, 20);
        password.setMaxStringLength(Integer.MAX_VALUE);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        getRepresentedScreen().drawBackground(0);
        RenderUtils.drawRect(30, 30, getRepresentedScreen().getWidth() - 30, getRepresentedScreen().getHeight() - 30, Integer.MIN_VALUE);

        Fonts.font40.drawCenteredString("Direct Login", getRepresentedScreen().getWidth() / 2.0f, 34, 0xffffff);
        Fonts.font35.drawCenteredString(status == null ? "" : status, getRepresentedScreen().getWidth() / 2.0f, getRepresentedScreen().getHeight() / 4.0f + 60, 0xffffff);

        username.drawTextBox();
        password.drawTextBox();

        if (username.getText().isEmpty() && !username.isFocused())
            Fonts.font40.drawCenteredString("§7Username / E-Mail", getRepresentedScreen().getWidth() / 2.0f - 55, 66, 0xffffff);

        if (password.getText().isEmpty() && !password.isFocused())
            Fonts.font40.drawCenteredString("§7Password", getRepresentedScreen().getWidth() / 2.0f - 74, 91, 0xffffff);
        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void actionPerformed(IGuiButton button) throws IOException {
        if (!button.getEnabled()) return;

        switch (button.getId()) {
            case 0:
                mc.displayGuiScreen(prevGui);
                break;
            case 1:
                if (username.getText().isEmpty()) {
                    status = "§cYou have to fill in both fields!";
                    return;
                }

                loginButton.setEnabled(false);
                clipboardLoginButton.setEnabled(false);

                new Thread(() -> {
                    status = "§aLogging in...";

                    if (password.getText().isEmpty())
                        status = GuiAltManager.login(new MinecraftAccount(ColorUtils.translateAlternateColorCodes(username.getText())));
                    else
                        status = GuiAltManager.login(new MinecraftAccount(username.getText(), password.getText()));

                    loginButton.setEnabled(true);
                    clipboardLoginButton.setEnabled(true);
                }).start();
                break;
            case 2:
                try {
                    final String clipboardData = (String) Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor);
                    final String[] args = clipboardData.split(":", 2);

                    if (!clipboardData.contains(":") || args.length != 2) {
                        status = "§cInvalid clipboard data. (Use: E-Mail:Password)";
                        return;
                    }

                    loginButton.setEnabled(false);
                    clipboardLoginButton.setEnabled(false);

                    new Thread(() -> {
                        status = "§aLogging in...";

                        status = GuiAltManager.login(new MinecraftAccount(args[0], args[1]));

                        loginButton.setEnabled(true);
                        clipboardLoginButton.setEnabled(true);
                    }).start();
                } catch (final UnsupportedFlavorException e) {
                    status = "§cClipboard flavor unsupported!";
                    ClientUtils.getLogger().error("Failed to read data from clipboard.", e);
                } catch (IOException e) {
                    status = "§cUnknown error! (See log)";
                    ClientUtils.getLogger().error(e);
                }
                break;
        }

    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        switch (keyCode) {
            case Keyboard.KEY_ESCAPE:
                mc.displayGuiScreen(prevGui);
                return;
            case Keyboard.KEY_TAB:
                TabUtils.tab(username, password);
                return;
            case Keyboard.KEY_RETURN:
                actionPerformed(loginButton);
                return;
        }

        if(username.isFocused())
            username.textboxKeyTyped(typedChar, keyCode);

        if(password.isFocused())
            password.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
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
