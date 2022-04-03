/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools;

import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.TabUtils;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;
import net.minecraft.client.gui.GuiButton;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.GuiTextField;
import org.lwjgl.input.Keyboard;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class GuiPortScanner extends GuiScreen {

    private final GuiScreen prevGui;
    private final List<Integer> ports = new ArrayList<>();
    private GuiTextField hostField;
    private GuiTextField minPortField;
    private GuiTextField maxPortField;
    private GuiTextField threadsField;
    private GuiButton buttonToggle;
    private boolean running;
    private String status = "§7Waiting...";
    private String host;
    private int currentPort;
    private int maxPort;
    private int minPort;
    private int checkedPort;

    public GuiPortScanner(final GuiScreen prevGui) {
        this.prevGui = prevGui;
    }

    @Override
    public void initGui() {
        Keyboard.enableRepeatEvents(true);

        hostField = new GuiTextField(0, Fonts.font40, width / 2 - 100, 60, 200, 20);
        hostField.setFocused(true);
        hostField.setMaxStringLength(Integer.MAX_VALUE);
        hostField.setText("localhost");

        minPortField = new GuiTextField(1, Fonts.font40, width / 2 - 100, 90, 90, 20);
        minPortField.setMaxStringLength(5);
        minPortField.setText(String.valueOf(1));

        maxPortField = new GuiTextField(2, Fonts.font40, width / 2 + 10, 90, 90, 20);
        maxPortField.setMaxStringLength(5);
        maxPortField.setText(String.valueOf(65535));

        threadsField = new GuiTextField(3, Fonts.font40, width / 2 - 100, 120, 200, 20);
        threadsField.setMaxStringLength(Integer.MAX_VALUE);
        threadsField.setText(String.valueOf(500));

        buttonList.add(buttonToggle = new GuiButton(1, width / 2 - 100, height / 4 + 95, running ? "Stop" : "Start"));
        buttonList.add(new GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"));
        buttonList.add(new GuiButton(2, width / 2 - 100, height / 4 + 155, "Export"));

        super.initGui();
    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        drawBackground(0);

        Fonts.font40.drawCenteredString("Port Scanner", width / 2.0f, 34, 0xffffff);
        Fonts.font35.drawCenteredString(running ? "§7" + checkedPort + " §8/ §7" + maxPort : status == null ? "" : status, width / 2.0f, height / 4.0f + 80, 0xffffff);

        buttonToggle.displayString = running ? "Stop" : "Start";

        hostField.drawTextBox();
        minPortField.drawTextBox();
        maxPortField.drawTextBox();
        threadsField.drawTextBox();

        Fonts.font40.drawString("§c§lPorts:", 2, 2, Color.WHITE.hashCode());

        synchronized (ports) {
            int i = 12;

            for (final Integer integer : ports) {
                Fonts.font35.drawString(String.valueOf(integer), 2, i, Color.WHITE.hashCode());
                i += Fonts.font35.getFontHeight();
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks);
    }

    @Override
    public void actionPerformed(GuiButton button) throws IOException {
        switch (button.id) {
            case 0:
                mc.displayGuiScreen(prevGui);
                break;
            case 1:
                if (running) {
                    running = false;
                } else {
                    host = hostField.getText();

                    if (host.isEmpty()) {
                        status = "§cInvalid host";
                        return;
                    }

                    try {
                        minPort = Integer.parseInt(minPortField.getText());
                    } catch (final NumberFormatException e) {
                        status = "§cInvalid min port";
                        return;
                    }

                    try {
                        maxPort = Integer.parseInt(maxPortField.getText());
                    } catch (final NumberFormatException e) {
                        status = "§cInvalid max port";
                        return;
                    }

                    int threads;
                    try {
                        threads = Integer.parseInt(threadsField.getText());
                    } catch (final NumberFormatException e) {
                        status = "§cInvalid threads";
                        return;
                    }

                    ports.clear();

                    currentPort = minPort - 1;
                    checkedPort = minPort;

                    for (int i = 0; i < threads; i++) {
                        new Thread(() -> {
                            try {
                                while (running && currentPort < maxPort) {
                                    currentPort++;

                                    final int port = currentPort;

                                    try {
                                        final Socket socket = new Socket();
                                        socket.connect(new InetSocketAddress(host, port), 500);
                                        socket.close();

                                        synchronized (ports) {
                                            if (!ports.contains(port))
                                                ports.add(port);
                                        }
                                    } catch (final Exception ignored) {
                                    }

                                    if (checkedPort < port)
                                        checkedPort = port;
                                }

                                running = false;
                                buttonToggle.displayString = "Start";
                            } catch (final Exception e) {
                                status = "§a§l" + e.getClass().getSimpleName() + ": §c" + e.getMessage();
                            }
                        }).start();
                    }

                    running = true;
                }

                buttonToggle.displayString = running ? "Stop" : "Start";
                break;
            case 2:
                final File selectedFile = MiscUtils.saveFileChooser();

                if (selectedFile == null || selectedFile.isDirectory())
                    return;

                try {
                    if (!selectedFile.exists())
                        selectedFile.createNewFile();

                    final FileWriter fileWriter = new FileWriter(selectedFile);

                    fileWriter.write("Portscan\r\n");
                    fileWriter.write("Host: " + host + "\r\n\r\n");

                    fileWriter.write("Ports (" + minPort + " - " + maxPort + "):\r\n");

                    for (final Integer integer : ports)
                        fileWriter.write(integer + "\r\n");
                    fileWriter.flush();
                    fileWriter.close();
                    JOptionPane.showMessageDialog(null, "Exported successfully!", "Port Scanner", JOptionPane.INFORMATION_MESSAGE);
                } catch (final Exception e) {
                    e.printStackTrace();
                    MiscUtils.showErrorPopup("Error", "Exception class: " + e.getClass().getName() + "\nMessage: " + e.getMessage());
                }
                break;
        }
        super.actionPerformed(button);
    }

    @Override
    public void keyTyped(char typedChar, int keyCode) throws IOException {
        if (Keyboard.KEY_ESCAPE == keyCode) {
            mc.displayGuiScreen(prevGui);
            return;
        }

        if (Keyboard.KEY_TAB == keyCode)
            TabUtils.tab(hostField, minPortField, maxPortField);

        if (running)
            return;

        if (hostField.isFocused())
            hostField.textboxKeyTyped(typedChar, keyCode);

        if (minPortField.isFocused() && !Character.isLetter(typedChar))
            minPortField.textboxKeyTyped(typedChar, keyCode);

        if (maxPortField.isFocused() && !Character.isLetter(typedChar))
            maxPortField.textboxKeyTyped(typedChar, keyCode);

        if (threadsField.isFocused() && !Character.isLetter(typedChar))
            threadsField.textboxKeyTyped(typedChar, keyCode);
        super.keyTyped(typedChar, keyCode);
    }

    @Override
    public void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        hostField.mouseClicked(mouseX, mouseY, mouseButton);
        minPortField.mouseClicked(mouseX, mouseY, mouseButton);
        maxPortField.mouseClicked(mouseX, mouseY, mouseButton);
        threadsField.mouseClicked(mouseX, mouseY, mouseButton);
        super.mouseClicked(mouseX, mouseY, mouseButton);
    }

    @Override
    public void updateScreen() {
        hostField.updateCursorCounter();
        minPortField.updateCursorCounter();
        maxPortField.updateCursorCounter();
        threadsField.updateCursorCounter();
        super.updateScreen();
    }

    @Override
    public void onGuiClosed() {
        Keyboard.enableRepeatEvents(false);
        running = false;
        super.onGuiClosed();
    }
}