/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator

import com.thealtening.AltService
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.mcleaks.MCLeaks
import net.mcleaks.RedeemResponse
import net.mcleaks.Session
import net.minecraft.client.gui.GuiButton
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.gui.GuiTextField
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiMCLeaks(private val prevGui: GuiAltManager) : GuiScreen()
{
    private lateinit var tokenField: GuiTextField

    private var status = "\u00A77Idle..."

    override fun updateScreen() = tokenField.updateCursorCounter()

    override fun initGui()
    {
        val screen = this
        val height = screen.height

        Keyboard.enableRepeatEvents(true)
        if (MCLeaks.isAltActive) status = "\u00A7aToken active. Using \u00A79${MCLeaks.session?.username}\u00A7a to login!"

        // Add buttons
        val middleScreen = screen.width shr 1
        val quarterScreen = height shr 2

        val buttonList = screen.buttonList
        buttonList.add(GuiButton(4, middleScreen - 100, quarterScreen + 65, 98, 20, "Add Alt and Login"))
        buttonList.add(GuiButton(1, middleScreen + 2, quarterScreen + 65, 98, 20, "Just Login"))
        buttonList.add(GuiButton(2, middleScreen - 100, height - 54, 98, 20, "Get Token"))
        buttonList.add(GuiButton(3, middleScreen + 2, height - 54, 98, 20, "Back"))

        // Token text field
        tokenField = GuiTextField(0, Fonts.font40, middleScreen - 100, quarterScreen + 40, 200, 20).apply {
            isFocused = true
            maxStringLength = 16
        }
    }

    override fun onGuiClosed() = Keyboard.enableRepeatEvents(false)

    override fun actionPerformed(button: GuiButton)
    {
        if (!button.enabled) return

        when (button.id)
        {
            1, 4 ->
            {
                if (tokenField.text.length != 16)
                {
                    status = "\u00A7cThe token has to be 16 characters long!"
                    return
                }

                button.enabled = false
                button.displayString = "Please wait ..."

                val account = MinecraftAccount(MinecraftAccount.AltServiceType.MCLEAKS, tokenField.text, LiquidBounce.CLIENT_NAME)

                MCLeaks.redeem(tokenField.text) {
                    if (it is String)
                    {
                        status = "\u00A7c$it"
                        button.enabled = true
                        button.displayString = "Login"
                        return@redeem
                    }

                    val redeemResponse = it as RedeemResponse
                    MCLeaks.refresh(Session(redeemResponse.username, redeemResponse.token))
                    account.accountName = redeemResponse.username
                    try
                    {
                        GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)
                    }
                    catch (e: Exception)
                    {
                        ClientUtils.logger.error("Failed to change alt service to Mojang.", e)
                    }

                    if (button.id == 4)
                    {
                        var moreMessage = ""
                        val mcleaksAccountName = account.name
                        val mcleaksAccountNickName = account.accountName
                        if (LiquidBounce.fileManager.accountsConfig.accounts.filter { acc -> mcleaksAccountName.equals(acc.name, true) }.any { acc -> mcleaksAccountNickName.equals(acc.accountName ?: "", true) }) moreMessage = " But the account has already been added."
                        else
                        {
                            LiquidBounce.fileManager.accountsConfig.accounts.add(account)
                            FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
                            moreMessage = " Account added to the list."
                        }
                        status = "\u00A7aYour token was redeemed successfully!\u00A7c$moreMessage"
                    }
                    else status = "\u00A7aYour token was redeemed successfully!"

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

    override fun keyTyped(typedChar: Char, keyCode: Int)
    {
        when (keyCode)
        {
            Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui)
            Keyboard.KEY_TAB -> tokenField.isFocused = !tokenField.isFocused
            Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> actionPerformed(buttonList[1])
            else -> tokenField.textboxKeyTyped(typedChar, keyCode)
        }
    }

    @Throws(IOException::class)
    override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
    {
        super.mouseClicked(mouseX, mouseY, mouseButton)
        tokenField.mouseClicked(mouseX, mouseY, mouseButton)
    }

    override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
    { // Draw background
        drawBackground(0)
        RenderUtils.drawRect(30.0f, 30.0f, width - 30.0f, height - 30.0f, Int.MIN_VALUE)

        // Draw text
        val middleScreen = (width shr 1).toFloat()
        Fonts.font40.drawCenteredString("MCLeaks", middleScreen, 6.0f, 0xffffff)
        Fonts.font40.drawString("Token:", middleScreen - 100, (height shr 2) + 30f, 10526880)

        // Draw status
        val status = status

        Fonts.font40.drawCenteredString(status, middleScreen, 18.0f, 0xffffff)

        tokenField.drawTextBox()
        super.drawScreen(mouseX, mouseY, partialTicks)
    }
}
