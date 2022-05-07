/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import com.thealtening.AltService.EnumAltService
import me.liuli.elixir.account.CrackedAccount
import me.liuli.elixir.account.MicrosoftAccount
import me.liuli.elixir.account.MojangAccount
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.elements.GuiPasswordField
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.TabUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import kotlin.concurrent.thread

class GuiLoginIntoAccount(private val prevGui: GuiAltManager, val directLogin: Boolean = false) : GuiScreen() {
    
    private lateinit var addButton: GuiButton
    private lateinit var clipboardButton: GuiButton
    private lateinit var username: GuiTextField
    private lateinit var password: GuiTextField
    
    private var status = "§7Idle..."

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        // Clipboard login
        buttonList.add(GuiButton(2, width / 2 - 100, 113, "Clipboard").also { clipboardButton = it })

        // Login via Microsoft account
        buttonList.add(GuiButton(3, width / 2 - 100, 143, "Login with Microsoft"))

        // Add and back button
        buttonList.add(GuiButton(1, width / 2 - 100, height - 54, 98, 20, if (directLogin) "Login" else "Add").also { addButton = it })
        buttonList.add(GuiButton(0, width / 2 + 2, height - 54, 98, 20, "Back"))
        
        username = GuiTextField(2, Fonts.font40, width / 2 - 100, 60, 200, 20)
        username.isFocused = true
        username.maxStringLength = Int.MAX_VALUE
        password = GuiPasswordField(3, Fonts.font40, width / 2 - 100, 85, 200, 20)
        password.maxStringLength = Int.MAX_VALUE
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        RenderUtils.drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)
        Fonts.font40.drawCenteredString(if (directLogin) "Direct Login" else "Add Account", width / 2.0f, 34f, 0xffffff)
        Fonts.font40.drawCenteredString("§7Login with Mojang", width / 2.0f, 49f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2.0f, height - 64f, 0xffffff)

        username.drawTextBox()
        password.drawTextBox()

        if (username.text.isEmpty() && !username.isFocused) {
            Fonts.font40.drawCenteredString(
                "§7Username / E-Mail",
                (width / 2 - 55).toFloat(),
                66f,
                0xffffff
            )
        }

        if (password.text.isEmpty() && !password.isFocused) {
            Fonts.font40.drawCenteredString(
                "§7Password",
                (width / 2 - 74).toFloat(),
                91f,
                0xffffff
            )
        }

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    public override fun actionPerformed(button: GuiButton) {
        // Not enabled buttons should be ignored
        if (!button.enabled) {
            return
        }

        when (button.id) {
            0 -> mc.displayGuiScreen(prevGui)

            1 -> {
                val usernameText = username.text
                val passwordText = password.text
                checkAndAddAccount(usernameText, passwordText)
            }

            2 -> try {
                val clipboardData = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String

                val accountData = clipboardData.split(":".toRegex(), limit = 2)
                if (!clipboardData.contains(":") || accountData.size != 2) {
                    status = "§cInvalid clipboard data. (Use: E-Mail:Password)"
                    return
                }

                checkAndAddAccount(accountData[0], accountData[1])
            } catch (e: UnsupportedFlavorException) {
                status = "§cClipboard flavor unsupported!"
                ClientUtils.getLogger().error("Failed to read data from clipboard.", e)
            }

            3 -> {
                MicrosoftAccount.buildFromOpenBrowser(object : MicrosoftAccount.OAuthHandler {

                    /**
                     * Called when the user has cancelled the authentication process or the thread has been interrupted
                     */
                    override fun authError(error: String) {
                        status = "§c$error"
                    }

                    /**
                     * Called when the user has completed authentication
                     */
                    override fun authResult(account: MicrosoftAccount) {
                        if (LiquidBounce.fileManager.accountsConfig.accountExists(account)) {
                            status = "§cThe account has already been added."
                            return
                        }

                        LiquidBounce.fileManager.accountsConfig.addAccount(account)
                        LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
                        status = "§aThe account has been added."
                        prevGui.status = status
                        mc.displayGuiScreen(prevGui)
                    }

                    /**
                     * Called when the server has prepared the user for authentication
                     */
                    override fun openUrl(url: String) {
                        MiscUtils.showURL(url)
                    }

                })
            }
        }
    }

    @Throws(IOException::class)
    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                mc.displayGuiScreen(prevGui)
                return
            }
            Keyboard.KEY_TAB -> {
                TabUtils.tab(username, password)
                return
            }
            Keyboard.KEY_RETURN -> {
                actionPerformed(addButton)
                return
            }
        }

        if (username.isFocused) {
            username.textboxKeyTyped(typedChar, keyCode)
        }

        if (password.isFocused) {
            password.textboxKeyTyped(typedChar, keyCode)
        }
        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        username.mouseClicked(mouseX, mouseY, mouseButton)
        password.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() {
        username.updateCursorCounter()
        password.updateCursorCounter()
        super.updateScreen()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
    }

    private fun checkAndAddAccount(usernameText: String, passwordText: String) {
        if (usernameText.isEmpty()) {
            // what?
            return
        }

        val minecraftAccount = if (passwordText.isEmpty()) {
            // cracked account
            val crackedAccount = CrackedAccount()
            crackedAccount.name = usernameText

            crackedAccount
        } else {
            // mojang account
            val mojangAccount = MojangAccount()
            mojangAccount.email = usernameText
            mojangAccount.password = passwordText

            mojangAccount
        }

        if (LiquidBounce.fileManager.accountsConfig.accountExists(minecraftAccount)) {
            status = "§cThe account has already been added."
            return
        }

        clipboardButton.enabled = false
        addButton.enabled = false

        thread(name = "Account-Checking-Task") {
            try {
                // Switch back to Mojang auth service
                val oldService = GuiAltManager.altService.currentService
                if (oldService != EnumAltService.MOJANG) {
                    GuiAltManager.altService.switchService(EnumAltService.MOJANG)
                }

                // Update account (login)
                minecraftAccount.update()
            } catch (e: Exception) {
                status = "§c" + e.message

                clipboardButton.enabled = true
                addButton.enabled = true
                return@thread
            }

            // Login directly into account
            if (directLogin) {
                Minecraft.getMinecraft().session = Session(
                    minecraftAccount.session.username,
                    minecraftAccount.session.uuid, minecraftAccount.session.token, "mojang"
                )
                status = "§aLogged into ${mc.session.username}."
            } else {
                LiquidBounce.fileManager.accountsConfig.addAccount(minecraftAccount)
                LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
                status = "§aThe account has been added."
            }
            prevGui.status = status
            mc.displayGuiScreen(prevGui)
        }
    }
}