/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator

import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MicrosoftAccount.AuthMethod.Companion.AZURE_APP
import me.liuli.elixir.account.MicrosoftAccount.Companion.buildFromOpenBrowser
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.showURL
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard

class GuiMicrosoft(private val prevGui: GuiAltManager) : GuiScreen()
{
    // Data Storage
    companion object
    {
        var apiKey: String = ""
    }

    // Buttons
    private lateinit var loginButton: GuiButton
    private lateinit var addAltAndLoginButton: GuiButton

    // Status
    private var status = "\u00A77Idle..."

    /**
     * Initialize The Altening Generator GUI
     */
    override fun initGui()
    {
        // Enable keyboard repeat events
        Keyboard.enableRepeatEvents(true)

        // Login button & Add to alt list and Login button
        val middleScreenX = width shr 1
        val middleScreenY = height shr 1
        val buttonList = buttonList

        addAltAndLoginButton = GuiButton(1, middleScreenX - 100, middleScreenY, 98, 20, "Add Alt and Login")
        buttonList.add(addAltAndLoginButton)

        loginButton = GuiButton(2, middleScreenX + 2, middleScreenY, 98, 20, "Just Login")
        buttonList.add(loginButton)

        buttonList.add(GuiButton(0, middleScreenX + 2, height - 54, 98, 20, "Back"))
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
        Fonts.font35.drawCenteredString("Microsoft", middleScreen, 6.0f, 0xffffff)
        Fonts.font35.drawCenteredString(status, middleScreen, 18.0f, 0xffffff)
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

            1, 2 ->
            {
                loginButton.enabled = false
                buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler
                {
                    override fun openUrl(url: String)
                    {
                        showURL(url)
                    }

                    override fun authResult(account: MicrosoftAccount)
                    {
                        val username = account.session.username
                        status = "\u00A7aYour name is now \u00A7b\u00A7l${username}\u00A7c."
                        mc.session = Session(username, account.session.uuid, account.session.token, "mojang")
                    }

                    override fun authError(error: String) = logger.error("Microsoft account authentication error: {}", error)
                }, AZURE_APP)
            }
        }
    }

    /**
     * Handle key typed
     */
    override fun keyTyped(typedChar: Char, keyCode: Int)
    { // Check if user want to escape from screen
        if (Keyboard.KEY_ESCAPE == keyCode)
        { // Send back to prev screen
            mc.displayGuiScreen(prevGui)
            return
        }

        super.keyTyped(typedChar, keyCode)
    }

    /**
     * Handle mouse clicked
     */
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    /**
     * Handle screen update
     */
    override fun updateScreen()
    {
        super.updateScreen()
    }

    /**
     * Handle gui closed
     */
    override fun onGuiClosed()
    {
        // Disable keyboard repeat events
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }
}
