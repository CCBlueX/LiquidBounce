package net.ccbluex.liquidbounce.ui.client.altmanager.sub.reconnect

import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.TabUtils
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetAddress
import java.net.URL
import java.util.*

/**
 * LiquidBounce Hacked Client
 * A minecraft forge injection client using Mixin
 *
 * @author CCBlueX, superblaubeere27
 * @game Minecraft
 */
class GuiFritzBoxReconnect(gui: GuiAltManager) : GuiScreen() {
    private val prevGui: GuiScreen
    private var reconnectButton: GuiButton? = null
    private var findRouter: GuiButton? = null
    private var routerIp: GuiTextField? = null
    private var status: String? = "§7Idle..."
    private var currentThread: Thread? = null

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 96, "Reconnect").also { reconnectButton = it })
        buttonList.add(GuiButton(0, width / 2 - 100, height / 4 + 120, "Back"))
        buttonList.add(GuiButton(2, width / 2 - 100, 85, "Find Router").also { findRouter = it })

        routerIp = GuiTextField(2, Fonts.font40, width / 2 - 100, 60, 200, 20)

        routerIp!!.isFocused = true
        routerIp!!.maxStringLength = Int.MAX_VALUE
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)
        Gui.drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)
        drawCenteredString(Fonts.font40, "Fritz!Box Reconnect", width / 2, 34, 0xffffff)

        if (status != null)
            drawCenteredString(Fonts.font35, status, width / 2, height / 4 + 84, 0xffffff)

        routerIp!!.drawTextBox()

        if (routerIp!!.text.isEmpty() && !routerIp!!.isFocused)
            drawCenteredString(Fonts.font40, "§7Router IP Address", width / 2 - 55, 66, 0xffffff)
        
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    @Throws(IOException::class)
    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return
        when (button.id) {
            0 -> {
                mc.displayGuiScreen(prevGui)
                currentThread!!.interrupt()
            }
            1 -> {
                currentThread = Thread(fun() {
                    if (routerIp!!.text.isEmpty()) {
                        status = "\u00a7cIP Address cannot be empty"
                        return
                    }

                    val oldIp = externalIP

                    status = "\u00a7aTrying to reconnect... (Current IP: $oldIp)"

                    // Source: https://github.com/doctordebug/FritzBoxIpChanger
                    try {
                        val url = URL("http://fritz.box:49000/igdupnp/control/WANIPConn1")

                        val conn = url.openConnection()
                        conn.setRequestProperty("Man", "/igdupnp/control/WANIPConn1 HTTP/1.1")
                        conn.setRequestProperty("Host", "fritz.box:49000")
                        conn.setRequestProperty("Content-Type", "text/xml; charset=\"utf-8\"")
                        conn.setRequestProperty("Content-Length", Integer.toString(REQUEST_BODY.length))
                        conn.setRequestProperty("SoapAction", "urn:schemas-upnp-org:service:WANIPConnection:1#ForceTermination")
                        // Write request body to SharePoint
                        conn.doOutput = true
                        val writer = OutputStreamWriter(conn.getOutputStream())
                        writer.write(REQUEST_BODY)
                        writer.close()
                        val sb = StringBuilder()

                        BufferedReader(InputStreamReader(conn.getInputStream())).use { reader ->
                            var inputLine: String?
                            while (reader.readLine().also { inputLine = it } != null) sb.append(inputLine)
                        }

                        status = "\u00a7aReconnected. New IP: $externalIP"
                    } catch (e: Exception) {
                        status = "\u00a7cFailed to reconnect: $e"
                    }
                })
                currentThread!!.start()
            }
            2 -> Thread(Runnable {
                status = "\u00a7aResolving fritz.box..."

                try {
                    val fritzBoxIp = InetAddress.getByName("fritz.box")

                    routerIp!!.text = Objects.requireNonNull(fritzBoxIp.hostAddress)

                    status = "\u00a7aFound Fritz!Box"
                } catch (ignored: Exception) {
                    status = "\u00a7cUnable to find router. Are you using a VPN? Did you change your DNS configuration?"
                }
            }).start()
        }
        super.actionPerformed(button)
    }

    private val externalIP: String
        private get() {
            try {
                val aws = URL("http://checkip.amazonaws.com")

                val input = BufferedReader(InputStreamReader(
                        aws.openStream()))

                return input.readLine()
            } catch (e: IOException) {
                e.printStackTrace()
            }
            return "Not connected"
        }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                mc.displayGuiScreen(prevGui)
                return
            }
            Keyboard.KEY_TAB -> {
                TabUtils.tab(routerIp)
                return
            }
            Keyboard.KEY_RETURN -> {
                actionPerformed(reconnectButton!!)
                return
            }
        }
        if (routerIp!!.isFocused) routerIp!!.textboxKeyTyped(typedChar, keyCode)
        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        routerIp!!.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() {
        routerIp!!.updateCursorCounter()
        super.updateScreen()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }

    companion object {
        const val REQUEST_BODY = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n" +
                "        <s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">\n" +
                "        <s:Body>\n" +
                "        <u:ForceTerminationResponse xmlns:u=\"urn:schemas-upnp-org:service:WANIPConnection:1\"></u:ForceTerminationResponse>\n" +
                "        </s:Body>\n" +
                "        </s:Envelope>"
    }

    init {
        prevGui = gui
    }
}