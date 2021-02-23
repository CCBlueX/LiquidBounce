/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager

import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.thealtening.AltService
import com.thealtening.AltService.EnumAltService
import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiScreen
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.*
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator.GuiMCLeaks
import net.ccbluex.liquidbounce.ui.client.altmanager.sub.altgenerator.GuiTheAltening
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.ClientUtils.logger
import net.ccbluex.liquidbounce.utils.ServerUtils.connectToLastServer
import net.ccbluex.liquidbounce.utils.WorkerUtils.workers
import net.ccbluex.liquidbounce.utils.login.LoginUtils.LoginResult
import net.ccbluex.liquidbounce.utils.login.LoginUtils.login
import net.ccbluex.liquidbounce.utils.login.LoginUtils.loginCracked
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount.AltServiceType
import net.ccbluex.liquidbounce.utils.login.UserUtils.isValidTokenOffline
import net.ccbluex.liquidbounce.utils.misc.HttpUtils
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.createBufferedFileReader
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.createBufferedFileWriter
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.openFileChooser
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.saveFileChooser
import net.ccbluex.liquidbounce.utils.misc.MiscUtils.showErrorPopup
import net.mcleaks.MCLeaks
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection
import java.io.IOException
import java.util.*
import java.util.function.Consumer
import javax.swing.JOptionPane

class GuiAltManager(private val prevGui: IGuiScreen) : WrappedGuiScreen()
{
	@JvmField
	var status = "\u00A77Idle..."

	lateinit var loginButton: IGuiButton
	lateinit var randomButton: IGuiButton
	lateinit var altsList: GuiAltsList

	private lateinit var searchField: IGuiTextField

	override fun initGui()
	{
		val provider = classProvider
		val screen = representedScreen
		val width = screen.width
		val height = screen.height

		val textFieldWidth = (width shr 3).coerceAtLeast(70)

		searchField = provider.createGuiTextField(2, Fonts.font40, width - textFieldWidth - 10, 10, textFieldWidth, 20)
		searchField.maxStringLength = Int.MAX_VALUE

		altsList = GuiAltsList(screen)
		altsList.represented.registerScrollButtons(7, 8)

		var index = -1

		val accountsConfig = LiquidBounce.fileManager.accountsConfig

		// Find the current logged-on account and automatically select it
		accountsConfig.accounts.indices.firstOrNull { i ->
			val minecraftAccount = accountsConfig.accounts[i]

			minecraftAccount.password.isNullOrEmpty() && minecraftAccount.name == mc.session.username || minecraftAccount.accountName != null && minecraftAccount.accountName == mc.session.username
		}?.let { index = it }

		altsList.elementClicked(index, false, 0, 0)
		altsList.represented.scrollBy(index * altsList.represented.slotHeight)

		val buttonScreenRightPos = width - 80
		val buttonList = screen.buttonList

		buttonList.add(provider.createGuiButton(0, buttonScreenRightPos, height - 65, 70, 20, "Back"))

		buttonList.add(provider.createGuiButton(1, buttonScreenRightPos, 46, 70, 20, "Add"))
		buttonList.add(provider.createGuiButton(2, buttonScreenRightPos, 70, 70, 20, "Remove"))
		buttonList.add(provider.createGuiButton(8, buttonScreenRightPos, 94, 70, 20, "Copy"))
		buttonList.add(provider.createGuiButton(7, buttonScreenRightPos, 118, 70, 20, "Import"))
		buttonList.add(provider.createGuiButton(12, buttonScreenRightPos, 142, 70, 20, "Export"))
		buttonList.add(provider.createGuiButton(13, buttonScreenRightPos, 166, 70, 20, "Banned Servers"))
		buttonList.add(provider.createGuiButton(14, buttonScreenRightPos, 190, 70, 20, "Copy Current Session Into Clipboard"))
		buttonList.add(provider.createGuiButton(15, buttonScreenRightPos, 214, 70, 20, "Current Session Info"))


		buttonList.add(provider.createGuiButton(3, 5, 46, 90, 20, "Login").also { loginButton = it })
		buttonList.add(provider.createGuiButton(4, 5, 70, 90, 20, "Random").also { randomButton = it })
		buttonList.add(provider.createGuiButton(6, 5, 94, 90, 20, "Direct Login"))
		buttonList.add(provider.createGuiButton(88, 5, 118, 90, 20, "Change Name"))
		buttonList.add(provider.createGuiButton(10, 5, 147, 90, 20, "Session Login"))
		buttonList.add(provider.createGuiButton(11, 5, 171, 90, 20, "Cape"))
		buttonList.add(provider.createGuiButton(99, 5, 195, 90, 20, "Reconnect to last server"))

		if (GENERATORS.getOrDefault("mcleaks", true)) buttonList.add(provider.createGuiButton(5, 5, 230, 90, 20, "MCLeaks"))
		if (GENERATORS.getOrDefault("thealtening", true)) buttonList.add(provider.createGuiButton(9, 5, 255, 90, 20, "TheAltening"))
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		val width = representedScreen.width

		representedScreen.drawBackground(0)
		altsList.represented.drawScreen(mouseX, mouseY, partialTicks)

		Fonts.font40.drawCenteredString("AltManager", (width shr 1).toFloat(), 6f, 0xffffff)
		Fonts.font35.drawCenteredString(if (searchField.text.isEmpty()) "${LiquidBounce.fileManager.accountsConfig.accounts.size} Alts" else "${altsList.accounts.size} Search Results", (width shr 1).toFloat(), 18f, 0xffffff)
		Fonts.font35.drawCenteredString(status, (width shr 1).toFloat(), 32f, 0xffffff)

		val mcleaksActive = MCLeaks.isAltActive
		val sessionUsername = if (mcleaksActive) MCLeaks.session?.username else mc.session.username
		val altServiceTypeText = when
		{
			altService.currentService == EnumAltService.THEALTENING -> "\u00A7aTheAltening"
			mcleaksActive -> "\u00A7bMCLeaks"
			isValidTokenOffline(mc.session.token) -> "\u00A76Mojang"
			else -> "\u00A78Cracked"
		}

		Fonts.font35.drawStringWithShadow("\u00A77User: \u00A7a$sessionUsername", 6, 6, 0xffffff)
		Fonts.font35.drawStringWithShadow("\u00A77Type: \u00A7a$altServiceTypeText", 6, 15, 0xffffff)

		searchField.drawTextBox()
		if (searchField.text.isEmpty() && !searchField.isFocused) Fonts.font40.drawStringWithShadow("\u00A77Search...", searchField.xPosition + 4, 17, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun actionPerformed(button: IGuiButton)
	{
		if (!button.enabled) return

		val provider = classProvider

		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui)

			1 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiAdd(this)))

