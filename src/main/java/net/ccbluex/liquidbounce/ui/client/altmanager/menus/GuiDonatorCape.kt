/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import net.ccbluex.liquidbounce.cape.CapeService
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import kotlin.math.log

class GuiDonatorCape(private val prevGui: GuiAltManager) : GuiScreen() {

    // Buttons
    private lateinit var upperButton: GuiButton
    private lateinit var lowerButton: GuiButton

    // User Input Fields
    private lateinit var transferCodeField: GuiTextField

    // Status
    private var status = ""

    private val loggedIntoAccount: Boolean
        get() = CapeService.clientCapeUser != null

    /**
     * Initialize Donator Cape GUI
     */
    override fun initGui() {
        // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Add buttons to screen
        val upperButtonText = if (loggedIntoAccount)
            "Login"
        else if (CapeService.clientCapeUser?.enabled == true)
            "Disable Cape"
        else
            "Enable Cape"

        buttonList.add(GuiButton(1, width / 2 - 100, height / 2 - 60, upperButtonText).apply { upperButton = this })
        buttonList.add(GuiButton(2, width / 2 - 100, height / 2 - 40, if (loggedIntoAccount) "Logout" else "Donate to get Cape").apply { lowerButton = this })
        buttonList.add(GuiButton(0, width / 2 - 100, height / 2 + 30, "Back"))

        // Add fields to screen
        transferCodeField = GuiPasswordField(666, Fonts.font40, width / 2 - 100, height / 2 - 90, 200, 20)
        transferCodeField.isFocused = false
        transferCodeField.maxStringLength = Integer.MAX_VALUE
        transferCodeField.text = CapeService.clientCapeUser?.token ?: ""

        // Call sub method
        super.initGui()
    }

    /**
     * Draw screen
     */
    override fun drawScreen(mouseX : Int, mouseY : Int, partialTicks : Float) {
        // Draw background to screen
        drawBackground(0)
        drawRect(30f, 30f, width - 30f, height - 30f, Integer.MIN_VALUE)

        // Draw title and status
        Fonts.font40.drawCenteredString("Donator Cape", width / 2f, height / 2 - 150f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2f, height / 2f - 30, 0xffffff)

        // Draw fields
        if (loggedIntoAccount) {
            transferCodeField.drawTextBox()

            if (transferCodeField.text.isEmpty() && !transferCodeField.isFocused) {
                Fonts.font40.drawCenteredString("§7Transfer Code", width / 2f - 60f, height / 2 - 84f, 0xffffff)
            }
        }

        upperButton.displayString = if (loggedIntoAccount)
            "Login"
        else if (CapeService.clientCapeUser?.enabled == true)
            "Disable Cape"
        else
            "Enable Cape"

        lowerButton.displayString = if (loggedIntoAccount) "Logout" else "Donate to get Cape"

        // Call sub method
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Handle button actions
     */
    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) {
            return
        }

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                if (loggedIntoAccount) {
                    upperButton.enabled = false

                    CapeService.toggleCapeState { enabled, success, statusCode ->
                        status = if (success) {
                            if (enabled) {
                                "§aSuccessfully enabled cape"
                            } else {
                                "§aSuccessfully disabled cape"
                            }
                        } else {
                            "§cFailed to toggle cape ($statusCode)"
                        }

                        upperButton.enabled = true
                    }
                } else {
                    if (transferCodeField.text.isBlank()) {
                        status = "§cTransfer code can't be blank"
                        return
                    }

                    runCatching {
                        CapeService.login(transferCodeField.text)
                    }.onSuccess {
                        status = "§aSuccessfully logged in"
                    }.onFailure {
                        status = "§cFailed to login (${it.message})"
                    }
                }
            }
            2 -> {
                if (loggedIntoAccount) {
                    CapeService.logout()
                    status = "§aSuccessfully logged out"
                } else {
                    MiscUtils.showURL("https://donate.liquidbounce.net")
                }
            }
        }

        // Call sub method
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar : Char, keyCode : Int) {
        // Check if user want to escape from screen
        if(Keyboard.KEY_ESCAPE == keyCode) {
            // Send back to prev screen
            mc.displayGuiScreen(prevGui)

            // Quit
            return
        }

        // Check if field is focused, then call key typed
        if(transferCodeField.isFocused) transferCodeField.textboxKeyTyped(typedChar, keyCode)

        // Call sub method
        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX : Int, mouseY : Int, mouseButton : Int) {
        // Call mouse clicked to field
        transferCodeField.mouseClicked(mouseX, mouseY, mouseButton)

        // Call sub method
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen() {
        transferCodeField.updateCursorCounter()
        super.updateScreen()
    }

    /**
     * Handle gui closed
     */
    override fun onGuiClosed() {
        // Disable keyboard repeat events
        Keyboard.enableRepeatEvents(false)

        // Call sub method
        super.onGuiClosed()
    }
}