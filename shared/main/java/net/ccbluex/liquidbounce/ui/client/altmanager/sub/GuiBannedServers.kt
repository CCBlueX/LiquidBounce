/*
 * LiquidBounce Hacked Client
 * A free open source mixin-based injection hacked client for Minecraft using Minecraft Forge.
 * https://github.com/CCBlueX/LiquidBounce/
 */
package net.ccbluex.liquidbounce.ui.client.altmanager.sub

import net.ccbluex.liquidbounce.LiquidBounce
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiButton
import net.ccbluex.liquidbounce.api.minecraft.client.gui.IGuiTextField
import net.ccbluex.liquidbounce.api.util.WrappedGuiScreen
import net.ccbluex.liquidbounce.api.util.WrappedGuiSlot
import net.ccbluex.liquidbounce.file.FileManager.Companion.saveConfig
import net.ccbluex.liquidbounce.ui.client.altmanager.GuiAltManager
import net.ccbluex.liquidbounce.ui.font.Fonts
import net.ccbluex.liquidbounce.utils.login.MinecraftAccount
import net.ccbluex.liquidbounce.utils.render.RenderUtils.drawRect
import org.lwjgl.input.Keyboard
import java.awt.Color
import java.io.IOException

class GuiBannedServers(private val prevGui: GuiAltManager, private val account: MinecraftAccount) : WrappedGuiScreen()
{
	private lateinit var serversList: GuiServersList

	var status = "\u00A77Idle..."

	override fun initGui()
	{
		val width = representedScreen.width
		val height = representedScreen.height

		serversList = GuiServersList(this, account)
		serversList.represented.registerScrollButtons(7, 8)

		representedScreen.buttonList.add(classProvider.createGuiButton(1, width - 80, 46, 70, 20, "Add"))
		representedScreen.buttonList.add(classProvider.createGuiButton(2, width - 80, 70, 70, 20, "Remove"))
		representedScreen.buttonList.add(classProvider.createGuiButton(0, width - 80, height - 65, 70, 20, "Back"))
	}

	override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
	{
		val width = representedScreen.width

		representedScreen.drawBackground(0)
		serversList.represented.drawScreen(mouseX, mouseY, partialTicks)

		val middleScreen = width / 2f

		Fonts.font40.drawCenteredString("\u00A7cBanned servers\u00A78 of \u00A7a" + if (account.accountName == null) account.name else account.accountName, middleScreen, 6f, 0xffffff)
		Fonts.font35.drawCenteredString("\u00A7a" + (if (account.accountName == null) account.name else account.accountName) + "\u00A78 is \u00A7cbanned\u00A78 from \u00A7c" + serversList.getSize() + "\u00A7a servers.", middleScreen, 18f, 0xffffff)
		Fonts.font35.drawCenteredString(status, middleScreen, 32f, 0xffffff)

		super.drawScreen(mouseX, mouseY, partialTicks)
	}

	@Throws(IOException::class)
	override fun actionPerformed(button: IGuiButton)
	{
		when (button.id)
		{
			0 -> mc.displayGuiScreen(prevGui.representedScreen)
			1 -> mc.displayGuiScreen(GuiAddBanned(this, account).representedScreen)
			2 -> status = if (serversList.selectedSlot != -1 && serversList.selectedSlot < serversList.getSize())
			{
				account.bannedServers.remove(account.bannedServers[serversList.selectedSlot])
				saveConfig(LiquidBounce.fileManager.accountsConfig)
				"\u00A7aThe server has been removed."
			}
			else "\u00A7cSelect a server."
		}
		super.actionPerformed(button)
	}

	@Throws(IOException::class)
	override fun keyTyped(typedChar: Char, keyCode: Int)
	{
		when (keyCode)
		{
			Keyboard.KEY_ESCAPE ->
			{
				mc.displayGuiScreen(prevGui.representedScreen)
				return
			}

			Keyboard.KEY_UP ->
			{
				var i = serversList.selectedSlot - 1
				if (i < 0) i = 0
				serversList.elementClicked(i, false, 0, 0)
			}

			Keyboard.KEY_DOWN ->
			{
				var i = serversList.selectedSlot + 1
				if (i >= serversList.getSize()) i = serversList.getSize() - 1
				serversList.elementClicked(i, false, 0, 0)
			}

			Keyboard.KEY_RETURN -> serversList.elementClicked(serversList.selectedSlot, true, 0, 0)
			Keyboard.KEY_NEXT -> serversList.represented.scrollBy(representedScreen.height - 100)

			Keyboard.KEY_PRIOR ->
			{
				serversList.represented.scrollBy(-representedScreen.height + 100)
				return
			}
		}
		super.keyTyped(typedChar, keyCode)
	}

