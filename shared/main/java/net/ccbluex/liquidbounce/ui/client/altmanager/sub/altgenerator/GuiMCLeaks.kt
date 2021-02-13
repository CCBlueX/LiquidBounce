/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator

import com.thealtening.AltService
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
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
import org.lwjgl.input.Keyboard
import java.io.IOException

class GuiMCLeaks(private val prevGui: GuiAltManager) : WrappedGuiScreen()
{
	private lateinit var tokenField: IGuiTextField

	private var status = "\u00A77Idle..."

	override fun updateScreen() = tokenField.updateCursorCounter()

	override fun initGui()
	{
		Keyboard.enableRepeatEvents(true)
		if (MCLeaks.isAltActive()) status = "\u00A7aToken active. Using \u00A79${MCLeaks.getSession().username}\u00A7a to login!"

		// Add buttons
		representedScreen.buttonList.add(classProvider.createGuiButton(4, representedScreen.width / 2 - 100, representedScreen.height / 4 + 65, 98, 20, "Add Alt and Login"))
		representedScreen.buttonList.add(classProvider.createGuiButton(1, representedScreen.width / 2 + 2, representedScreen.height / 4 + 65, 98, 20, "Just Login"))

		representedScreen.buttonList.add(classProvider.createGuiButton(2, representedScreen.width / 2 - 100, representedScreen.height - 54, 98, 20, "Get Token"))
		representedScreen.buttonList.add(classProvider.createGuiButton(3, representedScreen.width / 2 + 2, representedScreen.height - 54, 98, 20, "Back"))

		// Token text field
		tokenField = classProvider.createGuiTextField(0, Fonts.font40, representedScreen.width / 2 - 100, representedScreen.height / 4 + 40, 200, 20)
		tokenField.isFocused = true
		tokenField.maxStringLength = 16
	}

	override fun onGuiClosed() = Keyboard.enableRepeatEvents(false)

	override fun actionPerformed(button: IGuiButton)
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
						if (LiquidBounce.fileManager.accountsConfig.accounts.any { acc: MinecraftAccount ->
								account.name.equals(acc.name, true) && account.accountName.equals(acc.accountName ?: "", true)
							}) moreMessage = " But the account has already been added."
						else
						{
							LiquidBounce.fileManager.accountsConfig.accounts.add(account)
							FileManager.saveConfig(LiquidBounce.fileManager.accountsConfig)
						}
						status = "\u00A7aYour token was redeemed successfully!\u00A7c$moreMessage"
					}
					else status = "\u00A7aYour token was redeemed successfully!"

					button.enabled = true
					button.displayString = "Login"

					prevGui.status = status
					mc.displayGuiScreen(prevGui.representedScreen)
				}
			}

			2 -> MiscUtils.showURL("https://mcleaks.net/")
			3 -> mc.displayGuiScreen(prevGui.representedScreen)
		}
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		when (keyCode)
		{
			Keyboard.KEY_ESCAPE -> mc.displayGuiScreen(prevGui.representedScreen)
			Keyboard.KEY_TAB -> tokenField.isFocused = !tokenField.isFocused
			Keyboard.KEY_RETURN, Keyboard.KEY_NUMPADENTER -> actionPerformed(representedScreen.buttonList[1])
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
		representedScreen.drawBackground(0)
		RenderUtils.drawRect(30.0f, 30.0f, representedScreen.width - 30.0f, representedScreen.height - 30.0f, Int.MIN_VALUE)

		// Draw text
		Fonts.font40.drawCenteredString("MCLeaks", representedScreen.width / 2.0f, 6.0f, 0xffffff)
		Fonts.font40.drawString("Token:", representedScreen.width / 2.0f - 100, representedScreen.height / 4.0f + 30, 10526880)

		// Draw status
		val status = status

		Fonts.font40.drawCenteredString(status, representedScreen.width / 2.0f, 18.0f, 0xffffff)

		tokenField.drawTextBox()
		super.drawScreen(mouseX, mouseY, partialTicks)
	}
}
