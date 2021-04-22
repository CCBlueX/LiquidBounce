/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.awt.Desktop
import java.io.*
import java.net.URI
import java.net.URISyntaxException
import java.nio.charset.StandardCharsets
import javax.swing.JFileChooser
import javax.swing.JFrame
import javax.swing.JOptionPane

object MiscUtils : MinecraftInstance()
{
	@JvmStatic
	fun showErrorPopup(title: String?, message: String?)
	{
		JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE)
	}

	fun showURL(url: String)
	{
		try
		{
			Desktop.getDesktop().browse(URI(url))
		}
		catch (e: IOException)
		{
			// noinspection StringConcatenationArgumentToLogCall
			logger.error("Can't show URL \"$url\" (IOException)", e)
		}
		catch (e: URISyntaxException)
		{
			logger.error("Can't show URL \"$url\" (URI syntax exception)", e)
		}
	}

	@JvmStatic
	fun openFileChooser(): File?
	{
		if (mc.isFullScreen) mc.toggleFullscreen()

		var fileChooser: JFileChooser? = null
		var action = JFileChooser.CANCEL_OPTION

		try
		{
			fileChooser = JFileChooser()
			val frame = JFrame()
			fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
			frame.isVisible = true
			frame.toFront()
			frame.isVisible = false

			action = fileChooser.showOpenDialog(frame)
			frame.dispose()
		}
		catch (e: Throwable)
		{
			logger.warn("[openFileChooser()] Can't open file chooser", e)
		}

		return if (action == JFileChooser.APPROVE_OPTION) fileChooser?.selectedFile else null
	}

	@JvmStatic
	fun saveFileChooser(): File?
	{
		if (mc.isFullScreen) mc.toggleFullscreen()

		var fileChooser: JFileChooser? = null
		var action = JFileChooser.CANCEL_OPTION

		try
		{
			fileChooser = JFileChooser()
			val frame = JFrame()
			fileChooser.fileSelectionMode = JFileChooser.FILES_ONLY
			frame.isVisible = true
			frame.toFront()
			frame.isVisible = false

			action = fileChooser.showSaveDialog(frame)
			frame.dispose()
		}
		catch (e: Throwable)
		{
			logger.warn("[saveFileChooser()] Can't open file chooser", e)
		}

		return if (action == JFileChooser.APPROVE_OPTION) fileChooser?.selectedFile else null
	}

	/**
	 * Read specified file with UTF-8 and create a BufferedReader from it
	 *
	 * @param  file
	 * The file
	 * @return                       Created BufferedReader
	 * @author                       eric0210
	 * @throws FileNotFoundException
	 * If the file doesn't exists
	 */
	@JvmStatic
	@Throws(FileNotFoundException::class)
	fun createBufferedFileReader(file: File): BufferedReader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))

	/**
	 * Create specified file if it doesn't exists andcreate a BufferedWriter which writes bytes with UTF-8 from it
	 *
	 * @param  file
	 * The file
	 * @return                       Created BufferedWriter
	 * @throws FileNotFoundException
	 * if the file exists but is a directory rather than a regular file, does not exist but cannot be created, or cannot be opened for any other reason
	 */
	@JvmStatic
	@Throws(FileNotFoundException::class)
	fun createBufferedFileWriter(file: File): BufferedWriter = BufferedWriter(OutputStreamWriter(FileOutputStream(file), StandardCharsets.UTF_8))
}
