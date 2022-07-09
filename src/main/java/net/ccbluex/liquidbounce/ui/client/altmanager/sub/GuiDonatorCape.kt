/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import net.ccbluex.liquidbounce.LiquidBounce



import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.ccbluex.liquidbounce.utils.runAsync
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.apache.http.HttpHeaders
import org.apache.http.HttpStatus
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpPut
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
import org.lwjgl.input.Keyboard
import java.io.File

class GuiDonatorCape(private val prevGui: GuiAltManager) : GuiScreen()
{

    // Data Storage
    companion object
    {
        var transferCode: String = ""
        var capeEnabled: Boolean = true
    }

    // Buttons
    private lateinit var stateButton: GuiButton

    // User Input Fields
    private lateinit var transferCodeField: GuiTextField

    // Status
    private var status = "\u00A77Idle..."

    // Donator Cape

    /**
     * Initialize Donator Cape GUI
     */
    override fun initGui()
    { // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Add buttons to screen
        val buttonX = (width shr 1) - 100
        val quarterScreen = height shr 2

        stateButton = GuiButton(1, buttonX, 105, "Disable Cape")

        val buttonList = buttonList
        buttonList.add(stateButton)
        buttonList.add(GuiButton(2, buttonX, quarterScreen + 96, "Donate to get Cape"))
        buttonList.add(GuiButton(0, buttonX, quarterScreen + 120, "Back"))

        // Add fields to screen
        transferCodeField = GuiTextField(666, Fonts.font40, buttonX, 80, 200, 20)
        transferCodeField.isFocused = true
        transferCodeField.maxStringLength = Integer.MAX_VALUE
        transferCodeField.text = transferCode

        // Call sub method
        super.initGui()
    }

    /**
     * Draw screen
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    { // Draw background to screen
        drawBackground(0)
        RenderUtils.drawRect(30.0f, 30.0f, width - 30.0f, height - 30.0f, Integer.MIN_VALUE)

        // Draw title and status
        val middleScreen = (width shr 1).toFloat()

        Fonts.font35.drawCenteredString("Donator Cape", middleScreen, 36.0f, 0xffffff)
        Fonts.font35.drawCenteredString("use prefix \"file:\" to load cape file from .minecraft/LiquidBounce directory.", middleScreen, height - 70.0f, 0xffffff)
        Fonts.font35.drawCenteredString("(ex. \"file:cape\" or \"file:cape.png\" will load \"" + LiquidBounce.fileManager.dir.toString() + File.separatorChar + "cape.png\")", middleScreen, height - 56.0f, 0xffffff)

        Fonts.font35.drawCenteredString(status, middleScreen, (height shr 2) + 80f, 0xffffff)

        // Draw fields
        transferCodeField.drawTextBox()

        Fonts.font40.drawCenteredString("\u00A77Transfer Code:", middleScreen - 65, 66.0f, 0xffffff)

        stateButton.displayString = if (capeEnabled)
        {
            "Disable Cape"
        }
        else
        {
            "Enable Cape"
        }

        // Call sub method
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Handle button actions
     */
    override fun actionPerformed(button: GuiButton)
    {
        if (!button.enabled) return

        when (button.id)
        {
            0 -> mc.displayGuiScreen(prevGui)

            1 ->
            {
                stateButton.enabled = false

                if (transferCodeField.text.startsWith("file:", ignoreCase = true))
                {
                    stateButton.enabled = true
                    capeEnabled = !capeEnabled
                    status = if (capeEnabled) "\u00A7aSuccessfully enabled offline cape"
                    else "\u00A7aSuccessfully disabled offline cape"
                }
                else runAsync {
                    val httpClient = HttpClients.createDefault()
                    val headers = arrayOf(BasicHeader(HttpHeaders.CONTENT_TYPE, "application/json"), BasicHeader(HttpHeaders.AUTHORIZATION, transferCodeField.text))
                    val request = if (capeEnabled)
                    {
                        HttpDelete("http://capes.liquidbounce.net/api/v1/cape/self")
                    }
                    else
                    {
                        HttpPut("http://capes.liquidbounce.net/api/v1/cape/self")
                    }
                    request.setHeaders(headers)
                    val response = httpClient.execute(request)
                    val statusCode = response.statusLine.statusCode

                    status = if (statusCode == HttpStatus.SC_NO_CONTENT)
                    {
                        capeEnabled = !capeEnabled
                        if (capeEnabled)
                        {
                            "\u00A7aSuccessfully enabled cape"
                        }
                        else
                        {
                            "\u00A7aSuccessfully disabled cape"
                        }
                    }
                    else
                    {
                        "\u00A7cFailed to toggle cape ($statusCode)"
                    }

                    stateButton.enabled = true
                }
            }

            2 ->
            {
                MiscUtils.showURL("https://donate.liquidbounce.net")
            }
        }

        // Call sub method
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar: Char, keyCode: Int)
    {

        // Check if user want to escape from screen
        if (Keyboard.KEY_ESCAPE == keyCode)
        {

            // Send back to prev screen
            mc.displayGuiScreen(prevGui)

            // Quit
            return
        }

        // Check if field is focused, then call key typed
        if (transferCodeField.isFocused) transferCodeField.textboxKeyTyped(typedChar, keyCode)

        // Call sub method
        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {

        // Call mouse clicked to field
        transferCodeField.mouseClicked(mouseX, mouseY, mouseButton)

        // Call sub method
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen()
    {
        transferCodeField.updateCursorCounter()
        super.updateScreen()
    }

    /**
     * Handle gui closed
     */
    override fun onGuiClosed()
    { // Disable keyboard repeat events
        Keyboard.enableRepeatEvents(false)

        // Set API key
        transferCode = transferCodeField.text

        // Call sub method
        super.onGuiClosed()
    }
}
