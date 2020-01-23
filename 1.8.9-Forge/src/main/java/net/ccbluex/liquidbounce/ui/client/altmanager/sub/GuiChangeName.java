/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.event.SessionEvent;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import net.minecraft.util.Session;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

public class GuiChangeName extends GuiScreen {

    private final GuiAltManager prevGui;
    private GuiTextField name;
    private String status;

    public GuiChangeName(final GuiAltManager gui) {
        this.prevGui = gui;
    }

    public void initGui() {
        Keyboard.enableRepeatEvents(true);
        buttonList.add(new GuiButton(1, width / 2 - 100, height / 4 + 96, "Change"));
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"));

        name = new GuiTextField(2, Fonts.font40, width / 2 - 100, 60, 200, 20);
        name.setFocused(true);
        name.setText(mc.getSession().getUsername());
        name.setMaxStringLength(16);
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(0);
        Gui.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE);

        drawCenteredString(Fonts.font40, "Change Name", width / 2, 34, 0xffffff);
        drawCenteredString(Fonts.font40, status == null ? "" : status, width / 2, height / 4 + 84, 0xffffff);
        name.drawTextBox();

        if(name.getText().isEmpty() && !name.isFocused())
            drawCenteredString(Fonts.font40, "§7Username", width / 2 - 74, 66, 0xffffff);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    protected void actionPerformed(GuiButton button) throws IOException {
        switch(button.id) {
            case 0:
                mc.displayGuiScreen(prevGui);
                break;
            case 1:
                if(name.getText().isEmpty()) {
                    status = "§cEnter a name!";
                    return;
                }

                if(!name.getText().equalsIgnoreCase(mc.getSession().getUsername())) {
                    status = "§cJust change the upper and lower case!";
                    return;
                }

                mc.session = new Session(name.getText(), mc.getSession().getPlayerID(), mc.getSession().getToken(), mc.getSession().getSessionType().name());
                LiquidBounce.eventManager.callEvent(new SessionEvent());
                status = "§aChanged name to §7" + name.getText() + "§c.";
                prevGui.status = status;
                mc.displayGuiScreen(prevGui);
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    protected void keyTyped(char typedChar, int keyCode) throws IOException {
        if(Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui);
            return;
        }

        if(name.isFocused())
            name.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        name.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        name.updateCursorCounter();
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        super.onGuiClosed();
    }
}
