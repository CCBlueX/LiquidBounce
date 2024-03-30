/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import com.thealtening.AltService
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.LOGGER
import net.ccbluex.liquidbounce.utils.login.LoginUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRectNew
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import kotlin.concurrent.thread


class GuiSessionLogin(private val prevGui: GuiAltManager) : GuiScreen() {

    // Buttons
    private lateinit var loginButton: GuiButton

    // User Input Fields
    private lateinit var sessionTokenField: GuiTextField

    // Status
    private var status = ""

    /**
     * Initialize Session Login GUI
     */
    override fun initGui() {
        // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Add buttons to screen

        buttonList.run {
            add(GuiButton(1, width / 2 - 100, height / 2 - 60, "Login").also { loginButton = it })

            add(GuiButton(0, width / 2 - 100, height / 2 - 30, "Back"))
        }

        // Add fields to screen
        sessionTokenField = GuiTextField(666, Fonts.font40, width / 2 - 100, height / 2 - 90, 200, 20)
        sessionTokenField.isFocused = false
        sessionTokenField.maxStringLength = 1000

        // Call sub method
        super.initGui()
    }

    /**
     * Draw screen
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw background to screen
        drawBackground(0)
        drawRectNew(30f, 30f, width - 30f, height - 30f, Integer.MIN_VALUE)

        // Draw title and status
        Fonts.font40.drawCenteredString("Session Login", width / 2f, height / 2 - 150f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2f, height / 2f, 0xffffff)

        // Draw fields
        sessionTokenField.drawTextBox()

        if (sessionTokenField.text.isEmpty() && !sessionTokenField.isFocused)
            Fonts.font40.drawCenteredString("§7Session Token", width / 2f - 60f, height / 2 - 84f, 0xffffff)

        // Call sub method
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Handle button actions
     */
    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)
            1 -> {
                loginButton.enabled = false
                status = "§aLogging in..."

                thread {
                    val loginResult = LoginUtils.loginSessionId(sessionTokenField.text)

                    status = when (loginResult) {
                        LoginUtils.LoginResult.LOGGED -> {
                            if (GuiAltManager.altService.currentService != AltService.EnumAltService.MOJANG) {
                                try {
                                    GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)
                                } catch (e: NoSuchFieldException) {
                                    LOGGER.error("Something went wrong while trying to switch alt service.", e)
                                } catch (e: IllegalAccessException) {
                                    LOGGER.error("Something went wrong while trying to switch alt service.", e)
                                }
                            }

                            "§cYour name is now §f§l${mc.session.username}§c"
                        }
                        LoginUtils.LoginResult.FAILED_PARSE_TOKEN -> "§cFailed to parse Session ID!"
                        LoginUtils.LoginResult.INVALID_ACCOUNT_DATA -> "§cInvalid Session ID!"
                        else -> ""
                    }

                    loginButton.enabled = true
                }
            }
        }
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            // Check if user want to escape from screen
            Keyboard.KEY_ESCAPE -> {
                // Send back to prev screen
                mc.displayGuiScreen(prevGui)
                return
            }

            Keyboard.KEY_TAB -> {
                sessionTokenField.isFocused = true
                return
            }

            Keyboard.KEY_RETURN -> {
                actionPerformed(loginButton)
                return
            }
        }

        // Check if field is focused, then call key typed
        if (sessionTokenField.isFocused) sessionTokenField.textboxKeyTyped(typedChar, keyCode)

        // Call sub method
        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        // Call mouse clicked to field
        sessionTokenField.mouseClicked(mouseX, mouseY, mouseButton)

        // Call sub method
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen() {
        sessionTokenField.updateCursorCounter()
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
