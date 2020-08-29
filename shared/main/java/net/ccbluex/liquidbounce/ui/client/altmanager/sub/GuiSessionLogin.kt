/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import com.thealtening.AltService
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.login.LoginUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.mcleaks.MCLeaks
import org.lwjgl.input.Keyboard
import kotlin.concurrent.thread


class GuiSessionLogin(private val prevGui: GuiAltManager) : WrappedGuiScreen() {

    // Buttons
    private lateinit var loginButton: IGuiButton

    // User Input Fields
    private lateinit var sessionTokenField: IGuiTextField

    // Status
    private var status = ""

    /**
     * Initialize Session Login GUI
     */
    override fun initGui() {
        // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Add buttons to screen


        loginButton = classProvider.createGuiButton(1, representedScreen.width / 2 - 100, representedScreen.height / 4 + 96, "Login")
        representedScreen.buttonList.add(loginButton)


        representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width / 2 - 100, representedScreen.height / 4 + 120, "Back"))

        // Add fields to screen
        sessionTokenField = classProvider.createGuiTextField(666, Fonts.font40, representedScreen.width / 2 - 100, 80, 200, 20)
        sessionTokenField.isFocused = true
        sessionTokenField.maxStringLength = Integer.MAX_VALUE
        sessionTokenField

        // Call sub method
        super.initGui()
    }

    /**
     * Draw screen
     */
    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw background to screen
        representedScreen.drawBackground(0)
        RenderUtils.drawRect(30.0f, 30.0f, representedScreen.width - 30.0f, representedScreen.height - 30.0f, Integer.MIN_VALUE)

        // Draw title and status
        Fonts.font35.drawCenteredString("Session Login", representedScreen.width / 2.0f, 36.0f, 0xffffff)
        Fonts.font35.drawCenteredString(status, representedScreen.width / 2.0f, representedScreen.height / 4.0f + 80.0f, 0xffffff)

        // Draw fields
        sessionTokenField.drawTextBox()

        Fonts.font40.drawCenteredString("§7Session Token:", representedScreen.width / 2.0f - 65.0f, 66.0f, 0xffffff)

        // Call sub method
        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    /**
     * Handle button actions
     */
    override fun actionPerformed(button: IGuiButton) {
        if (!button.enabled) return

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui.representedScreen)
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
                                    ClientUtils.getLogger().error("Something went wrong while trying to switch alt service.", e)
                                } catch (e: IllegalAccessException) {
                                    ClientUtils.getLogger().error("Something went wrong while trying to switch alt service.", e)
                                }
                            }

                            MCLeaks.remove()

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
        // Check if user want to escape from screen
        if (Keyboard.KEY_ESCAPE == keyCode) {
            // Send back to prev screen
            mc.displayGuiScreen(prevGui.representedScreen)

            // Quit
            return
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