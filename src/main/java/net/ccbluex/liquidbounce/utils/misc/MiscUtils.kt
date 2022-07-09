/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.utils.misc

import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.MinecraftInstance
import java.awt.Desktop
import java.io.File
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
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
}
