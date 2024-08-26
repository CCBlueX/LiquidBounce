/*
    * LiquidBounce Hacked Client
    * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
    * https://github.com/CCBlueX/LiquidBounce/
    */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.event.EventManager.callEvent
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.file.FileManager.accountsConfig
import net.ccbluex.liquidbounce.file.FileManager.saveConfig
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.RandomUtils.randomUsername
import net.minecraft.client.gui.ButtonWidget
import net.minecraft.client.gui.screen.Screen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiLoginIntoAccount(private val prevGui: GuiAltManager, val directLogin: Boolean = false) : Screen() {

    private lateinit var addButton: ButtonWidget
    private lateinit var username: GuiTextField

    private var status = ""

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        // Add button
        buttonList.run {
            add(ButtonWidget(1, width / 2 - 100, height / 2 - 60 , if (directLogin) "Login" else "Add")
                .also { addButton = it })

            // Random button
            add(ButtonWidget(2, width / 2 + 105, height / 2 - 90, 40, 20, "Random"))

            // Login via Microsoft account
            add(ButtonWidget(3, width / 2 - 100, height / 2, "${if (directLogin) "Login to" else "Add"} a Microsoft account"))

            // Back button
            add(ButtonWidget(0, width / 2 - 100, height / 2 + 30, "Back"))
        }

        username = GuiTextField(2, Fonts.font40, width / 2 - 100, height / 2 - 90, 200, 20)
        username.isFocused = false
        username.maxStringLength = 16
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)
        Fonts.font40.drawCenteredString(if (directLogin) "Direct Login" else "Add Account", width / 2f, height / 2 - 170f, 0xffffff)
        Fonts.font40.drawCenteredString("§7${if (directLogin) "Login to" else "Add"} an offline account", width / 2f, height / 2 - 110f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2f, height / 2f - 30, 0xffffff)

        username.drawTextBox()

        if (username.text.isEmpty() && !username.isFocused)
            Fonts.font40.drawCenteredString("§7Username", width / 2 - 72f, height / 2 - 84f, 0xffffff)

        super.drawScreen(mouseX, mouseY, partialTicks)
    }

    public override fun actionPerformed(button: ButtonWidget) {
        // Not enabled buttons should be ignored
        if (!button.enabled) {
            return
        }

        when (button.id) {
            0 -> mc.displayScreen(prevGui)

            1 -> {
                val usernameText = username.text
                checkAndAddAccount(usernameText)
            }

            2 -> {
                username.text = randomUsername()
            }

            3 -> {
                mc.displayScreen(
                    GuiMicrosoftLoginProgress({
                        status = it
                    }, {
                        prevGui.status = status
                        mc.displayScreen(prevGui)
                    })
                )
            }
        }
    }

    @Throws(IOException::class)
    public override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> {
                mc.displayScreen(prevGui)
                return
            }

            Keyboard.KEY_RETURN -> {
                actionPerformed(addButton)
                return
            }

            Keyboard.KEY_TAB -> {
                username.isFocused = true
                return
            }
        }

        if (username.isFocused) {
            username.textboxKeyTyped(typedChar, keyCode)
        }

        super.keyTyped(typedChar, keyCode)
    }

    @Throws(IOException::class)
    public override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        username.mouseClicked(mouseX, mouseY, mouseButton)
        super.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun updateScreen() {
        username.updateCursorCounter()
        super.updateScreen()
    }

    override fun onGuiClosed() {
        Keyboard.enableRepeatEvents(false)
    }

    private fun checkAndAddAccount(usernameText: String) {
        if (usernameText.isEmpty() || usernameText.length < 3) {
            status = "§cInput at least 3 characters long username."
            return
        }

        val crackedAccount = CrackedAccount()
        crackedAccount.name = usernameText

        if (accountsConfig.accountExists(crackedAccount)) {
            status = "§cThis account already exists."
            return
        }

        addButton.enabled = false

        if (directLogin) {
            // Login directly into account
            mc.session = Session(
                crackedAccount.session.username, crackedAccount.session.uuid,
                crackedAccount.session.token, crackedAccount.session.type
            )
            callEvent(SessionEvent())
            status = "§aLogged into ${mc.session.username}."
        } else {
            accountsConfig.addAccount(crackedAccount)
            saveConfig(accountsConfig)
            status = "§aThe account has been added."
        }

        prevGui.status = status
        mc.displayScreen(prevGui)
    }
}
