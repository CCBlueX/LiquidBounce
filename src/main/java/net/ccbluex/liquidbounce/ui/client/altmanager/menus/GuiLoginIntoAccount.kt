/*
    * LiquidBounce Hacked Client
    * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
    * https://github.com/CCBlueX/LiquidBounce/
    */
package net.ccbluex.liquidbounce.ui.client.altmanager.menus

import me.liuli.elixir.account.CrackedAccount
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.misc.RandomUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import net.minecraft.util.Session
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiLoginIntoAccount(private val prevGui: GuiAltManager, val directLogin: Boolean = false) : GuiScreen() {

    private lateinit var addButton: GuiButton
    private lateinit var randomUsernameButton: GuiButton
    private lateinit var username: GuiTextField

    private var status = ""

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)

        // Clipboard login
        buttonList.add(GuiButton(2, width / 2 - 100, height / 2 - 60, "Random username").also { randomUsernameButton = it })

        // Login via Microsoft account
        buttonList.add(GuiButton(3, width / 2 - 100, height / 2 - 30, "Microsoft login"))

        // Add and back button
        buttonList.add(GuiButton(1, width / 2 - 100, height / 2, 98, 20, if (directLogin) "Login" else "Add")
            .also { addButton = it })
        buttonList.add(GuiButton(0, width / 2 + 2, height / 2, 98, 20, "Back"))

        username = GuiTextField(2, Fonts.font40, width / 2 - 100, height / 2 - 90, 200, 20)
        username.isFocused = false
        username.maxStringLength = 16
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        drawBackground(0)

        RenderUtils.drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)
        Fonts.font40.drawCenteredString(if (directLogin) "Direct Login" else "Add Account", width / 2.0f, height / 2 - 170f, 0xffffff)
        Fonts.font40.drawCenteredString("§7Cracked login", width / 2.0f, height / 2 - 110f, 0xffffff)
        Fonts.font35.drawCenteredString(status, width / 2.0f, height / 2 + 30f, 0xffffff)

        username.drawTextBox()

        if (username.text.isEmpty() && !username.isFocused)
            Fonts.font40.drawCenteredString("§7Username", width / 2 - 72f, height / 2 - 84f, 0xffffff)

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
                checkAndAddAccount(usernameText)
            }

            2 -> {
                username.text = RandomUtils.randomUsername()
            }

            3 -> {
                mc.displayGuiScreen(
                    GuiMicrosoftLoginProgress({
                        status = it
                    }, {
                        prevGui.status = status
                        mc.displayGuiScreen(prevGui)
                    })
                )
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

            Keyboard.KEY_RETURN -> {
                actionPerformed(addButton)
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
        if (usernameText.isEmpty()) {
            // what?
            return
        }

        val crackedAccount = CrackedAccount()
        crackedAccount.name = usernameText

        if (LiquidBounce.fileManager.accountsConfig.accountExists(crackedAccount)) {
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
            LiquidBounce.eventManager.callEvent(SessionEvent())
            status = "§aLogged into ${mc.session.username}."
        } else {
            LiquidBounce.fileManager.accountsConfig.addAccount(crackedAccount)
            LiquidBounce.fileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
            status = "§aThe account has been added."
        }

        prevGui.status = status
        mc.displayGuiScreen(prevGui)
    }
}
