/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc;

import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;

import javax.swing.*;

import net.ccbluex.liquidbounce.utils.MinecraftInstance;

public final class MiscUtils extends MinecraftInstance
{

	public static void showErrorPopup(final String title, final String message)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
	}

	public static void showURL(final String url)
	{
		try
		{
			Desktop.getDesktop().browse(new URI(url));
		}
		catch (final IOException | URISyntaxException e)
		{
			e.printStackTrace();
		}
	}

	public static File openFileChooser()
	{
		if (mc.isFullScreen())
			mc.toggleFullscreen();

		final JFileChooser fileChooser = new JFileChooser();
		final JFrame frame = new JFrame();

		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		frame.setVisible(true);
		frame.toFront();
		frame.setVisible(false);

		final int action = fileChooser.showOpenDialog(frame);
		frame.dispose();

		return action == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
	}

	public static File saveFileChooser()
	{
		if (mc.isFullScreen())
			mc.toggleFullscreen();

		final JFileChooser fileChooser = new JFileChooser();
		final JFrame frame = new JFrame();

		fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

		frame.setVisible(true);
		frame.toFront();
		frame.setVisible(false);

		final int action = fileChooser.showSaveDialog(frame);
		frame.dispose();

		return action == JFileChooser.APPROVE_OPTION ? fileChooser.getSelectedFile() : null;
	}

	/**
	 * Read specified file with UTF-8 and create a BufferedReader from it
	 * 
	 * @param  file
	 *                               The file
	 * @return                       Created BufferedReader
	 * @author                       eric0210
	 * @throws FileNotFoundException
	 *                               If the file doesn't exists
	 */
	public static BufferedReader createBufferedFileReader(final File file) throws FileNotFoundException
	{
		return new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8));
	}

	/**
	 * Create specified file if it doesn't exists andcreate a BufferedWriter which writes bytes with UTF-8 from it
	 * @param  file The file
	 * @return Created BufferedWriter
	 * @throws FileNotFoundException
	 *                               if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason
	 */
	public static BufferedWriter createBufferedFileWriter(final File file) throws FileNotFoundException
	{
		return new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8));
	}
}