			2 -> if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.getSize())
			{
				LiquidBounce.fileManager.accountsConfig.removeAccount(altsList.accounts[altsList.selectedSlot])
				saveConfig(LiquidBounce.fileManager.accountsConfig)
				status = "\u00A7aThe account has been removed."
				altsList.updateAccounts(searchField.text)
			}
			else status = "\u00A7cSelect an account."

			3 -> if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.getSize())
			{
				loginButton.enabled = false
				randomButton.enabled = false
				workers.execute {
					try
					{
						val minecraftAccount = altsList.accounts[altsList.selectedSlot]
						status = "\u00A7aLogging in..."
						status = login(minecraftAccount)
					}
					finally
					{
						loginButton.enabled = true
						randomButton.enabled = true
					}
				}
			}
			else status = "\u00A7cSelect an account."

			4 ->
			{
				if (altsList.accounts.isEmpty())
				{
					status = "\u00A7cThe list is empty."
					return
				}
				val randomInteger = Random().nextInt(altsList.accounts.size)
				if (randomInteger < altsList.getSize()) altsList.selectedSlot = randomInteger
				loginButton.enabled = false
				randomButton.enabled = false
				workers.execute {
					try
					{
						val minecraftAccount = altsList.accounts[randomInteger]
						status = "\u00A7aLogging in..."
						status = login(minecraftAccount)
					}
					finally
					{
						loginButton.enabled = true
						randomButton.enabled = true
					}
				}
			}

			5 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiMCLeaks(this)))

			6 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiDirectLogin(this)))

			7 ->
			{
				val file = openFileChooser() ?: return
				val bufferedReader = createBufferedFileReader(file)
				var line: String
				while (bufferedReader.readLine().also { line = it } != null)
				{
					val accountData = line.split(":", ignoreCase = true, limit = 2)
					if (!LiquidBounce.fileManager.accountsConfig.isAccountExists(accountData[0])) LiquidBounce.fileManager.accountsConfig.addAccount(if (accountData.size > 1) MinecraftAccount(AltServiceType.MOJANG, accountData[0], accountData[1]) else MinecraftAccount(AltServiceType.MOJANG, accountData[0]))
				}
				bufferedReader.close()
				altsList.updateAccounts(searchField.text)
				saveConfig(LiquidBounce.fileManager.accountsConfig)
				status = "\u00A7aThe accounts were imported successfully."
			}

			8 -> status = if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.getSize())
			{
				val minecraftAccount = altsList.accounts[altsList.selectedSlot]
				Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(minecraftAccount.name + ":" + minecraftAccount.password), null)
				"\u00A7aCopied account into your clipboard."
			}
			else "\u00A7cSelect an account."

			88 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiChangeName(this)))

			9 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiTheAltening(this)))

			10 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiSessionLogin(representedScreen)))

			11 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiDonatorCape(this)))

			12 ->
			{
				if (LiquidBounce.fileManager.accountsConfig.accounts.isEmpty())
				{
					status = "\u00A7cThe list is empty."
					return
				}
				val selectedFile = saveFileChooser()
				if (selectedFile == null || selectedFile.isDirectory) return
				try
				{
					if (!selectedFile.exists()) selectedFile.createNewFile()
					val fileWriter = createBufferedFileWriter(selectedFile)
					for (account in LiquidBounce.fileManager.accountsConfig.accounts) fileWriter.write(if (account.isCracked) account.name + System.lineSeparator() else account.name + ":" + account.password + System.lineSeparator())
					fileWriter.flush()
					fileWriter.close()
					JOptionPane.showMessageDialog(null, "Exported successfully!", "AltManager", JOptionPane.INFORMATION_MESSAGE)
				}
				catch (e: Exception)
				{
					logger.error("Can't export accounts in AltManager", e)
					showErrorPopup("Error", """
 	Exception class: ${e.javaClass.name}
 	Message: ${e.message}
 	""".trimIndent())
				}
			}

			99 -> connectToLastServer()

			13 -> if (altsList.selectedSlot != -1 && altsList.selectedSlot < altsList.getSize()) mc.displayGuiScreen(provider.wrapGuiScreen(GuiBannedServers(representedScreen, LiquidBounce.fileManager.accountsConfig.accounts[altsList.selectedSlot]))) else status = "\u00A7cSelect an account."

			14 ->
			{
				Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(mc.session.sessionID), null)
				status = "\u00A7aCopied current session id into your clipboard."
			}

			15 -> mc.displayGuiScreen(provider.wrapGuiScreen(GuiSessionInfo(representedScreen, null)))
		}
	}

	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		val list = altsList

		val search = searchField
		if (search.isFocused)
		{
			search.textboxKeyTyped(typedChar, keyCode)
			list.updateAccounts(search.text)
		}

		val screen = representedScreen
		val height = screen.height
		val selected = list.selectedSlot

		when (keyCode)
		{
			Keyboard.KEY_ESCAPE ->
			{
				mc.displayGuiScreen(prevGui)
				return
			}

			Keyboard.KEY_UP -> list.elementClicked((selected - 1).coerceAtLeast(0), false, 0, 0)
			Keyboard.KEY_DOWN -> list.elementClicked((selected + 1).coerceAtMost(list.getSize() - 1), false, 0, 0)
			Keyboard.KEY_RETURN -> list.elementClicked(selected, true, 0, 0)
			Keyboard.KEY_NEXT -> list.represented.scrollBy(height - 100)

			Keyboard.KEY_PRIOR ->
			{
				list.represented.scrollBy(-height + 100)
				return
			}
		}

		screen.superKeyTyped(typedChar, keyCode)
	}

	override fun handleMouseInput()
	{
		representedScreen.superHandleMouseInput()
		altsList.represented.handleMouseInput()
	}

	override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
	{
		searchField.mouseClicked(mouseX, mouseY, mouseButton)
		representedScreen.superMouseClicked(mouseX, mouseY, mouseButton)
	}

	override fun updateScreen()
	{
		searchField.updateCursorCounter()
	}

	inner class GuiAltsList internal constructor(prevGui: IGuiScreen) : WrappedGuiSlot(mc, prevGui.width, prevGui.height, 40, prevGui.height - 40, 30)
	{
		lateinit var accounts: MutableList<MinecraftAccount>
		var selectedSlot = 0
			get()
			{
				if (field > accounts.size) field = -1
				return field
			}
			internal set

		fun updateAccounts(search: String?)
		{
			val defaultValue = LiquidBounce.fileManager.accountsConfig.accounts

			if (search.isNullOrEmpty())
			{
				accounts = defaultValue
				return
			}

			accounts = ArrayList(defaultValue.size)

			LiquidBounce.fileManager.accountsConfig.accounts.asSequence().filter { it.name.contains(search, ignoreCase = true) || it.accountName != null && it.accountName!!.contains(search, ignoreCase = true) }.forEach { accounts.add(it) }
		}

		override fun isSelected(id: Int): Boolean = selectedSlot == id

		override fun getSize(): Int = accounts.size

		override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
		{
			selectedSlot = id

			if (doubleClick)
			{
				val selected = altsList.selectedSlot

				if (selected != -1 && selected < altsList.getSize() && loginButton.enabled)
				{
					loginButton.enabled = false
					randomButton.enabled = false

					workers.execute {
						val minecraftAccount = accounts[selected]
						status = "\u00A7aLogging in..."
						status = "\u00A7c" + login(minecraftAccount)
						loginButton.enabled = true
						randomButton.enabled = true
					}
				}
				else status = "\u00A7cSelect an account."
			}
		}

		override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
		{
			val width = represented.width
			val middleScreen = (width shr 1).toFloat()

			val account = accounts[id]
			val serviceType = account.serviceType
			val accName = account.accountName
			val cracked = account.isCracked

			// noinspection NegativelyNamedBooleanVariable
			val isInvalid = serviceType == AltServiceType.MOJANG_INVALID || serviceType == AltServiceType.MOJANG_MIGRATED || serviceType == AltServiceType.MCLEAKS_INVALID || serviceType == AltServiceType.THEALTENING_INVALID

			// Draw account name
			Fonts.font40.drawCenteredString((accName ?: account.name), middleScreen, y + 2f, Color.WHITE.rgb, true)

			val serviceTypeText = if (cracked) "Cracked" else serviceType.id
			val serviceTypeColor = when
			{
				cracked -> Color.GRAY // Cracked
				accName == null -> Color.LIGHT_GRAY // Unchecked
				isInvalid -> Color.RED // Premium (Invalid)
				else -> Color.GREEN // Premium
			}.rgb

			Fonts.font40.drawCenteredString(serviceTypeText, middleScreen, y + 10f, serviceTypeColor, true)

			if (account.bannedServers.isNotEmpty()) Fonts.font35.drawCenteredString("Banned on " + account.serializeBannedServers(), middleScreen, y + 20f, Color.RED.rgb, true)
		}

		override fun drawBackground()
		{
		}

		init
		{
			updateAccounts(null)
		}
	}

	companion object
	{
		@JvmField
		val altService = AltService()

		private val GENERATORS: MutableMap<String, Boolean> = HashMap(2)

		fun loadGenerators()
		{
			try
			{
				// Read versions json from cloud
				val jsonElement = JsonParser().parse(HttpUtils[LiquidBounce.CLIENT_CLOUD + "/generators.json"])

				// Check json is valid object
				if (jsonElement.isJsonObject)
				{
					// Get json object of element
					val jsonObject = jsonElement.asJsonObject
					jsonObject.entrySet().forEach(Consumer { stringJsonElementEntry: Map.Entry<String, JsonElement> -> GENERATORS[stringJsonElementEntry.key] = stringJsonElementEntry.value.asBoolean })
				}
			}
			catch (throwable: Throwable)
			{
				// Print throwable to console
				logger.error("Failed to load enabled generators.", throwable)
			}
		}

		@JvmStatic
		fun login(minecraftAccount: MinecraftAccount?): String
		{
			if (minecraftAccount == null) return ""

			if (AltServiceType.MOJANG.equals(minecraftAccount.serviceType) && altService.currentService != EnumAltService.MOJANG) try
			{
				altService.switchService(EnumAltService.MOJANG)
			}
			catch (e: NoSuchFieldException)
			{
				logger.error("Something went wrong while trying to switch alt service.", e)
			}
			catch (e: IllegalAccessException)
			{
				logger.error("Something went wrong while trying to switch alt service.", e)
			}

			// Cracked account (not premium) login
			if (minecraftAccount.isCracked)
			{
				loginCracked(minecraftAccount.name)
				MCLeaks.remove()
				return "\u00A7cYour name is now \u00A78" + minecraftAccount.name + "\u00A7c."
			}

			val saveConfig = { saveConfig(LiquidBounce.fileManager.accountsConfig) }

			return when (login(minecraftAccount.serviceType, minecraftAccount.name, minecraftAccount.password))
			{
				LoginResult.LOGGED_IN ->
				{
					if (!AltServiceType.MCLEAKS.equals(minecraftAccount.serviceType)) MCLeaks.remove()
					val userName = mc.session.username
					minecraftAccount.accountName = userName
					saveConfig()
					"\u00A7aYour name is now \u00A7b\u00A7l$userName\u00A7c."
				}

				LoginResult.AUTHENTICATION_FAILURE ->
				{
					"\u00A7cAuthentication failed. Please check e-mail and password."
				}

				LoginResult.AUTHENTICATION_UNAVAILABLE ->
				{
					"\u00A7cCannot contact authentication server."
				}

				LoginResult.INVALID_ACCOUNT_DATA ->
				{
					when (minecraftAccount.serviceType)
					{
						AltServiceType.MCLEAKS ->
						{
							minecraftAccount.serviceType = AltServiceType.MCLEAKS_INVALID
							saveConfig()
							"\u00A7cThe MCLeaks token has to be 16 characters long!"
						}

						AltServiceType.MCLEAKS_INVALID ->
						{
							saveConfig()
							"\u00A7cThe MCLeaks token has to be 16 characters long!"
						}

						else ->
						{
							minecraftAccount.serviceType = AltServiceType.MOJANG_INVALID
							saveConfig()
							"\u00A7cInvalid username or wrong password or the account is get mojang-banned."
						}
					}
				}

				LoginResult.MIGRATED ->
				{
					minecraftAccount.serviceType = AltServiceType.MOJANG_MIGRATED
					saveConfig()
					"\u00A7cAccount migrated."
				}

				LoginResult.MCLEAKS_INVALID ->
				{
					minecraftAccount.serviceType = AltServiceType.MCLEAKS_INVALID
					saveConfig()
					"\u00A7cMCLeaks token invalid or expired."
				}

				LoginResult.THEALTENING_INVALID ->
				{
					minecraftAccount.serviceType = AltServiceType.THEALTENING_INVALID
					saveConfig()
					"\u00A7cTheAltening token invalid or expired."
				}

				else -> ""
			}
		}

		@JvmStatic
		fun canEnableMarkBannedButton(): Boolean
		{
			val profileName = mc.session.profile.name
			return LiquidBounce.fileManager.accountsConfig.accounts.any { acc: MinecraftAccount -> profileName.equals(acc.name, ignoreCase = true) || profileName.equals(acc.accountName, ignoreCase = true) }
		}

		@JvmStatic
		fun canMarkBannedCurrent(serverIp: String?): Boolean
		{
			val profileName = mc.session.profile.name
			return serverIp == null || LiquidBounce.fileManager.accountsConfig.accounts.firstOrNull { profileName.equals(it.name, ignoreCase = true) || profileName.equals(it.accountName, ignoreCase = true) }?.let { !it.bannedServers.contains(serverIp) } ?: true
		}

		@JvmStatic
		fun toggleMarkBanned(serverIp: String)
		{
			val sessionProfile = mc.session.profile

			LiquidBounce.fileManager.accountsConfig.accounts.asSequence().filter { sessionProfile.name.equals(it.name, ignoreCase = true) || sessionProfile.name.equals(it.accountName, ignoreCase = true) }.forEach { account ->
				val canMarkAsBanned = canMarkBannedCurrent(serverIp)

				if (canMarkAsBanned) account.bannedServers.add(serverIp) else account.bannedServers.remove(serverIp)

				saveConfig(LiquidBounce.fileManager.accountsConfig)

				logger.info("Marked account {} {} on {}", account.name, if (canMarkAsBanned) "banned" else "un-banned", serverIp)
			}
		}
	}
}
