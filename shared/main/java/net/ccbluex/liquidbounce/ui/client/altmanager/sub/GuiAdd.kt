/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import com.mojang.authlib.Agent
import com.mojang.authlib.exceptions.AuthenticationException
import com.mojang.authlib.yggdrasil.YggdrasilAuthenticationService
import com.thealtening.AltService.EnumAltService
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.TabUtils.tab
import net.ccbluex.liquidbounce.utils.WorkerUtils.workers
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import org.lwjgl.input.Keyboard
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.UnsupportedFlavorException
import java.io.IOException
import java.net.Proxy

class GuiAdd(private val prevGui: GuiAltManager) : WrappedGuiScreen()
{
	private lateinit var addButton: IGuiButton
	private lateinit var clipboardButton: IGuiButton
	private lateinit var username: IGuiTextField
	private lateinit var password: IGuiTextField

	private var status = "\u00A77Idle..."

	override fun initGui()
	{
		Keyboard.enableRepeatEvents(true)

		val provider = classProvider
		val screen = representedScreen
		val buttonX = (screen.width shr 1) - 100
		val quarterScreen = screen.height shr 2

		val buttonList = screen.buttonList
		buttonList.add(provider.createGuiButton(1, buttonX, quarterScreen + 72, "Add").also { addButton = it })
		buttonList.add(provider.createGuiButton(2, buttonX, quarterScreen + 96, "Clipboard").also { clipboardButton = it })
		buttonList.add(provider.createGuiButton(0, buttonX, quarterScreen + 120, "Back"))

		username = provider.createGuiTextField(2, Fonts.font40, buttonX, 60, 200, 20).apply {
			isFocused = true
			maxStringLength = Int.MAX_VALUE
		}

		password = provider.createGuiPasswordField(3, Fonts.font40, buttonX, 85, 200, 20).apply { maxStringLength = Int.MAX_VALUE }
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		val screen = representedScreen
		screen.drawBackground(0)

		drawRect(30, 30, screen.width - 30, screen.height - 30, Int.MIN_VALUE)

		val middleScreen = (screen.width shr 1).toFloat()

		Fonts.font40.drawCenteredString("Add Account", middleScreen, 34f, 0xffffff)
		Fonts.font35.drawCenteredString(status, middleScreen, (screen.height shr 2) + 60f, 0xffffff)

		val username = username
		val password = password

		username.drawTextBox()
		password.drawTextBox()

		if (username.text.isEmpty() && !username.isFocused) Fonts.font40.drawCenteredString("\u00A77Username / E-Mail", middleScreen - 55f, 66f, 0xffffff)
		if (password.text.isEmpty() && !password.isFocused) Fonts.font40.drawCenteredString("\u00A77Password", middleScreen - 74f, 91f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun actionPerformed(button: IGuiButton)
	{
		if (!button.enabled) return

		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui.representedScreen)

			1 ->
			{
				val name = username.text

				if (LiquidBounce.fileManager.accountsConfig.isAccountExists(name))
				{
					status = "\u00A7cThe account has already been added."
					return
				}

				addAccount(name, password.text)
			}

			2 -> try
			{
				val clipboardData = Toolkit.getDefaultToolkit().systemClipboard.getData(DataFlavor.stringFlavor) as String
				val accountData = clipboardData.split(":", ignoreCase = true, limit = 2)

				if (!clipboardData.contains(":") || accountData.size != 2)
				{
					status = "\u00A7cInvalid clipboard data. (Use: E-Mail:Password)"
					return
				}

				addAccount(accountData[0], accountData[1])
			}
			catch (e: UnsupportedFlavorException)
			{
				status = "\u00A7cClipboard flavor unsupported!"
				logger.error("Failed to read data from clipboard.", e)
			}
		}
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		val username = username
		val password = password

		when (keyCode)
		{
			Keyboard.KEY_ESCAPE ->
			{
				mc.displayGuiScreen(prevGui.representedScreen)
				return
			}

			Keyboard.KEY_TAB ->
			{
				tab(username, password)
				return
			}

			Keyboard.KEY_RETURN ->
			{
				actionPerformed(addButton)
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
	}

	private fun addAccount(name: String, password: String)
	{
		if (LiquidBounce.fileManager.accountsConfig.isAccountExists(name))
		{
			status = "\u00A7cThe account has already been added."
			return
		}

		addButton.enabled = false
		clipboardButton.enabled = false

		val account = MinecraftAccount(AltServiceType.MOJANG, name, password.ifEmpty { null })

		workers.execute {
			if (!account.isCracked)
			{
				status = "\u00A7aChecking..."

				try
				{
					val oldService = GuiAltManager.altService.currentService

					if (oldService != EnumAltService.MOJANG) GuiAltManager.altService.switchService(EnumAltService.MOJANG)

					val userAuthentication = YggdrasilAuthenticationService(Proxy.NO_PROXY, "").createUserAuthentication(Agent.MINECRAFT)
					userAuthentication.setUsername(account.name)
					userAuthentication.setPassword(account.password)
					userAuthentication.logIn()

					account.accountName = userAuthentication.selectedProfile.name

					if (oldService == EnumAltService.THEALTENING) GuiAltManager.altService.switchService(EnumAltService.THEALTENING)
				}
				catch (e: NullPointerException)
				{
					status = "\u00A7cThe account doesn't work. (NPE)"
					addButton.enabled = true
					clipboardButton.enabled = true
					logger.warn("The account \"${account.name}:${account.password}\" not working.", e)

					return@execute
				}
				catch (e: AuthenticationException)
				{
					status = "\u00A7cThe account doesn't work. (Authentication failed)"
					addButton.enabled = true
					clipboardButton.enabled = true
					logger.warn("The account \"${account.name}:${account.password}\" not working.", e)

					return@execute
				}
				catch (e: NoSuchFieldException)
				{
					status = "\u00A7cThe account doesn't work. (Reflection API failed)"
					addButton.enabled = true
					clipboardButton.enabled = true
					logger.warn("The account \"${account.name}:${account.password}\" not working.", e)

					return@execute
				}
				catch (e: IllegalAccessException)
				{
					status = "\u00A7cThe account doesn't work. (Reflection API failed)"
					addButton.enabled = true
					clipboardButton.enabled = true
					logger.warn("The account \"${account.name}:${account.password}\" not working.", e)

					return@execute
				}
			}

			LiquidBounce.fileManager.accountsConfig.accounts.add(account)
			saveConfig(LiquidBounce.fileManager.accountsConfig)

			status = "\u00A7aThe account has been added."

			prevGui.status = status

			mc.displayGuiScreen(prevGui.representedScreen)
		}
	}
}
