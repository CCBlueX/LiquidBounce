/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.TabUtils.tab
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger
import javax.swing.JOptionPane

class GuiPortScanner(private val prevGui: GuiScreen) : GuiScreen()
{
    private val ports: MutableCollection<Int> = ArrayList(65536)

    private lateinit var hostField: GuiTextField
    private lateinit var minPortField: GuiTextField
    private lateinit var maxPortField: GuiTextField
    private lateinit var threadsField: GuiTextField
    private lateinit var buttonToggle: GuiButton

    private var running = false
    private var status = "\u00A77Idle..."
    private var host: String? = null
    private var maxPort = 0
    private var minPort = 0
    private var checkedPort = AtomicInteger(0)

    override fun initGui()
    {
        Keyboard.enableRepeatEvents(true)

        val middleScreen = width shr 1

        val buttonX = middleScreen - 100

        hostField = GuiTextField(0, Fonts.font40, buttonX, 60, 200, 20).apply { isFocused = true }.apply { maxStringLength = Int.MAX_VALUE }.apply { text = "localhost" }
        minPortField = GuiTextField(1, Fonts.font40, buttonX, 90, 90, 20).apply { maxStringLength = 5 }.apply { text = "1" }
        maxPortField = GuiTextField(2, Fonts.font40, middleScreen + 10, 90, 90, 20).apply { maxStringLength = 5 }.apply { text = "65535" }
        threadsField = GuiTextField(3, Fonts.font40, buttonX, 120, 200, 20).apply { maxStringLength = Int.MAX_VALUE }.apply { text = "500" }

        val quarterScreen = height shr 2

        val buttonList = buttonList
        buttonList.add(GuiButton(1, buttonX, quarterScreen + 95, if (running) "Stop" else "Start").also { buttonToggle = it })
        buttonList.add(GuiButton(0, buttonX, quarterScreen + 120, "Back"))
        buttonList.add(GuiButton(2, buttonX, quarterScreen + 155, "Export"))

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        val titleFont = Fonts.font40

        val textFont = Fonts.font35
        val textFontHeight = textFont.fontHeight

        titleFont.drawCenteredString("Port Scanner", (width shr 1).toFloat(), 34f, 0xffffff)

        textFont.drawCenteredString(if (running) "\u00A77$checkedPort \u00A78/ \u00A77$maxPort" else status, (width shr 1).toFloat(), (height shr 2) + 80f, 0xffffff)

        buttonToggle.displayString = if (running) "Stop" else "Start"
        hostField.drawTextBox()
        minPortField.drawTextBox()
        maxPortField.drawTextBox()
        threadsField.drawTextBox()

        titleFont.drawString("\u00A7c\u00A7lPorts:", 2, 2, Color.WHITE.hashCode())

        synchronized(ports) {
            var height = 12

            for (integer in ports)
            {
                textFont.drawString("$integer", 2, height, Color.WHITE.hashCode())
                height += textFontHeight
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun actionPerformed(button: GuiButton)
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
                    val fileWriter = selectedFile.bufferedWriter()
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

            if (host.isNullOrEmpty())
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
            checkedPort.set(minPort)

            val threadPool = Executors.newWorkStealingPool(threads)
            // FIXME: Check the solution really works
            val task = { startPort: Int, endPort: Int ->
                Runnable {
                    var currentPort = startPort - 1

                    try
                    {
                        while (running && currentPort < endPort)
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
                            if (checkedPort.get() < port) checkedPort.set(port)
                        }
                        running = false
                        buttonToggle.displayString = "Start"
                    }
                    catch (e: Exception)
                    {
                        status = "\u00A7a\u00A7l" + e.javaClass.simpleName + ": \u00A7c" + e.message
                    }
                }
            }

            var offset = 0
            val mod = (maxPort - minPort) % threads
            val piece = (maxPort - minPort - mod) / threads
            repeat(threads) {
                val currentPiece = if (it == threads - 1) mod else piece
                threadPool.execute(task(offset, offset + currentPiece))
                offset += currentPiece + 1
            }

            running = true
        }
    }
}
