/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.tools

import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.TabUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.FileWriter
import java.net.InetSocketAddress
import java.net.Socket
import javax.swing.JOptionPane
import kotlin.concurrent.thread

class GuiPortScanner(private val prevGui: GuiScreen) : GuiScreen() {

    private val ports = mutableListOf<Int>()
    private lateinit var hostField: GuiTextField
    private lateinit var minPortField: GuiTextField
    private lateinit var maxPortField: GuiTextField
    private lateinit var threadsField: GuiTextField
    private lateinit var buttonToggle: GuiButton
    private var running = false
    private var status = "§7Waiting..."
    private var host: String = ""
    private var currentPort = 0
    private var maxPort = 0
    private var minPort = 0
    private var checkedPort = 0

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        hostField = GuiTextField(0, Fonts.font40, width / 2 - 100, 60, 200, 20).apply {
            isFocused = true
            maxStringLength = Int.MAX_VALUE
            text = "localhost"
        }

        minPortField = GuiTextField(1, Fonts.font40, width / 2 - 100, 90, 90, 20).apply {
            maxStringLength = 5
            text = "1"
        }

        maxPortField = GuiTextField(2, Fonts.font40, width / 2 + 10, 90, 90, 20).apply {
            maxStringLength = 5
            text = "65535"
        }

        threadsField = GuiTextField(3, Fonts.font40, width / 2 - 100, 120, 200, 20).apply {
            maxStringLength = Int.MAX_VALUE
            text = "500"
        }

        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 95, if (running) "Stop" else "Start").also { buttonToggle = it })
        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"))
        buttonList.add(GuiButton(2, width / 2 - 100, height / 4 + 155, "Export"))

        super.initGui()
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        Fonts.font40.drawCenteredString("Port Scanner", (width / 2).toFloat(), 34f, 0xffffff)
        Fonts.font35.drawCenteredString(
            if (running) "§7$checkedPort §8/ §7$maxPort" else status, (width / 2).toFloat(), (height / 4 + 80).toFloat(), 0xffffff
        )

        buttonToggle.displayString = if (running) "Stop" else "Start"

        hostField.drawTextBox()
        minPortField.drawTextBox()
        maxPortField.drawTextBox()
        threadsField.drawTextBox()

        Fonts.font40.drawString("§c§lPorts:", 2, 2, Color.WHITE.hashCode())

        synchronized(ports) {
            var yOffset = 12
            for (port in ports) {
                Fonts.font35.drawString(port.toString(), 2, yOffset, Color.WHITE.hashCode())
                yOffset += Fonts.font35.fontHeight
            }
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton) {
        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> togglePortScanning()
            2 -> exportPorts()
        }
        super.actionPerformed(button)
    }

    private fun togglePortScanning() {
        if (running) {
            running = false
        } else {
            host = hostField.text
            if (host.isEmpty()) {
                status = "§cInvalid host"
                return
            }

            minPort = minPortField.text.toIntOrNull() ?: run {
                status = "§cInvalid min port"
                return
            }

            maxPort = maxPortField.text.toIntOrNull() ?: run {
                status = "§cInvalid max port"
                return
            }

            val threads = threadsField.text.toIntOrNull() ?: run {
                status = "§cInvalid threads"
                return
            }

            ports.clear()
            currentPort = minPort - 1
            checkedPort = minPort

            repeat(threads) {
                thread {
                    while (running && currentPort < maxPort) {
                        currentPort++
                        val port = currentPort
                        try {
                            Socket().use { socket ->
                                socket.connect(InetSocketAddress(host, port), 500)
                            }
                            synchronized(ports) {
                                if (!ports.contains(port)) ports.add(port)
                            }
                        } catch (ignored: Exception) {
                        }
                        if (checkedPort < port) checkedPort = port
                    }
                    running = false
                    buttonToggle.displayString = "Start"
                }
            }

            running = true
        }
        buttonToggle.displayString = if (running) "Stop" else "Start"
    }

    private fun exportPorts() {
        val selectedFile = MiscUtils.saveFileChooser() ?: return
        if (selectedFile.isDirectory) return

        try {
            if (!selectedFile.exists()) selectedFile.createNewFile()

            FileWriter(selectedFile).use { fileWriter ->
                fileWriter.write("Portscan\r\n")
                fileWriter.write("Host: $host\r\n\r\n")
                fileWriter.write("Ports ($minPort - $maxPort):\r\n")
                ports.forEach { port -> fileWriter.write("$port\r\n") }
            }
            JOptionPane.showMessageDialog(null, "Exported successfully!", "Port Scanner", JOptionPane.INFORMATION_MESSAGE)
        } catch (e: Exception) {
            e.printStackTrace()
            MiscUtils.showErrorPopup("Error", "Exception class: ${e::class.java.name}\nMessage: ${e.message}")
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        if (keyCode == Keyboard.KEY_ESCAPE) {
            mc.displayGuiScreen(prevGui)
            return
        }

        if (keyCode == Keyboard.KEY_TAB) {
            TabUtils.tab(hostField, minPortField, maxPortField, threadsField)
        }

        if (running) return

        when {
            hostField.isFocused -> hostField.textboxKeyTyped(typedChar, keyCode)
            minPortField.isFocused && !typedChar.isLetter() -> minPortField.textboxKeyTyped(typedChar, keyCode)
            maxPortField.isFocused && !typedChar.isLetter() -> maxPortField.textboxKeyTyped(typedChar, keyCode)
            threadsField.isFocused && !typedChar.isLetter() -> threadsField.textboxKeyTyped(typedChar, keyCode)
        }

        super.keyTyped(typedChar, keyCode)
    }

    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        hostField.mouseClicked(mouseX, mouseY, mouseButton)
        minPortField.mouseClicked(mouseX, mouseY, mouseButton)
        maxPortField.mouseClicked(mouseX, mouseY, mouseButton)
        threadsField.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() {
        hostField.updateCursorCounter()
        minPortField.updateCursorCounter()
        maxPortField.updateCursorCounter()
        threadsField.updateCursorCounter()
        super.updateScreen()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        running = false
        super.onGuiClosed()
    }
}