	@Throws(IOException::class)
	override fun handleMouseInput()
	{
		super.handleMouseInput()
		serversList.represented.handleMouseInput()
	}

	class GuiServersList internal constructor(prevGui: GuiBannedServers, private val account: MinecraftAccount) : WrappedGuiSlot(mc, prevGui.representedScreen.width, prevGui.representedScreen.height, 40, prevGui.representedScreen.height - 40, 15)
	{
		internal var selectedSlot = 0
			get()
			{
				if (field > account.bannedServers.size) selectedSlot = -1
				return field
			}

		override fun isSelected(id: Int): Boolean = selectedSlot == id

		override fun getSize(): Int = account.bannedServers.size

		override fun elementClicked(id: Int, doubleClick: Boolean, var3: Int, var4: Int)
		{
			selectedSlot = id
		}

		override fun drawSlot(id: Int, x: Int, y: Int, var4: Int, mouseXIn: Int, mouseYIn: Int)
		{
			val server = account.bannedServers[id]
			val middleScreen = represented.width / 2f

			Fonts.font40.drawCenteredString(server, middleScreen, y + 1f, Color.RED.rgb, true)
		}

		override fun drawBackground()
		{
		}
	}

	private class GuiAddBanned(private val prevGui: GuiBannedServers, private val account: MinecraftAccount) : WrappedGuiScreen()
	{
		private lateinit var name: IGuiTextField
		var status = "\u00A77Idle..."

		override fun initGui()
		{
			val width = representedScreen.width
			val height = representedScreen.height

			Keyboard.enableRepeatEvents(true)

			val buttonX = width / 2 - 100
			val quarterScreen = height / 4

			representedScreen.buttonList.add(classProvider.createGuiButton(1, buttonX, quarterScreen + 96, "Add " + (if (account.accountName == null) account.name else account.accountName) + "'s banned server"))
			representedScreen.buttonList.add(classProvider.createGuiButton(0, buttonX, quarterScreen + 120, "Back"))

			name = classProvider.createGuiTextField(2, Fonts.font40, buttonX, 60, 200, 20).apply {
				isFocused = true
				text = ""
				maxStringLength = 128
			}
		}

		override fun drawScreen(mouseX: Int, mouseY: Int, partialTicks: Float)
		{
			val width = representedScreen.width
			val height = representedScreen.height

			val middleScreen = width / 2f
			val quarterScreen = height / 4f

			representedScreen.drawBackground(0)

			drawRect(30, 30, width - 30, height - 30, Int.MIN_VALUE)


			Fonts.font40.drawCenteredString("Add Banned Server", middleScreen, 34f, 0xffffff)
			Fonts.font40.drawCenteredString(status, middleScreen, quarterScreen + 84f, 0xffffff)

			name.drawTextBox()

			if (name.text.isEmpty() && !name.isFocused) Fonts.font40.drawCenteredString("\u00A77Add Server", middleScreen - 74f, 66f, 0xffffff)

			super.drawScreen(mouseX, mouseY, partialTicks)
		}

		@Throws(IOException::class)
		override fun actionPerformed(button: IGuiButton)
		{
			when (button.id)
			{
				0 -> mc.displayGuiScreen(prevGui.representedScreen)

				1 ->
				{
					val server = name.text

					if (server.isEmpty())
					{
						status = "\u00A7cEnter a server address!"
						return
					}

					if (account.bannedServers.contains(server))
					{
						status = "\u00A7cServer already exists!"
						return
					}

					account.bannedServers.add(server)

					saveConfig(LiquidBounce.fileManager.accountsConfig)

					prevGui.status = "\u00A7aAdded banned server \u00A7c" + server + " \u00A7aof " + (if (account.accountName == null) account.name else account.accountName) + "\u00A7c."

					mc.displayGuiScreen(prevGui.representedScreen)
				}
			}
			super.actionPerformed(button)
		}

		@Throws(IOException::class)
		override fun keyTyped(typedChar: Char, keyCode: Int)
		{
			if (keyCode == Keyboard.KEY_ESCAPE)
			{
				mc.displayGuiScreen(prevGui.representedScreen)
				return
			}
			if (name.isFocused) name.textboxKeyTyped(typedChar, keyCode)
			super.keyTyped(typedChar, keyCode)
		}

		@Throws(IOException::class)
		override fun mouseClicked(mouseX: Int, mouseY: Int, mouseButton: Int)
		{
			name.mouseClicked(mouseX, mouseY, mouseButton)
			super.mouseClicked(mouseX, mouseY, mouseButton)
		}

		override fun updateScreen()
		{
			name.updateCursorCounter()
			super.updateScreen()
		}

		override fun onGuiClosed()
		{
			Keyboard.enableRepeatEvents(false)
			super.onGuiClosed()
		}
	}
}
