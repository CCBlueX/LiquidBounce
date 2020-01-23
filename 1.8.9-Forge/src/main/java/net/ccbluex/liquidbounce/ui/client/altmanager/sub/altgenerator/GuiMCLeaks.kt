/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator

import com.thealtening.AltService
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.mcleaks.MCLeaks
import net.mcleaks.RedeemResponse
import net.mcleaks.Session
import net.minecraft.client.gui.Gui
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiMCLeaks(private val prevGui: GuiAltManager) : GuiScreen() {
    private lateinit var tokenField: GuiTextField
    private var status: String? = null

    override fun updateScreen() = tokenField.updateCursorCounter()

    override fun initGui() {
        Keyboard.enableRepeatEvents(true)
        if (MCLeaks.isAltActive()) status = "§aToken active. Using §9${MCLeaks.getSession().username}§a to login!"

        // Add buttons
        buttonList.add(GuiButton(1, width / 2 - 100, height / 4 + 65, 200, 20, "Login"))
        buttonList.add(GuiButton(2, width / 2 - 100, height - 54, 98, 20, "Get Token"))
        buttonList.add(GuiButton(3, width / 2 + 2, height - 54, 98, 20, "Back"))

        // Token text field
        tokenField = GuiTextField(0, Fonts.font40, width / 2 - 100, height / 4 + 40, 200, 20)
        tokenField.isFocused = true
        tokenField.maxStringLength = 16
    }

    override fun onGuiClosed() = Keyboard.enableRepeatEvents(false)

    override fun actionPerformed(button: GuiButton) {
        if (!button.enabled) return

        when (button.id) {
            1 -> {
                if (tokenField.text.length != 16) {
                    status = "§cThe token has to be 16 characters long!"
                    return
                }

                button.enabled = false
                button.displayString = "Please wait ..."

                MCLeaks.redeem(tokenField.text) {
                    if (it is String) {
                        status = "§c$it"
                        button.enabled = true
                        button.displayString = "Login"
                        return@redeem
                    }

                    val redeemResponse = it as RedeemResponse
                    MCLeaks.refresh(Session(redeemResponse.username, redeemResponse.token))

                    try {
                        GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)
                    } catch (e: Exception) {
                        ClientUtils.getLogger().error("Failed to change alt service to Mojang.", e)
                    }

                    status = "§aYour token was redeemed successfully!"
                    button.enabled = true
                    button.displayString = "Login"

                    prevGui.status = status
                    mc.displayGuiScreen(prevGui)
                }
            }
            2 -> MiscUtils.showURL("https://mcleaks.net/")
            3 -> mc.displayGuiScreen(prevGui)
        }
    }

    override fun keyTyped(typedChar: Char, keyCode: Int) {
        when (keyCode) {
            Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui)
            Keyboard.KEY_TAB -> tokenField.isFocused = !tokenField.isFocused
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> actionPerformed(buttonList[1])
            else -> tokenField.textboxKeyTyped(typedChar, keyCode)
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int) {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        tokenField.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float) {
        // Draw background
        drawBackground(0)
        Gui.drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)

        // Draw text
        drawCenteredString(Fonts.font40, "MCLeaks", width / 2, 6, 0xffffff)
        drawString(Fonts.font40, "Token:", width / 2 - 100, height / 4 + 30, 10526880)

        // Draw status
        if (status != null) drawCenteredString(Fonts.font40, status, width / 2, 18, 0xffffff)

        tokenField.drawTextBox()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }
}