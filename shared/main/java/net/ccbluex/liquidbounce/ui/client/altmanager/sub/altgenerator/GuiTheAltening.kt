/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator

import com.mojang.authlib.Agent.MINECRAFT
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.mojang.authlib.yggdrasil.YggdrasilUserAuthentication
import com.thealtening.AltService
import com.thealtening.api.TheAltening
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.event.SessionEvent
import net.ccbluex.liquidbounce.file.FileManager
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils
import net.ccbluex.liquidbounce.utils.WorkerUtils
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.misc.MiscUtils
import net.ccbluex.liquidbounce.utils.render.RenderUtils
import net.mcleaks.MCLeaks
import org.lwjgl.input.Keyboard
import java.net.Proxy.NO_PROXY

class GuiTheAltening(private val prevGui: GuiAltManager) : WrappedGuiScreen()
{

	// Data Storage
	companion object
	{
		var apiKey: String = ""
	}

	// Buttons
	private lateinit var loginButton: IGuiButton
	private lateinit var generateButton: IGuiButton
	private lateinit var addAltAndLoginButton: IGuiButton

	// User Input Fields
	private lateinit var apiKeyField: IGuiTextField
	private lateinit var tokenField: IGuiTextField

	// Status
	private var status = "\u00A77Idle..."

	/**
	 * Initialize The Altening Generator GUI
	 */
	override fun initGui()
	{ // Enable keyboard repeat events
		Keyboard.enableRepeatEvents(true)

		// Login button & Add to alt list and Login button
		addAltAndLoginButton = classProvider.createGuiButton(4, representedScreen.width / 2 - 100, 75, 98, 20, "Add Alt and Login")
		representedScreen.buttonList.add(addAltAndLoginButton)
		loginButton = classProvider.createGuiButton(2, representedScreen.width / 2 + 2, 75, 98, 20, "Just Login")
		representedScreen.buttonList.add(loginButton)

		// Generate button
		generateButton = classProvider.createGuiButton(1, representedScreen.width / 2 - 100, 140, "Generate")
		representedScreen.buttonList.add(generateButton)

		// Buy & Back buttons
		representedScreen.buttonList.add(classProvider.createGuiButton(3, representedScreen.width / 2 - 100, representedScreen.height - 54, 98, 20, "Buy"))
		representedScreen.buttonList.add(classProvider.createGuiButton(0, representedScreen.width / 2 + 2, representedScreen.height - 54, 98, 20, "Back"))

		// Token text field
		tokenField = classProvider.createGuiTextField(666, Fonts.font40, representedScreen.width / 2 - 100, 50, 200, 20)
		tokenField.isFocused = true
		tokenField.maxStringLength = Integer.MAX_VALUE

		// Api key password field
		apiKeyField = classProvider.createGuiPasswordField(1337, Fonts.font40, representedScreen.width / 2 - 100, 115, 200, 20)
		apiKeyField.maxStringLength = 18
		apiKeyField.text = apiKey
		super.initGui()
	}

