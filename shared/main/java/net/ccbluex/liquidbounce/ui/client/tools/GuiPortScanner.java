/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools;

import java.awt.*;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.*;

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen;
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField;
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen;
import net.ccbluex.liquidbounce.ui.font.Fonts;
import net.ccbluex.liquidbounce.utils.TabUtils;
import net.ccbluex.liquidbounce.utils.misc.MiscUtils;

import org.lwjgl.input.Keyboard;

// TODO: Create own thread pool
public class GuiPortScanner extends WrappedGuiScreen
{

	private final IGuiScreen prevGui;
	private final Collection<Integer> ports = new ArrayList<>();
	private IGuiTextField hostField;
	private IGuiTextField minPortField;
	private IGuiTextField maxPortField;
	private IGuiTextField threadsField;
	private IGuiButton buttonToggle;
	private boolean running;
	private String status = "\u00A77Waiting...";
	private String host;
	private int currentPort;
	private int maxPort;
	private int minPort;
	private int checkedPort;

	public GuiPortScanner(final IGuiScreen prevGui)
	{
		this.prevGui = prevGui;
	}

	@Override
	public void initGui()
	{
		Keyboard.enableRepeatEvents(true);

		hostField = classProvider.createGuiTextField(0, Fonts.font40, representedScreen.getWidth() / 2 - 100, 60, 200, 20);
		hostField.setFocused(true);
		hostField.setMaxStringLength(Integer.MAX_VALUE);
		hostField.setText("localhost");

		minPortField = classProvider.createGuiTextField(1, Fonts.font40, representedScreen.getWidth() / 2 - 100, 90, 90, 20);
		minPortField.setMaxStringLength(5);
		minPortField.setText(String.valueOf(1));

		maxPortField = classProvider.createGuiTextField(2, Fonts.font40, representedScreen.getWidth() / 2 + 10, 90, 90, 20);
		maxPortField.setMaxStringLength(5);
		maxPortField.setText(String.valueOf(65535));

		threadsField = classProvider.createGuiTextField(3, Fonts.font40, representedScreen.getWidth() / 2 - 100, 120, 200, 20);
		threadsField.setMaxStringLength(Integer.MAX_VALUE);
		threadsField.setText(String.valueOf(500));

		representedScreen.getButtonList().add(buttonToggle = classProvider.createGuiButton(1, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 95, running ? "Stop" : "Start"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(0, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 120, "Back"));
		representedScreen.getButtonList().add(classProvider.createGuiButton(2, representedScreen.getWidth() / 2 - 100, representedScreen.getHeight() / 4 + 155, "Export"));

		super.initGui();
	}

	@Override
	public void drawScreen(final int mouseX, final int mouseY, final float partialTicks)
	{
		representedScreen.drawBackground(0);

		Fonts.font40.drawCenteredString("Port Scanner", representedScreen.getWidth() / 2.0f, 34, 0xffffff);
		Fonts.font35.drawCenteredString(running ? "\u00A77" + checkedPort + " \u00A78/ \u00A77" + maxPort : Optional.ofNullable(status).orElse(""), representedScreen.getWidth() / 2.0f, representedScreen.getHeight() / 4.0f + 80, 0xffffff);

		buttonToggle.setDisplayString(running ? "Stop" : "Start");

		hostField.drawTextBox();
		minPortField.drawTextBox();
		maxPortField.drawTextBox();
		threadsField.drawTextBox();

		Fonts.font40.drawString("\u00A7c\u00A7lPorts:", 2, 2, Color.WHITE.hashCode());

		synchronized (ports)
		{
			int i = 12;

			for (final Integer integer : ports)
			{
				Fonts.font35.drawString(String.valueOf(integer), 2, i, Color.WHITE.hashCode());
				i += Fonts.font35.getFontHeight();
			}
		}

		super.drawScreen(mouseX, mouseY, partialTicks);
	}

	@Override
	public void actionPerformed(final IGuiButton button) throws IOException
	{
		switch (button.getId())
		{
			case 0:
				mc.displayGuiScreen(prevGui);
				break;
			case 1:
				if (running)
					running = false;
				else
				{
					host = hostField.getText();

					if (host.isEmpty())
					{
						status = "\u00A7cInvalid host";
						return;
					}

					try
					{
						minPort = Integer.parseInt(minPortField.getText());
					}
					catch (final NumberFormatException e)
					{
						status = "\u00A7cInvalid min port";
						return;
					}

					try
					{
						maxPort = Integer.parseInt(maxPortField.getText());
					}
					catch (final NumberFormatException e)
					{
						status = "\u00A7cInvalid max port";
						return;
					}

					final int threads;
					try
					{
						threads = Integer.parseInt(threadsField.getText());
					}
					catch (final NumberFormatException e)
					{
						status = "\u00A7cInvalid threads";
						return;
					}

					ports.clear();

					currentPort = minPort - 1;
					checkedPort = minPort;

					final ExecutorService threadPool = Executors.newWorkStealingPool(threads);

					final Runnable task = () ->
					{
						try
						{
							while (running && currentPort < maxPort)
							{
								currentPort++;

								final int port = currentPort;

								try
								{
									final Socket socket = new Socket();
									socket.connect(new InetSocketAddress(host, port), 500);
									socket.close();

									synchronized (ports)
									{
										if (!ports.contains(port))
											ports.add(port);
									}
								}
								catch (final Exception ignored)
								{
								}

								if (checkedPort < port)
									checkedPort = port;
							}

							running = false;
							buttonToggle.setDisplayString("Start");
						}
						catch (final Exception e)
						{
							status = "\u00A7a\u00A7l" + e.getClass().getSimpleName() + ": \u00A7c" + e.getMessage();
						}
					};

					for (int i = 0; i < threads; i++)
						threadPool.submit(task);

					running = true;
				}

				buttonToggle.setDisplayString(running ? "Stop" : "Start");
				break;
			case 2:
				final File selectedFile = MiscUtils.saveFileChooser();

				if (selectedFile == null || selectedFile.isDirectory())
					return;

				try
				{
					if (!selectedFile.exists())
						selectedFile.createNewFile();

					final BufferedWriter fileWriter = MiscUtils.createBufferedFileWriter(selectedFile);

					fileWriter.write("Portscan" + System.lineSeparator());
					fileWriter.write("Host: " + host + System.lineSeparator() + System.lineSeparator());

					fileWriter.write("Ports (" + minPort + " - " + maxPort + "): " + System.lineSeparator());

					for (final Integer integer : ports)
						fileWriter.write(integer + System.lineSeparator());

					fileWriter.flush();
					fileWriter.close();
					JOptionPane.showMessageDialog(null, "Exported successfully!", "Port Scanner", JOptionPane.INFORMATION_MESSAGE);
				}
				catch (final Exception e)
				{
					e.printStackTrace();
					MiscUtils.showErrorPopup("Error", "Exception class: " + e.getClass().getName() + "\nMessage: " + e.getMessage());
				}
				break;
		}
		super.actionPerformed(button);
	}

	@Override
	public void keyTyped(final char typedChar, final int keyCode) throws IOException
	{
		if (Keyboard.KEY_ESCAPE == keyCode)
		{
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
	public void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException
	{
		hostField.mouseClicked(mouseX, mouseY, mouseButton);
		minPortField.mouseClicked(mouseX, mouseY, mouseButton);
		maxPortField.mouseClicked(mouseX, mouseY, mouseButton);
		threadsField.mouseClicked(mouseX, mouseY, mouseButton);
		super.mouseClicked(mouseX, mouseY, mouseButton);
	}

	@Override
	public void updateScreen()
	{
		hostField.updateCursorCounter();
		minPortField.updateCursorCounter();
		maxPortField.updateCursorCounter();
		threadsField.updateCursorCounter();
		super.updateScreen();
	}

	@Override
	public void onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false);
		running = false;
		super.onGuiClosed();
	}
}
