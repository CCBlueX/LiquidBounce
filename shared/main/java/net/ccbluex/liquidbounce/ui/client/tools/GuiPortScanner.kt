/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools

import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.TabUtils.tab
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.*
import java.util.concurrent.Executors
import javax.swing.JOptionPane

class GuiPortScanner(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{
	private val ports: MutableCollection<Int> = ArrayList(65536)

	private lateinit var hostField: IGuiTextField
	private lateinit var minPortField: IGuiTextField
	private lateinit var maxPortField: IGuiTextField
	private lateinit var threadsField: IGuiTextField
	private lateinit var buttonToggle: IGuiButton

	private var running = false
	private var status = "\u00A77Waiting..."
	private var host: String? = null
	private var currentPort = 0
	private var maxPort = 0
	private var minPort = 0
	private var checkedPort = 0

	override fun initGui()
	{
		Keyboard.enableRepeatEvents(true)

		val middleScreen = representedScreen.width shr 1

		val buttonX = middleScreen - 100

		hostField = classProvider.createGuiTextField(0, Fonts.font40, buttonX, 60, 200, 20).apply { isFocused = true }.apply { maxStringLength = Int.MAX_VALUE }.apply { text = "localhost" }

		minPortField = classProvider.createGuiTextField(1, Fonts.font40, buttonX, 90, 90, 20).apply { maxStringLength = 5 }.apply { text = "1" }

		maxPortField = classProvider.createGuiTextField(2, Fonts.font40, middleScreen + 10, 90, 90, 20).apply { maxStringLength = 5 }.apply { text = "65535" }

		threadsField = classProvider.createGuiTextField(3, Fonts.font40, buttonX, 120, 200, 20).apply { maxStringLength = Int.MAX_VALUE }.apply { text = "500" }

		val quarterScreen = representedScreen.height shr 2

		val buttonList = representedScreen.buttonList
		buttonList.add(classProvider.createGuiButton(1, buttonX, quarterScreen + 95, if (running) "Stop" else "Start").also { buttonToggle = it })
		buttonList.add(classProvider.createGuiButton(0, buttonX, quarterScreen + 120, "Back"))
		buttonList.add(classProvider.createGuiButton(2, buttonX, quarterScreen + 155, "Export"))

		super.initGui()
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		representedScreen.drawBackground(0)

		Fonts.font40.drawCenteredString("Port Scanner", (representedScreen.width shr 1).toFloat(), 34f, 0xffffff)
		Fonts.font35.drawCenteredString(if (running) "\u00A77$checkedPort \u00A78/ \u00A77$maxPort" else status, (representedScreen.width shr 1).toFloat(), (representedScreen.height shr 2) + 80f, 0xffffff)

		buttonToggle.displayString = if (running) "Stop" else "Start"
		hostField.drawTextBox()
		minPortField.drawTextBox()
		maxPortField.drawTextBox()
		threadsField.drawTextBox()

		Fonts.font40.drawString("\u00A7c\u00A7lPorts:", 2, 2, Color.WHITE.hashCode())

		synchronized(ports) {
			var i = 12

			for (integer in ports)
			{
				Fonts.font35.drawString("$integer", 2, i, Color.WHITE.hashCode())
				i += Fonts.font35.fontHeight
			}
		}

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui)

			1 ->
			{
				toggle()
				buttonToggle.displayString = if (running) "Stop" else "Start"
			}

			2 ->
			{
				val selectedFile = MiscUtils.saveFileChooser()
				if (selectedFile == null || selectedFile.isDirectory) return
				try
				{
					if (!selectedFile.exists()) selectedFile.createNewFile()
					val fileWriter = MiscUtils.createBufferedFileWriter(selectedFile)
					fileWriter.write("Portscan" + System.lineSeparator())
					fileWriter.write("Host: " + host + System.lineSeparator() + System.lineSeparator())
					fileWriter.write("Ports (" + minPort + " - " + maxPort + "): " + System.lineSeparator())
					for (integer in ports)
					{
						fileWriter.write("$integer")
						fileWriter.write(System.lineSeparator())
					}
					fileWriter.flush()
					fileWriter.close()
					JOptionPane.showMessageDialog(null, "Exported successfully!", "Port Scanner", JOptionPane.INFORMATION_MESSAGE)
				}
				catch (e: Exception)
				{
					logger.error("Can't export the port-scan result", e)
					MiscUtils.showErrorPopup("Error", """
 	Exception class: ${e.javaClass.name}
 	Message: ${e.message}
 	""".trimIndent())
				}
			}
		}
		super.actionPerformed(button)
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		if (keyCode == Keyboard.KEY_ESCAPE)
		{
			mc.displayGuiScreen(prevGui)
			return
		}
		if (keyCode == Keyboard.KEY_TAB) tab(hostField, minPortField, maxPortField)
		if (running) return
		if (hostField.isFocused) hostField.textboxKeyTyped(typedChar, keyCode)
		if (minPortField.isFocused && !Character.isLetter(typedChar)) minPortField.textboxKeyTyped(typedChar, keyCode)
		if (maxPortField.isFocused && !Character.isLetter(typedChar)) maxPortField.textboxKeyTyped(typedChar, keyCode)
		if (threadsField.isFocused && !Character.isLetter(typedChar)) threadsField.textboxKeyTyped(typedChar, keyCode)
		super.keyTyped(typedChar, keyCode)
	}

	@Throws(IOException::class)
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		hostField.mouseClicked(mouseX, mouseY, mouseButton)
		minPortField.mouseClicked(mouseX, mouseY, mouseButton)
		maxPortField.mouseClicked(mouseX, mouseY, mouseButton)
		threadsField.mouseClicked(mouseX, mouseY, mouseButton)
		super.mouseClicked(mouseX, mouseY, mouseButton)
	}

	override fun updateScreen()
	{
		hostField.updateCursorCounter()
		minPortField.updateCursorCounter()
		maxPortField.updateCursorCounter()
		threadsField.updateCursorCounter()
		super.updateScreen()
	}

	override fun onGuiClosed()
	{
		Keyboard.enableRepeatEvents(false)
		running = false
		super.onGuiClosed()
	}

	private fun toggle()
	{
		if (running) running = false
		else
		{
			host = hostField.text

			if (host!!.isEmpty())
			{
				status = "\u00A7cInvalid host"
				return
			}

			try
			{
				minPort = minPortField.text.toInt()
			}
			catch (e: NumberFormatException)
			{
				status = "\u00A7cInvalid min port"
				return
			}

			try
			{
				maxPort = maxPortField.text.toInt()
			}
			catch (e: NumberFormatException)
			{
				status = "\u00A7cInvalid max port"
				return
			}

			val threads: Int
			try
			{
				threads = threadsField.text.toInt()
			}
			catch (e: NumberFormatException)
			{
				status = "\u00A7cInvalid threads"
				return
			}

			ports.clear()
			currentPort = minPort - 1
			checkedPort = minPort

			val threadPool = Executors.newWorkStealingPool(threads)
			val task = Runnable {
				try
				{
					while (running && currentPort < maxPort)
					{
						currentPort++
						val port = currentPort
						try
						{
							val socket = Socket()
							socket.connect(InetSocketAddress(host, port), 500)
							socket.close()
							synchronized(ports) { if (!ports.contains(port)) ports.add(port) }
						}
						catch (ignored: Exception)
						{
						}
						if (checkedPort < port) checkedPort = port
					}
					running = false
					buttonToggle.displayString = "Start"
				}
				catch (e: Exception)
				{
					status = "\u00A7a\u00A7l" + e.javaClass.simpleName + ": \u00A7c" + e.message
				}
			}

			repeat(threads) { threadPool.execute(task) }

			running = true
		}
	}
}