	/**
	 * Draw screen
	 */
	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{ // Draw background to screen
		representedScreen.drawBackground(0)
		RenderUtils.drawRect(30.0f, 30.0f, representedScreen.width - 30.0f, representedScreen.height - 30.0f, Integer.MIN_VALUE)

		// Draw title and status
		Fonts.font35.drawCenteredString("TheAltening", representedScreen.width / 2.0f, 6.0f, 0xffffff)
		Fonts.font35.drawCenteredString(status, representedScreen.width / 2.0f, 18.0f, 0xffffff)

		// Draw fields
		apiKeyField.drawTextBox()
		tokenField.drawTextBox()

		// Draw text
		Fonts.font40.drawCenteredString("\u00A77Token:", representedScreen.width / 2.0f - 84, 40.0f, 0xffffff)
		Fonts.font40.drawCenteredString("\u00A77API-Key:", representedScreen.width / 2.0f - 78, 105.0f, 0xffffff)
		Fonts.font40.drawCenteredString("\u00A77Use coupon code 'liquidbounce' for 20% off!", representedScreen.width / 2.0f, representedScreen.height - 65.0f, 0xffffff)
		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	/**
	 * Handle button actions
	 */
	override fun actionPerformed(button: IGuiButton)
	{
		if (!button.enabled) return

		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui.representedScreen)

			1 ->
			{
				loginButton.enabled = false
				generateButton.enabled = false
				apiKey = apiKeyField.text

				val altening = TheAltening(apiKey)
				val asynchronous = TheAltening.Asynchronous(altening)
				status = "\u00A7cGenerating account..."

				asynchronous.accountData.thenAccept { account ->
					status = "\u00A7aGenerated account: \u00A7b\u00A7l${account.username}"

					try
					{
						status = "\u00A7cSwitching Alt Service..."

						// Change Alt Service
						GuiAltManager.altService.switchService(AltService.EnumAltService.THEALTENING)

						status = "\u00A7cLogging in..."

						// Set token as username
						val yggdrasilUserAuthentication = YggdrasilUserAuthentication(YggdrasilAuthenticationService(NO_PROXY, ""), MINECRAFT)
						yggdrasilUserAuthentication.setUsername(account.token)
						yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME)

						status = try
						{
							yggdrasilUserAuthentication.logIn()

							mc.session = classProvider.createSession(yggdrasilUserAuthentication.selectedProfile.name, yggdrasilUserAuthentication.selectedProfile.id.toString(), yggdrasilUserAuthentication.authenticatedToken, "mojang")
							LiquidBounce.eventManager.callEvent(SessionEvent())
							MCLeaks.remove()

							prevGui.status = "\u00A7aYour name is now \u00A7b\u00A7l${yggdrasilUserAuthentication.selectedProfile.name}\u00A7c."
							mc.displayGuiScreen(prevGui.representedScreen)
							""
						}
						catch (e: AuthenticationException)
						{
							GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)

							ClientUtils.logger.error("Failed to login.", e)
							"\u00A7cFailed to login: ${e.message}"
						}
					}
					catch (throwable: Throwable)
					{
						status = "\u00A7cFailed to login. Unknown error."
						ClientUtils.logger.error("Failed to login.", throwable)
					}

					loginButton.enabled = true
					generateButton.enabled = true
				}.handle { _, err ->
					status = "\u00A7cFailed to generate account."
					ClientUtils.logger.error("Failed to generate account.", err)
				}.whenComplete { _, _ ->
					loginButton.enabled = true
					generateButton.enabled = true
				}
			}

			2, 4 ->
			{
				loginButton.enabled = false
				generateButton.enabled = false

				val account = MinecraftAccount(MinecraftAccount.AltServiceType.THEALTENING, tokenField.text, LiquidBounce.CLIENT_NAME)

				WorkerUtils.workers.submit {
					try
					{
						status = "\u00A7cSwitching Alt Service..."

						// Change Alt Service
						GuiAltManager.altService.switchService(AltService.EnumAltService.THEALTENING)
						status = "\u00A7cLogging in..."

						// Set token as username
						val yggdrasilUserAuthentication = YggdrasilUserAuthentication(YggdrasilAuthenticationService(NO_PROXY, ""), MINECRAFT)
						yggdrasilUserAuthentication.setUsername(tokenField.text)
						yggdrasilUserAuthentication.setPassword(LiquidBounce.CLIENT_NAME)

						status = try
						{
							yggdrasilUserAuthentication.logIn()

							account.accountName = yggdrasilUserAuthentication.selectedProfile.name

							mc.session = classProvider.createSession(yggdrasilUserAuthentication.selectedProfile.name, yggdrasilUserAuthentication.selectedProfile.id.toString(), yggdrasilUserAuthentication.authenticatedToken, "mojang")
							LiquidBounce.eventManager.callEvent(SessionEvent())
							MCLeaks.remove()

							prevGui.status = "\u00A7aYour name is now \u00A7b\u00A7l${yggdrasilUserAuthentication.selectedProfile.name}\u00A7c."
							mc.displayGuiScreen(prevGui.representedScreen)

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

								"\u00A7aYour name is now \u00A7b\u00A7l${yggdrasilUserAuthentication.selectedProfile.name}\u00A7c.$moreMessage"
							}
							else "\u00A7aYour name is now \u00A7b\u00A7l${yggdrasilUserAuthentication.selectedProfile.name}\u00A7c."
						}
						catch (e: AuthenticationException)
						{
							GuiAltManager.altService.switchService(AltService.EnumAltService.MOJANG)

							ClientUtils.logger.error("Failed to login.", e)
							"\u00A7cFailed to login: ${e.message}"
						}
					}
					catch (throwable: Throwable)
					{
						ClientUtils.logger.error("Failed to login.", throwable)
						status = "\u00A7cFailed to login. Unknown error."
					}

					loginButton.enabled = true
					generateButton.enabled = true
				}
			}

			3 -> MiscUtils.showURL("https://thealtening.com/?ref=liquidbounce")
		}
	}

	/**
	 * Handle key typed
	 */
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{ // Check if user want to escape from screen
		if (Keyboard.KEY_ESCAPE == keyCode)
		{ // Send back to prev screen
			mc.displayGuiScreen(prevGui.representedScreen)
			return
		}

		// Check if field is focused, then call key typed
		if (apiKeyField.isFocused) apiKeyField.textboxKeyTyped(typedChar, keyCode)
		if (tokenField.isFocused) tokenField.textboxKeyTyped(typedChar, keyCode)
		super.keyTyped(typedChar, keyCode)
	}

	/**
	 * Handle mouse clicked
	 */
	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{ // Call mouse clicked to field
		apiKeyField.mouseClicked(mouseX, mouseY, mouseButton)
		tokenField.mouseClicked(mouseX, mouseY, mouseButton)
		super.mouseClicked(mouseX, mouseY, mouseButton)
	}

	/**
	 * Handle screen update
	 */
	override fun updateScreen()
	{
		apiKeyField.updateCursorCounter()
		tokenField.updateCursorCounter()
		super.updateScreen()
	}

	/**
	 * Handle gui closed
	 */
	override fun onGuiClosed()
	{ // Disable keyboard repeat events
		Keyboard.enableRepeatEvents(false)

		// Set API key
		apiKey = apiKeyField.text
		super.onGuiClosed()
	}
}
