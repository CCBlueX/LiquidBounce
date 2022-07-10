/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager.Companion.login
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.TabUtils.tab
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType
import net.ccbluex.liquidbounce.utils.render.ColorUtils.translateAlternateColorCodes
import net.ccbluex.liquidbounce.utils.runAsync
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException

class GuiDirectLogin(gui: GuiAltManager) : GuiScreen()
{
    private val prevGui: GuiScreen = gui

    private lateinit var loginButton: GuiButton
    private lateinit var clipboardLoginButton: GuiButton
    private lateinit var username: GuiTextField
    private lateinit var password: GuiTextField

    private var status = "\u00A77Idle..."

    override fun initGui()
    {
        Keyboard.enableRepeatEvents(true)

        val buttonX = (width shr 1) - 100
        val quarterScreen = height shr 2

        val buttonList = buttonList

        buttonList.add(GuiButton(1, buttonX, quarterScreen + 72, "Login").also { loginButton = it })
        buttonList.add(GuiButton(2, buttonX, quarterScreen + 96, "Clipboard Login").also { clipboardLoginButton = it })
        buttonList.add(GuiButton(0, buttonX, quarterScreen + 120, "Back"))

        username = GuiTextField(2, Fonts.font40, buttonX, 60, 200, 20).apply {
            isFocused = true
            maxStringLength = Int.MAX_VALUE
        }

        password = GuiPasswordField(3, Fonts.font40, buttonX, 85, 200, 20).apply { maxStringLength = Int.MAX_VALUE }
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    {
        drawBackground(0)

        drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)

        val middleScreen = (width shr 1).toFloat()

        Fonts.font40.drawCenteredString("Direct Login", middleScreen, 34f, 0xffffff)
        Fonts.font35.drawCenteredString(status, middleScreen, (height shr 2) + 60f, 0xffffff)

        username.drawTextBox()
        password.drawTextBox()

        if (username.text.isEmpty() && !username.isFocused) Fonts.font40.drawCenteredString("\u00A77Username / E-Mail", middleScreen - 55, 66f, 0xffffff)
        if (password.text.isEmpty() && !password.isFocused) Fonts.font40.drawCenteredString("\u00A77Password", middleScreen - 74, 91f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    override fun actionPerformed(button: GuiButton)
    {
        if (!button.enabled) return
        when (button.id)
        {
            0 -> mc.displayGuiScreen(prevGui)

            1 ->
            {
                if (username.text.isEmpty())
                {
                    status = "\u00A7cYou have to fill in both fields!"
                    return
                }

                loginButton.enabled = false
                clipboardLoginButton.enabled = false

                runAsync {
                    status = "\u00A7aLogging in..."
                    status = if (password.text.isEmpty()) login(MinecraftAccount(AltServiceType.MOJANG, translateAlternateColorCodes(username.text))) else login(MinecraftAccount(AltServiceType.MOJANG, username.text, password.text))

                    loginButton.enabled = true
                    clipboardLoginButton.enabled = true
                }
            }

            2 -> try
            {
                val clipboardData = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
                val args = clipboardData.split(":", ignoreCase = true, limit = 2)

                if (!clipboardData.contains(":") || args.size != 2)
                {
                    status = "\u00A7cInvalid clipboard data. (Use: E-Mail:Password)"
                    return
                }

                loginButton.enabled = false
                clipboardLoginButton.enabled = false

                runAsync {
                    status = "\u00A7aLogging in..."
                    status = login(MinecraftAccount(AltServiceType.MOJANG, args[0], args[1]))

                    loginButton.enabled = true
                    clipboardLoginButton.enabled = true
                }
            }
            catch (e: UnsupportedFlavorException)
            {
                status = "\u00A7cClipboard flavor unsupported!"
                logger.error("Failed to read data from clipboard.", e)
            }
            catch (e: IOException)
            {
                status = "\u00A7cUnknown error! (See log)"
                logger.error(e)
            }
        }
    }

    @Throws(IOException::class)
    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        when (keyCode)
        {
            Keyboard.KEY_ESCAPE ->
            {
                mc.displayGuiScreen(prevGui)
                return
            }

            Keyboard.KEY_TAB ->
            {
                tab(username, password)
                return
            }

            Keyboard.KEY_RETURN ->
            {
                actionPerformed(loginButton)
                return
            }
        }

        if (username.isFocused) username.textboxKeyTyped(typedChar, keyCode)
        if (password.isFocused) password.textboxKeyTyped(typedChar, keyCode)

        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        username.mouseClicked(mouseX, mouseY, mouseButton)
        password.mouseClicked(mouseX, mouseY, mouseButton)

        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen()
    {
        username.updateCursorCounter()
        password.updateCursorCounter()

        super.updateScreen()
    }

    override fun onGuiClosed()
    {
        Keyboard.enableRepeatEvents(false)
        super.onGuiClosed()
    }
}
