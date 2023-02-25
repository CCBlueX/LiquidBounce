/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client;

import net.ccbluex.liquidbounce.LiquidBounce;
import net.ccbluex.liquidbounce.features.special.ClientFixes;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import org.lwjgl.input.Keyboard;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class GuiClientFixes extends GuiScreen {

    private final GuiScreen prevGui;

    private GuiButton enabledButton;
    private GuiButton fmlButton;
    private GuiButton proxyButton;
    private GuiButton payloadButton;
    private GuiButton customBrandButton;
    private GuiButton resourcePackButton;



    public GuiClientFixes(final GuiScreen prevGui) {
        this.prevGui = prevGui;
    }

    @Override
    public void initGui() {
        buttonList.add(enabledButton = new GuiButton(1, width / 2 - 100, height / 4 + 35, "AntiForge (" + (ClientFixes.fmlFixesEnabled ? "On" : "Off") + ")"));
        buttonList.add(fmlButton = new GuiButton(2, width / 2 - 100, height / 4 + 35 + 25, "Block FML (" + (ClientFixes.blockFML ? "On" : "Off") + ")"));
        buttonList.add(proxyButton = new GuiButton(3, width / 2 - 100, height / 4 + 35 + 25 * 2, "Block FML Proxy Packet (" + (ClientFixes.blockProxyPacket ? "On" : "Off") + ")"));
        buttonList.add(payloadButton = new GuiButton(4, width / 2 - 100, height / 4 + 35 + 25 * 3, "Block Non-MC Payloads (" + (ClientFixes.blockPayloadPackets ? "On" : "Off") + ")"));
        buttonList.add(customBrandButton = new GuiButton(5, width / 2 - 100, height / 4 + 35 + 25 * 4, "Brand (" + ClientFixes.clientBrand + ")"));

        buttonList.add(resourcePackButton = new GuiButton(6, width / 2 - 100, height / 4 + 50 + 25 * 5, "Block Resource Pack Exploit (" + (ClientFixes.blockResourcePackExploit ? "On" : "Off") + ")"));

        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 55 + 25 * 6 + 5, "Back"));
    }

    @Override
    public void actionPerformed(GuiButton button) {
        switch (button.id) {
            case 1:
                ClientFixes.fmlFixesEnabled = !ClientFixes.fmlFixesEnabled;
                enabledButton.displayString = "AntiForge (" + (ClientFixes.fmlFixesEnabled ? "On" : "Off") + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 2:
                ClientFixes.blockFML = !ClientFixes.blockFML;
                fmlButton.displayString = "Block FML (" + (ClientFixes.blockFML ? "On" : "Off") + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 3:
                ClientFixes.blockProxyPacket = !ClientFixes.blockProxyPacket;
                proxyButton.displayString = "Block FML Proxy Packet (" + (ClientFixes.blockProxyPacket ? "On" : "Off") + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 4:
                ClientFixes.blockPayloadPackets = !ClientFixes.blockPayloadPackets;
                payloadButton.displayString = "Block FML Payload Packets (" + (ClientFixes.blockPayloadPackets ? "On" : "Off") + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 5:
                final List<String> brands = Arrays.asList(ClientFixes.possibleBrands);
                final int index = brands.indexOf(ClientFixes.clientBrand);

                if(index == brands.size() - 1) {
                    ClientFixes.clientBrand = brands.get(0);
                } else {
                    ClientFixes.clientBrand = brands.get(index + 1);
                }

                customBrandButton.displayString = "Brand (" + ClientFixes.clientBrand + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 6:
                ClientFixes.blockResourcePackExploit = !ClientFixes.blockResourcePackExploit;
                resourcePackButton.displayString = "Block Resource Pack Exploit (" + (ClientFixes.blockResourcePackExploit ? "On" : "Off") + ")";
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.valuesConfig);
                break;
            case 0:
                mc.displayGuiScreen(prevGui);
                break;
        }
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(0);
        Fonts.fontBold180.drawCenteredString("Fixes", (int) (width / 2F), (int) (height / 8F + 5F), 4673984, true);

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if(Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui);
            return;
        }

        super.keyTyped(typedChar, keyCode);
    }
}