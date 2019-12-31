package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator;

import com.thealtening.AltService;
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.ClientUtils;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import net.mcleaks.MCLeaks;
import net.mcleaks.RedeemResponse;
import net.mcleaks.Session;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import java.io.IOException;

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @game Minecraft
 * @author CCBlueX
 */
public class GuiMCLeaks extends GuiScreen {

    private final GuiAltManager prevGui;

    private String status;
    private GuiTextField tokenField;

    public GuiMCLeaks(final GuiAltManager prevGui) {
        this.prevGui = prevGui;
    }

    @Override
    public void updateScreen() {
        tokenField.updateCursorCounter();
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        buttonList.add(new GuiButton(1, width / 2 - 150, height / 4 + 96, 300, 20, "Redeem Token"));
        buttonList.add(new GuiButton(2, width / 2 - 150, height / 4 + 120, 158, 20, "Get Token"));
        buttonList.add(new GuiButton(3, width / 2 + 12, height / 4 + 120, 138, 20, "Back"));

        tokenField = new GuiTextField(0, Fonts.font40, width / 2 - 100, 110, 200, 20);
        tokenField.setFocused(true);
        tokenField.setMaxStringLength(Integer.MAX_VALUE);
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
    }

    @Override
    protected void actionPerformed(final GuiButton button) {
        if(!button.enabled) return;

        switch(button.id) {
            case 1:
                if(tokenField.getText().length() != 16) {
                    status = "§cThe token has to be 16 characters long!";
                    return;
                }

                button.enabled = false;
                button.displayString = "Please wait ...";

                MCLeaks.redeem(tokenField.getText(), o -> {
                    if(o instanceof String) {
                        status = "§c" + o;
                        return;
                    }

                    final RedeemResponse redeemResponse = (RedeemResponse) o;
                    MCLeaks.refresh(new Session(redeemResponse.getUsername(), redeemResponse.getToken()));
                    try{
                        GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG);
                    }catch(NoSuchFieldException | IllegalAccessException e) {
                        ClientUtils.getLogger().error("Failed to change alt service to Mojang.", e);
                    }
                    status = "§aYour token was redeemed successfully!";
                    button.enabled = true;
                    button.displayString = "Redeem Token";

                    prevGui.status = status;
                    mc.displayGuiScreen(prevGui);
                });
                break;
            case 2:
                MiscUtils.showURL("https://mcleaks.net/");
                break;
            case 3:
                mc.displayGuiScreen(prevGui);
                break;
        }
    }

    @Override
    protected void keyTyped(final char typedChar, final int keyCode) {
        tokenField.textboxKeyTyped(typedChar, keyCode);

        switch(keyCode) {
            case Keyboard.KEY_ESCAPE:
                mc.displayGuiScreen(prevGui);
                break;
            case Keyboard.KEY_TAB:
                tokenField.setFocused(!tokenField.isFocused());
                break;
            case Keyboard.KEY_RETURN:
            case Keyboard.KEY_NUMPADENTER:
                actionPerformed(buttonList.get(1));
                break;
        }
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        tokenField.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        // Draw background
        drawBackground(0);
        Gui.drawRect(30, 30, width - 30, height - 30, Integer.MIN_VALUE);

        // Draw text
        drawCenteredString(Fonts.font40, "MCLeaks", width / 2, 34, 0xffffff);
        drawCenteredString(Fonts.font40, MCLeaks.isAltActive() ? "§aToken active. Using §9" + MCLeaks.getSession().getUsername() + "§a to login!" : "§7No Token redeemed.", width / 2, height / 4 + (status != null ? 74 : 84), 16777215);
        drawString(Fonts.font40, "Token", width / 2 - 100, 98, 10526880);

        if(status != null) drawCenteredString(Fonts.font40, status, width / 2, height / 4 + 84, 16777215);
        tokenField.drawTextBox();
        super.drawScreen(mouseX, mouseY, partialTicks);
    }
}